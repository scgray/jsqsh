/*
 * Copyright 2007-2012 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqsh;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sqsh.input.ConsoleLineReader;
import org.sqsh.jni.ShellManager;

/**
 * The SqshContext is the master container of all things jsqsh. Its primary
 * purpose is to manage resources global to all sessions (variables, the
 * set of available commands, aliases, drivers, etc. as well as to manage
 * the sessions themselves.
 * 
 * <h2>Session handling</h2>
 * 
 * <p>Once the context is initialized to your liking, the {@link #run()} method
 * is called which kicks off the jsqsh processing. This method picks the
 * first available session and calls its {@link Session#readEvalPrint()}
 * method and the session itself begins reading input and processing.
 * 
 * <p>For most of the life of a session, the session's readEvalPrint() loop
 * never returns until it has run out of input to process or the user
 * asks the session to end (the <b>\end</b> command. One a session ends,
 * the SqshContext picks the next available session, and calls its 
 * readEvalPrint() loop, until <b>it</b> ends, and so on....
 * 
 * <p>There is a  notable exception to this behavior, though. Certain 
 * {@link Command}s executed in jsqsh (specifically executed by the
 * {@link Session} need to cause a "context switch" from on session to
 * another (commands such as <b>\session</b> command or <b>\connect -n</b>)
 * in these sorts of cases, a special exception is thrown that is
 * sub-classed from {@link SqshContextMessage}. This exception passes
 * completely out of the session and is caught by the SqshContext's
 * {@link #run()} method and causes the context to perform the desired
 * action; for example if it is a {@link SqshContextSwitchMessage} then
 * the session identified by {@link SqshContextSwitchMessage#getTargetSession()}
 * is made the active session.  Similarly, the {@link SqshContextExitMessage}
 * causes the {link {@link #run()} method to return.
 */
public class SqshContext {
    
    private static final Logger LOG = 
        Logger.getLogger(SqshContext.class.getName());
    
    /**
     * The levels of detail that are available when displaying stack traces.
     */
    public static enum ExceptionDetail {
        LOW,     /// Just the SQL state/code and exception class
        MEDIUM,  /// Default display level. Includes message text and nested message text
        HIGH,    /// Displays stack trace
    }
    
    /**
     * Path to the built-in file full of variable definitions.
     */
    private static final String CONTEXT_VARS
        = "org/sqsh/variables/GlobalVariables.xml";
    
    /**
     * This is where we read our input from.
     */
    ConsoleLineReader console = null;
    
    /**
     * This objects instantiates all commands and manages all of the
     * command aliases.
     */
    private CommandManager commandManager = new CommandManager();
    
    /**
     * The alias manager manages, uh, aliases.
     */
    private AliasManager aliasManager = new AliasManager();
    
    /**
     * This manages the drivers that are available for establishing
     * database connection.
     */
    private SQLDriverManager driverManager = new SQLDriverManager();
    
    /**
     * This object is responsible for managing the SQL execution history
     * for all sessions (the history is shared across the session).
     */
    private BufferManager bufferManager = new BufferManager();
    
    /**
     * Used to format column values.
     */
    private DataFormatter dataFormatter = new DataFormatter();
    
    /**
     * Manages the values of variables.
     */
    private VariableManager variableManager = new VariableManager();
    
    /**
     * Used as a factory to generate renderers.
     */
    private RendererManager rendererManager = new RendererManager();
    
    /**
     * The renderer that will be used for displaying SQL.
     */
    private SQLRenderer sqlRenderer = new SQLRenderer(this);
    
    /**
     * The shell manager is responsible for executing command in a sub-shell
     * (such as during pipes and back-ticks).
     */
    private ShellManager shellManager = ShellManager.getInstance();
    
    /**
     * This guy is responsible for loading up and managing 
     * the connection.xml file.
     */
    private ConnectionDescriptorManager connDescMan = new ConnectionDescriptorManager();
    
    /**
     * The help manager owns general help topics.
     */
    private HelpManager helpManager = new HelpManager();
    
    /**
     * Our string expander.
     */
    private StringExpander stringExpander = new StringExpander();
    
    /**
     * Handle to the current active session.
     */
    private Session currentSession = null;
    
    /**
     * This keeps track of the previous session id used by the
     * user.
     */
    private int prevSessionId = 1;
    
    /**
     * This is the next available session id.
     */
    private int nextSessionId = 1;
    
    /**
     * The set of sessions thare are current defined within sqsh.
     */
    private List<Session> sessions = new ArrayList<Session>();

    /**
     * A list of directories in which we will try to load configuration
     * files.
     */
    private List<String> configDirectories = new ArrayList<String>();

    /**
     * A list of driver.xml files that should be added upon startup.
     */
    private List<String> driverFiles = new ArrayList<String>();

    /**
     * This is set once at startup by asking readline if it has ahold
     * of the terminal. If a) readline was properly initialized and 
     * b) readline says "yes", then this will remain true.
     */
    private boolean isInteractive = false;
    
    /**
     * If true, then user input will be echoed back to stdout. This is
     * useful when you want to watch the SQL behind a script fly by along
     * with the results.
     */
    private boolean isInputEchoed = false;
    
    /**
     * Records if we have given the copyright banner yet.
     */
    private boolean doneBanner = false;
    
    /**
     * The width to use when displaying output.
     */
    private int screenWidth = -1;
    
    /**
     * The command terminator that is currently in use. Setting this to -1
     * disables command termination.
     */
    private int terminator = ';';
    
    /**
     * Controls how much detail is displayed in exceptions.
     */
    private ExceptionDetail exceptionDetail = ExceptionDetail.MEDIUM;
    
    /**
     * Used to control whether or not exceptions that are printed out 
     * using the session's printStackTrace() method will include
     * the name of the exception class in the output.
     */
    private boolean printExceptionClass = false;
    
    /**
     * Creates a new SqshContext. 
     * 
     * @param readerType The type of readline implementation to utilize. This
     *   may be one of
     *   <ul>
     *     <li> readline
     *     <li> editline
     *     <li> getline
     *     <li> jline
     *     <li> purejava
     *     <li> null - This will attempt to determine which API is available
     *        on your platform.
     *    </ul>
     */
    public SqshContext() {
        
        InputStream in = 
            getClass().getClassLoader().getResourceAsStream(CONTEXT_VARS);
        
        /*
         * This object will be available as a bean called "global" in the
         * sqsh variables.
         */
        variableManager.addBean("global", this);
        
        /*
         * This allows information about the jsqsh version to be exposed as
         * variables.
         */
        variableManager.addBean("version", new Version.Bean());
        
        if (in == null) {
            
            throw new IllegalStateException("Cannot locate internal sqsh "
                + "variable definition file: " + CONTEXT_VARS);
        }
        else {
            
            variableManager.load(CONTEXT_VARS, in);
        }
        
        try {
            
            in.close();
        }
        catch (IOException e) {
            
            /* IGNORED */
        }
        
        /*
         * The init script should always be run with no editline support
         */
        setReader(ConsoleLineReader.NONE, false);
        
        /*
         * Run the internal initializations cript.
         */
        runInitializationScript();
        
        /*
         * Create a configuration directory for the user if necessary.
         */
        createConfigDirectory();
        
        /*
         * Load configuration files that may be located in the users 
         * configuration directory.
         */
        loadConfigDirectory(getConfigDirectory(), true);
        
        /*
         * Some of the activities above created sessions to do their
         * work. Just for the sake of "prettyness" to the user, I'll
         * re-set the session numbers so they start at 1.
         */
        nextSessionId = 1;
        prevSessionId = 1;
    }
    
    /**
     * @return The directory in which the jsqsh configuration can be found
     *   (this is $HOME/.jsqsh).
     */
    private File getConfigDirectory() {
        
        /*
         * Load configuration files that may be located in the users 
         * configuration directory.
         */
        String filesep = System.getProperty("file.separator");
        File homedir = new File(
            System.getProperty("user.home") + filesep + ".jsqsh");
        
        return homedir;
    }
    
    /**
     * Changes which input handler is being used for this context.
     * @param readerType The name of the reader
     */
    public void setReader(String readerType, boolean isInteractive) {
        
        /*
         * If we were already interactive, and switch to (possibly) 
         * non-interactive mode then save the current state away.
         */
        if (this.isInteractive) {
            
            try {
                
                console.writeHistory();
            }
            catch (Exception e) {
                
                /* IGNORED */
            }
        }
        
        /*
         * Create the new reader and mark if we are interactive.
         */
        console = ConsoleLineReader.getReader(this, readerType);
        this.isInteractive = isInteractive;
        
        if (isInteractive) {
            
            loadReadlineHistory(getConfigDirectory());
        }
    }
    
    
    /**
     * @return true if the user's input is to be echoed back.
     */
    public boolean isInputEchoed() {
    
        return isInputEchoed;
    }

    /**
     * Sets whether or not input that is read in is echoed back out. This
     * is usually used in non-interactive mode to display the SQL being run
     * along with the output of the SQL.
     * 
     * @param isInputEchoed set to true if you want input echoed back.
     */
    public void setInputEchoed(boolean isInputEchoed) {
    
        this.isInputEchoed = isInputEchoed;
    }
    
    /**
     * Determines whether or not exceptions that are printed out via
     * the sessions printException() method will show the stack trace.
     * This method is kept around just for compatibility reasons. You
     * should use setExceptionDetail() now.
     * 
     * @param printStackTrace if true, then exceptions will have
     *   their stack trace printed.
     */
    public void setPrintStackTrace(boolean printStackTrace) {
    
        if (printStackTrace) {
            
            exceptionDetail = ExceptionDetail.HIGH;
        }
        else {
            
            exceptionDetail = ExceptionDetail.MEDIUM;
        }
    }
    
    /**
     * @return The width of the screen to use when rendering output.
     */
    public int getScreenWidth() {
    
        if (screenWidth < 1) {
            
            return shellManager.getConsoleWidth();
        }
        
        return screenWidth;
    }
    
    /**
     * Sets the screen width to use when displaying output. Setting this
     * to a value <= 0 indicates that jsqsh should automatically attempt to
     * determine the width of the screen
     * 
     * @param screenWidth The new screen width.
     */
    public void setScreenWidth(int screenWidth) {
    
        this.screenWidth = screenWidth;
    }

    /**
     * @return whether or not exceptions that are printed out via
     * the sessions printException() method will show the stack trace.
     */
    public boolean isPrintStackTrace() {
    
        return exceptionDetail == ExceptionDetail.HIGH;
    }
    
    /**
     * @return The current level of detail that is used when displaying
     *    exceptions.
     */
    public ExceptionDetail getExceptionDetail() {
    
        return exceptionDetail;
    }

    /**
     * Sets the level of detail for displaying exceptions.
     * @param exceptionDetail The new level.
     */
    public void setExceptionDetail(ExceptionDetail exceptionDetail) {
    
        this.exceptionDetail = exceptionDetail;
    }
    
    /**
     * @return The current level of detail that is used when displaying
     *    exceptions.
     */
    public String getExceptionDetailString() {
    
        return exceptionDetail.toString();
    }

    /**
     * Sets the level of detail for displaying exceptions.
     * @param exceptionDetail The new level.
     */
    public void setExceptionDetailString(String exceptionDetail) {
    
        ExceptionDetail detail = ExceptionDetail.valueOf(exceptionDetail.toUpperCase());
        if (detail != null) {
            
            this.exceptionDetail = detail;
        }
        else {
            
            throw new IllegalArgumentException("Illegal detail level \""
                + exceptionDetail + "\" use low, medium, or high");
        }
    }

    /**
     * Determines whether or not exceptions printed with the printException()
     * method will include the name of the exception class in the output.
     * 
     * @param printExceptionClass if true, then the class name will be
     *   included in the output.
     */
    public void setPrintExceptionClass(boolean printExceptionClass) {
    
        this.printExceptionClass = printExceptionClass;
    }

    /**
     * @return whether or not exceptions printed with the printException()
     * method will include the name of the exception class in the output.
     */
    public boolean isPrintExceptionClass() {
    
        return printExceptionClass;
    }
    
    /**
     * This is a utility method that should be used by any command attempting
     * to print an exception. It prints the exception to the sessions error
     * output, and honors the session settings for {@link #setPrintExceptionClass(boolean)}
     * and {@link #setPrintStackTrace(boolean)}.
     * 
     * @param e The exception to print.
     */
    public void printException (PrintStream out, Throwable e) {
        
        if (exceptionDetail == ExceptionDetail.LOW) {
            
            out.println("Exception: " + e.getClass().getName());
            return;
        }
        
        if (printExceptionClass)
            out.print("["+e.getClass().getName()+"]: ");
        
        if (exceptionDetail == ExceptionDetail.HIGH) {
            
            e.printStackTrace(out);
        }
        else if (exceptionDetail == ExceptionDetail.MEDIUM) {
            
            out.println(e.toString());
            
            e = e.getCause();
            while (e != null) {
            
                if (printExceptionClass)
                    out.print("["+e.getClass().getName()+"]: ");
                out.println(e.toString());
                e = e.getCause();
            }
        }
    }
    
    /**
     * @return The current command terminator character. A negative value
     *    indicates that there is no terminator.
     */
    public int getTerminator() {
    
        return terminator;
    }
    
    /**
     * Sets the command terminator.
     * @param terminator The character that should be used to terminate commands.
     *   A negative value disabled character termination.
     */
    public void setTerminator(int terminator) {
    
        this.terminator = terminator;
    }

    /**
     * Sets the command terminator using a string. This method is
     * primarily intended for use when setting the value using BeanUtils,
     * which is done with the $terminator variable is set.
     * 
     * @param terminator The command terminator. If the string is null or
     *    zero length, then the command terminator is disabled, otherwise 
     *    the first character of the string is used.
     */
    public void setTerminatorString(String terminator) {
    
        setTerminator((terminator == null || terminator.length() == 0)
            ? -1 : terminator.charAt(0));
    }
    
    /**
     * Returns the command terminator using a string. This method is
     * primarily intended for use when setting the value using BeanUtils,
     * which is done with the $terminator variable is set.
     * @return The current command terminator as a string. 
     */
    public String getTerminatorString() {
        
        if (terminator < 0)
            return null;
        
        return Character.toString((char) terminator);
    }
    

    /**
     * @return The command manager.
     */
    public CommandManager getCommandManager () {
    
        return commandManager;
    }
    
    /**
     * @return The console handle.
     */
    public ConsoleLineReader getConsole() {
        
        return console;
    }
    
    /**
     * Returns the alias manager.
     * @return the alias manager.
     */
    public AliasManager getAliasManager() {
        
        return aliasManager;
    }
    
    /**
     * Returns a handle to the help manager.
     * 
     * @return A handle to the help manager.
     */
    public HelpManager getHelpManager() {
        
        return helpManager;
    }
    
    /**
     * Returns a handle to the SQL driver manager.
     * @return a handle to the SQL driver manager.
     */
    public SQLDriverManager getDriverManager() {
        
        return driverManager;
    }
    
    /**
     * Returns the SQL buffer manager for this sqsh context.
     * 
     * @return The SQL buffer manager for this sqsh context.
     */
    public BufferManager getBufferManager() {
        
        return bufferManager;
    }
    
    /**
     * Returns the tool that is used to format data values into columns.
     * @return The tool that is used to format data values into columns.
     */
    public DataFormatter getDataFormatter() {
        
        return dataFormatter;
    }
    
    /**
     * Returns the thing that holds all of the sqsh variables.
     * @return The variable manager.
     */
    public VariableManager getVariableManager() {
        
        return variableManager;
    }
    
    /**
     * Get the follow responsible for installing pluggable renderers and
     * managing which one is the default.
     * @return the renderer manager.
     */
    public RendererManager getRendererManager() {
        
        return rendererManager;
    }
    
    /**
     * Returns the tool that is responsible for converting the output of
     * a sql statement into something that looks pretty on the screen.
     * 
     * @return The SQL renderer.
     */
    public SQLRenderer getSQLRenderer() {
        
        return sqlRenderer;
    }
    
    /**
     * Returns the shell manager.
     * 
     * @return The shell manager.
     */
    public ShellManager getShellManager() {
        
        return shellManager;
    }
    
    /**
     * Returns the object that manages the connections.xml file.
     * 
     * @return The ConnectionDescriptorManager.
     */
    public ConnectionDescriptorManager getConnectionDescriptorManager() {
        
        return connDescMan;
    }
    
    /**
     * Returns the string expander for sqsh.
     * @return the string expander for sqsh.
     */
    public StringExpander getStringExpander() {
        
        return stringExpander;
    }
    
    /**
     * Returns the value of a sqsh variable.
     * 
     * @param name The name of the variable to retrieve.
     * @return The value of the variable or null if the variable is 
     *    not defined.
     */
    public String getVariable(String name) {
        
        return variableManager.get(name);
    }
    
    /**
     * Sets the value of a variable.
     * 
     * @param name The name of the variable.
     * @param value The value of the variable.
     */
    public void setVariable(String name, String value) {
        
        variableManager.put(name, value);
    }
    
    /**
     * Adds an additional configuration directory to be processed
     * when the context is run(). This method may be called multiple
     * times to install additional configuration directories, however
     * these directories are not processed until run() is called.
     */ 
    public void addConfigurationDirectory(String name) {

        configDirectories.add(name);
        
        File dir = new File(name);
        if (dir.isDirectory()) {

            loadConfigDirectory(dir, false);
        }
        else {

            System.err.println("WARNING: Configuration directory "
                + dir.toString() + " does not exist or is not a directory");
        }
    }

    /**
     * Adds an additional file in the format of the drivers.xml that
     * should be processed upon startup.
     *
     * @param name The path to a file that is in the formation of drivers.xml.
     */ 
    public void addDriverFile(String name) {

        driverFiles.add(name);
        
        File file = new File(name);
        if (file.exists()) {

            driverManager.load(file);
        }
        else {

            System.err.println("WARNING: Could not load driver file '"
                + file.toString() + "'");
        }
    }
    
    /**
     * Creates a new session that has its intput and output streams tied
     * to the System streams and sets the session as the current session.
     * 
     * @return A newly created session.
     */
    public Session newSession() {
        
        Session session = new Session(this, nextSessionId);
        session.setInteractive(isInteractive);
            
        ++nextSessionId;
        
        sessions.add(session);
        return session;
    }
    
    /**
     * Creates a new session with its communications streams explicitly
     * defined by the caller.
     * 
     * @param in The input stream to utilize.
     * @param out The output stream to utilize
     * @param err The error stream to utilize.
     * @return The newly created session.
     */
    public Session newSession(InputStream in, 
            PrintStream out, PrintStream err) {
        
        Session session =
            new Session(this, nextSessionId, in, out, err);
        session.setInteractive(isInteractive);
        
        ++nextSessionId;
        
        sessions.add(session);
        return session;
    }
    
    /**
     * Returns the list of sessions that are currently defined in this
     * context.
     * 
     * @return The list of sessions currently defined for this context.
     */
    public Session[] getSessions() {
        
        return sessions.toArray(new Session[0]);
    }
    
    /**
     * Looks up a session according to its session id.
     * @param sesionId. The id number for the session
     * @return  The matching session or null if the id does not
     *   refer to a valid session.
     */
    public Session getSession(int sessionId) {
        
        for (Session session : sessions) {
            
            if (session.getId() == sessionId) {
                
                return session;
            }
        }
        
        return null;
    }
    
    /**
     * Returns the current session.
     * 
     * @return The current session or null if no session is active.
     */
    public Session getCurrentSession() {
        
        return currentSession;
    }
    
    /**
     * Sets the current active session to be the one indicated by
     * the provided sessionId.
     * 
     * @param sessionId The Id of the session to set as current.
     * 
     * @return true if the operation was successful.
     */
    public boolean setCurrentSession(int sessionId) {
        
        Session sess = getSession(sessionId);
        if (sess != null) {
            
            currentSession = sess;
        }
        
        return (sess != null);
    }

    /**
     * Closes all connections and removes all sessions.
     */
    public void close() {

        removeSession(-1);
    }
    
    /**
     * Kills a session.
     * 
     * @param sessionId The id of the session to kill.
     * @return the session that was removed, or null if the session was not
     *   found.
     */
    public Session removeSession(int sessionId) {
        
        Iterator<Session> iter = sessions.iterator();
        Session curr = null;
        Session prev = null;
        
        while (iter.hasNext()) {
            
            prev = curr;
            curr = iter.next();
            
            if (sessionId == -1 || curr.getId() == sessionId) {
                
                /*
                 * Make sure the connection is closed for the session.
                 */
                curr.close();

                iter.remove();
                if (curr == currentSession) {
                    
                    if (prev != null) {
                        
                        currentSession = prev;
                    }
                    else {
                        
                        if (sessions.size() == 0) {
                            
                            currentSession = null;
                        }
                        else {
                            
                            currentSession = sessions.get(0);
                        }
                    }
                }
                
                return curr;
            }
        }
        
        return null;
    }
    
    /**
     * This is the master jsqsh loop. This loop is responsible for picking
     * up the first available session, executing it until it completes, 
     * choosing the next session, running it until it completes, and so on.
     * When the last session has executed or a session throws a 
     * {@link SqshContextExitMessage} then this method will return.
     * 
     * @return The total number of commands executed across all sessions
     *   that failed.
     */
    public int run() {
        
        return run((Session) null);
    }
    
    /**
     * This method is similar to {@link #run()} but is intended to be used
     * to execute a single session. When this session completes, then the
     * method returns.  It is important to note that the session is <b>not</b>
     * removed from the SqshContext after completion. If you want to remove
     * the session, call {@link SqshContext#removeSession(int)}.
     * 
     * <p>Why isn't it removed, you may ask?  The reason this method exists
     * is so that jsqsh commands, such as \eval, utilize a session object
     * to execute some arbitrary SQL or jsqsh commands in a recursive manner.
     * 
     * <p>That is, lets say a command had a block of text containing SQL and/or
     * jsqsh commands in it, then the command would want to do something like:
     * 
     * <pre>
     *     InputStream in = new StringBufferInputStream(
     *        bufferWithCommandsAndSql);
     *     session.setIn(in, true, false);
     *     session.getContext().run(session);
     * </pre>
     * 
     * <p>The <code>session.setIn()</code> replaces the input stream with
     * the new InputStream we created to read the buffer, then the
     * <code>session.getContext().run(session)</code> is asking the
     * SqshContext to effectively recurse back into the session to
     * execute the input. When the command containing this code returns
     * the Session will take care of restoring the input descriptor back
     * to its original state.
     * 
     * @return The total number of commands executed across all sessions
     *   that failed.
     */
    public int run(Session session) {
        
        Session priorCurrentSession = currentSession;
        int failCount = 0;

        /*
         * If we were asked to execute a specific session, then make
         * it the current session.
         */
        if (session != null) {
            
            currentSession = session;
        }
        else {
        
            /*
             * If we have no current session, then pick one.
             */
            if (currentSession == null
                    && sessions.size() > 0) {
                
                currentSession = sessions.get(0);
            }
        }
        
        
        /*
         * First interactive session we get, display our copyright
         * banner.
         */
        if (!doneBanner && currentSession.isInteractive()) {

            if (!shellManager.isJNI()) {

                currentSession.err.println(
                    "WARNING: Sqsh JNI layer not available ("
                    + shellManager.getJNIFailureReason() 
                    + "). Run '\\help jni' for details");
            }
            
            /*
             * End year in the copyright will always be right. I'm a sneaky
             * bastard.
             */
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            
            currentSession.out.println("JSqsh Release " 
                + Version.getVersion() 
                + ", Copyright (C) 2007-" + year + ", Scott C. Gray");
            currentSession.out.println("Type \\help for available help "
                + "topics. Using " + console.getName() + ".");
            
            doneBanner = true;
        }
        
        /*
         * Stick in a loop until we are out of sessions or until
         * the session we are being asked to execute has finished.
         */
        boolean done = false;
        while (!done) {
            
            if (currentSession == null) {
                
                done = true;
            }
            else {
                
                try {
                    
                    /*
                     * Enter the current sessions read-eval-print loop. If
                     * this loop just returns that means the session is
                     * over (exited).
                     */
                    currentSession.readEvalPrint();
                    
                    /*
                     * If any commands failed during the session, 
                     * accumulate them into our failure count.
                     */
                    failCount += currentSession.getCommandFailCount();
                    
                    /*
                     * If we were asked to run a specific session and it
                     * has just finished, then we are done.
                     */
                    if (currentSession == session) {
                        
                        done = true;
                    }
                    else {
                        
                        removeSession(currentSession.getId());
                    }
                }
                catch (SqshContextExitMessage e) {
                    
                    failCount += currentSession.getCommandFailCount();
                    removeSession(currentSession.getId());
                    done = true;
                }
                catch (SqshContextSwitchMessage e) {
                    
                    Session target = e.getTargetSession();
                    
                    /*
                     * If no target was supplied, then try to switch
                     * to the previous session.
                     */
                    if (target == null 
                            && prevSessionId >= 0
                            && currentSession.getId() != prevSessionId) {
                        
                        target = getSession(prevSessionId);
                    }
                    
                    /*
                     * No previous session? Then just grab the next 
                     * available.
                     */
                    if (target == null) {
                        
                        for (Session s : sessions) {
                            
                            if (s != currentSession) {
                                
                                target = s;
                                break;
                            }
                        }
                    }
                    
                    if (e.isEndingCurrent()) {
                        
                        failCount += currentSession.getCommandFailCount();
                        removeSession(currentSession.getId());
                        prevSessionId = -1;
                    }
                    else {
                        
                        prevSessionId = currentSession.getId();
                    }
                    
                    currentSession = target;
                    
                    if (currentSession != null) {
                        
                        currentSession.out.println("Current session: " 
                            + currentSession.getId() + " ("
                            + (currentSession.isConnected() 
                                    ? currentSession.getConnectionContext().toString()
                                            : "*no connection*") + ")");
                    }
                }
            }
        }
        
        saveConfigDirectory();
        
        /*
         * If we were asked to execute a specific session, then we
         * will restore our context back to the session that was
         * asking us to do so.
         */
        if (session != null && priorCurrentSession != null
                && getSession(priorCurrentSession.getId()) != null) {
            
            currentSession = priorCurrentSession;
        }
        
        return failCount;
    }
    
    /**
     * Executes sqsh's internal configuration script. This script is 
     * treated identically to the user's $HOME/.jsqsh/sqshrc file except
     * that this one is executed first.
     */
    private void runInitializationScript() {
        
        InputStream in = getClass().getClassLoader().getResourceAsStream(
            "org/sqsh/InitScript.txt");
            
        if (in == null) {
            
            System.err.println("WARNING: Unable to located internal"
                + "initialization script.");
        }
        
        executeStream(in);
        
        try {
            
            in.close();
        }
        catch (IOException e) {
            
            /* IGNORED */
        }
        
    }
    
    /**
     * Attempts to create a .jsqsh directory in the users home folder
     * if it does not already exist, and populates it with copies
     * of all of the files contained in org/sqsh/template.
     */
    private void createConfigDirectory() {
        
        String filesep = System.getProperty("file.separator");
        File homedir = new File(
            System.getProperty("user.home") + filesep + ".jsqsh");
        
        /*
         * Bail if the directory already exists.
         */
        if (homedir.exists()) {
            
            return;
        }
        
        /*
         * Create the directory.
         */
        if (homedir.mkdirs() == false) {
            
            System.err.println("WARNING: Unable to create configuration "
                + "directory structure '" + homedir.toString() + "'");
            return;
        }
        
        copyResource(homedir, "org/sqsh/template/sqshrc");
        copyResource(homedir, "org/sqsh/template/drivers.xml");
        
        HelpTopic welcome = helpManager.getTopic("welcome");
        System.out.println(welcome.getHelp());
    }
    
    /**
     * Copies a file contained in the sqsh jar to a local directory.
     * 
     * @param dir The destination directory.
     * @param resource The path to the resource.
     */
    private void copyResource(File dir, String resource) {
        
        InputStream in =
            getClass().getClassLoader().getResourceAsStream(resource);
        PrintStream out = null;
        
        if (in == null) {
            
            System.err.println("WARNING: Unable to locate internal sqsh "
                + "resource file '" + resource + "'");
            return;
        }
        
        File dest = new File(dir, resource.substring(
            resource.lastIndexOf('/') + 1));
        
        try {
            
            BufferedReader src = 
                new BufferedReader(new InputStreamReader(in));
            
            out = new PrintStream(new FileOutputStream(dest));
            String line = src.readLine();
            while (line != null) {
                
                out.println(line);
                line = src.readLine();
            }
        }
        catch (IOException e) {
            
            System.err.println("WARNING: Unable to create file '"
                + dest.toString() + "': " + e.getMessage());
        }
        finally {
            
            try {
                
                in.close();
            }
            catch (IOException e) {
                
                /* IGNORED */
            }
            
            out.close();
        }
    }

    /**
     * Processes files located in a configuration directory. A configuration
     * directory can have a drivers.xml file to define drivers, a sqshrc
     * file to contain commands to execute and a history.xml file containing
     * previous query history.
     *
     * @param configDir The name of the directory to load
     * @param doHistory If true, then the directory is checked to see if
     *   it contains a "history.xml" file that is used to prime the 
     *   query history.
     */
    private void loadConfigDirectory(File configDir, boolean doHistory) {

        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Processing configuration directory '"
                + configDir.toString() + "'");
        }
        
        /*
         * Load our readline history.
         */
        if (isInteractive) {
            
            loadReadlineHistory(configDir);
        }
        
        /*
         * Then load any additional drivers that the user
         * may have defined in their .jsqsh/drivers.xml directory.
         */
        File drivers = new File(configDir, "drivers.xml");
        if (drivers.exists()) {
            
            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("    Loading drivers from " + drivers.toString());
            }

            driverManager.load(drivers);
        }
        else {

            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("    Skipping drivers, '" + drivers.toString()
                    + "' does not exist.");
            }
        }
        
        /*
         * Next try to load the sqshrc file. We do this by forcing the
         * current session to take its input from the sqshrc file.
         */
        File sqshrc = new File(configDir, "sqshrc");
        if (sqshrc.exists()) {

            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("    Reading configuration file '"
                    + sqshrc.toString() + "'");
            }

            
            try {
                
                InputStream in = new BufferedInputStream(
                    new FileInputStream(sqshrc));
                
                executeStream(in);
                
                in.close();
            }
            catch (IOException e) {
                
                System.err.println("Unable to read sqsh configuration '"
                    + sqshrc.toString() + "': " + e.toString());
            }
        }
        else {

            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("    Configuration file '"
                    + sqshrc.toString() + "' does not exist. Skipping.");
            }
        }
        
        
        if (doHistory) {

            /*
             * The buffer manage has likely been used already to load
             * the initialization script and/or other tasks. To make
             * sure that it only contains history from the last time
             * jsqsh was started, we clear it out here.
             */
            bufferManager.clear();
            
            File buffers = new File(configDir, "history.xml");
            if (buffers.exists()) {

                if (LOG.isLoggable(Level.FINE)) {

                    LOG.fine("    Reading history file '"
                        + buffers.toString() + "'");
                }
                
                bufferManager.load(buffers);
            }
            else {

                if (LOG.isLoggable(Level.FINE)) {

                    LOG.fine("    History file '"
                        + buffers.toString() + "' does not exist. Skipping.");
                }
            }
        }
        
        /*
         * Populate our connection descriptors.
         */
        File connections = new File(configDir, "connections.xml");
        connDescMan.load(connections.toString());
    }
    
    /**
     * Reads and executes the contents of a stream as if it was input
     * by the user.
     * 
     * @param input The stream to execute.
     */
    private void executeStream(InputStream input) {
        
        Session session = null;
        
        session = newSession(input, System.out, System.err);
        session.setInteractive(false);
        session.readEvalPrint();
        removeSession(session.getId());
    }
    
    /**
     * Attempts to load the readline history file.
     */
    private void loadReadlineHistory(File homedir) {
        
        File history = new File(homedir, "readline_history");
        try {
                    
            console.readHistory(history.toString());
        }
        catch (Throwable e) {
                    
            System.err.println("WARNING: Failed to process readline" +
                    "history: " + e.getMessage());
        }
    }
    
    
    /**
     * Attempts to load the readline history file.
     */
    private void saveReadlineHistory(File homedir) {
        
        if (homedir.exists()) {
            
            try {
                
                console.writeHistory();
            }
            catch (Throwable e) {
                
                System.err.println("WARNING: Failed to save readline" +
                        " history: " + e.getMessage());
            }
        }
    }
    
    /**
     * Saves off the state of sqsh into the configuration directory.
     */
    private void saveConfigDirectory() {
       
        File homedir = getConfigDirectory();
        File history = new File(homedir, "history.xml");
        bufferManager.save(history);
        
        saveReadlineHistory(homedir);
    }
}
