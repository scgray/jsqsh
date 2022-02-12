/*
 * Copyright 2007-2022 Scott C. Gray
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

import org.sqsh.shell.ShellManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static ThreadLocal<SqshContext> threadLocalInstance = new ThreadLocal<SqshContext>();
    
    /**
     * The levels of detail that are available when displaying stack traces.
     */
    public enum ExceptionDetail {
        LOW,     /// Just the SQL state/code and exception class
        MEDIUM,  /// Default display level. Includes message text and nested message text
        HIGH,    /// Displays stack trace
    }
    
    /**
     * When a context exists, this indicates how any failures in the context
     * should be reported.
     */
    public enum ExitStatus {
        TOTAL_FAILURES, /// The total number of commands that returned a non-zero exit code
        LAST_FAILURE    /// A 1 if the last command executed failed
    }
    
    /**
     * Path to the built-in file full of variable definitions.
     */
    private static final String CONTEXT_VARS
        = "org/sqsh/variables/GlobalVariables.xml";
    
    /**
     * The line reader to use during interactive sessions
     */
    private SqshConsole console = null;

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
     * The manager that is used to load extensions
     */
    private ExtensionManager extensionManager;
    
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
     * Handle to the current active session.
     */
    private Session currentSession = null;
    
    /**
     * Responsible for visually displaying a query timer.
     */
    private VisualTimer visualTimer = null;
    
    /**
     * This contains a list of commands upon which we will exit if they fail. The
     * special command name "any" indicates that if any command fails, then 
     * the session executing it will exit.
     */
    private String []exitOnCommandFailure = null;
    private static final String []ANY = new String[] { "any" };
    
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
     * The default query timeout interval
     */
    private int queryTimeout = 0;
    
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
     * How the exit status of the context is determined
     */
    private ExitStatus exitStatus = ExitStatus.TOTAL_FAILURES;

    private String fieldSeparator = Tokenizer.WHITESPACE;
    
    /**
     * Creates a new SqshContext. 
     */
    private SqshContext() {

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
         * Run the internal initializations cript.
         */
        runInitializationScript();

        /*
         * If $JSQSH_HOME/config exists, load it.
         */
        loadGlobalConfigDirectoryIfExists();
        
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

        /*
         * Create the extension manager. This will, as a side effect, load
         * any extensions that were set to auto-load. This happens at the 
         * end since we want the context pretty well initialized by the time
         * this happens
         */
        extensionManager = new ExtensionManager(this);
        
        /*
         * Register a listener that will cause the extension manager to
         * load any extensions that are requesting to be loaded when the
         * driver is available.
         */
        driverManager.addListener(
            new SqshContextDriverListener(this));
    }

    /**
     * Retrieves an instance of a SqshContext for the currently running thread.
     * @return An instance of SqshContext.
     */
    public static SqshContext getThreadLocal() {

        SqshContext instance = threadLocalInstance.get();
        if (instance == null) {

            instance = new SqshContext();
            threadLocalInstance.set(instance);
        }

        return instance;
    }

    /**
     * If the directory <code>$JSQSH_HOME/conf</code> it is loaded.
     */
    private void loadGlobalConfigDirectoryIfExists() {

        final String jsqshHome = System.getenv("JSQSH_HOME");
        if (jsqshHome == null) {

            return;
        }

        File homeConfig = new File(jsqshHome, "conf");
        if (! homeConfig.isDirectory()) {

            return;
        }

        loadConfigDirectory(homeConfig, false);
    }
    
    /**
     * @return The directory in which the jsqsh configuration can be found
     *   (this is $HOME/.jsqsh).
     */
    public File getConfigDirectory() {
        
        /*
         * Load configuration files that may be located in the users 
         * configuration directory.
         */
        File homedir = new File(
            System.getProperty("user.home") + File.separatorChar + ".jsqsh");
        
        return homedir;
    }
    
    /**
     * Sets the default query timeout for all queries on all sessions (only
     * takes effect for new sessions, not already existing sessions).
     * 
     * @param secs The number of seconds before query timeout. A value &lt;= 0
     *   indicates that there is an indefinite timeout period.
     */
    public void setQueryTimeout(int secs) {
        
        this.queryTimeout = secs;
    }
    
    /**
     * @return the current query timeout interval
     */
    public int getQueryTimeout() {
        
        return queryTimeout;
    }

    /**
     * Given a comma delimited list of command names, registers the commands to cause
     * the invoking session to exit in the event the command returns an error. The special
     * name "none" indicates that no command should cause jsqsh to exit (except, of course
     * exit) and "all" indicates that all command failures should cause the session to
     * exit.
     * 
     * @param commands A comma delimited list of commands. 
     */
    public void setCommandsToExitOnFailure(String commands) {
        
        if ("none".equals(commands) || "false".equals(commands) || commands.length() == 0) {
            
            exitOnCommandFailure = null;
        }
        else {            
            
            exitOnCommandFailure = commands.split(",");
            for (String command : exitOnCommandFailure) {
                
                if ("true".equals(command) || "any".equals(command)) {
                    
                    exitOnCommandFailure = ANY;
                    break;
                }
            }
        }
        
    }
    
    /**
     * @return A comma delimited list of commands that, should they return an error 
     *   will cause the current session to exit.
     */
    public String getCommandsToExitOnFailure() {
        
        if (exitOnCommandFailure == null) {
            
            return "none";
        }
        
        if (exitOnCommandFailure.length == 1) {
            
            return exitOnCommandFailure[0];
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < exitOnCommandFailure.length; i++) {
            
            if (i > 0) {
                
                sb.append(',');
            }
            
            sb.append(exitOnCommandFailure[i]);
        }
        
        return sb.toString();
    }
    
    /**
     * Given the name of a command, returns "true" if the command has been registered
     * to cause the session to exit upon failure.
     * 
     * @param command The name of the command to test
     * @return true if the command is registered to cause an exit upon failure.
     */
    public boolean shouldExitOnFailure(String command) {
        
        if (exitOnCommandFailure == null) {
            
            return false;
        }
        
        if (exitOnCommandFailure == ANY) {
            
            return true;
        }
        
        for (String name : exitOnCommandFailure) {
            
            if (command.equals(name) 
                 || (command.charAt(0) == '\\' && command.length() == name.length()+1
                       && command.regionMatches(1, name, 0, name.length()))) {
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * @return The class that is used to manage jsqsh extensions
     */
    public ExtensionManager getExtensionManager() {
        
        return this.extensionManager;
    }

    /**
     * @return The visual timer handle
     */
    public VisualTimer getVisualTimer() {

        if (visualTimer == null) {

            visualTimer = new VisualTimer(true, getConsole().getTerminal().writer());
        }

        return visualTimer;
    }

    /**
     * Enables or disables the displaying of a visual timer that is shown
     * in interactive mode for long running queries. This feature is off
     * by default
     * @param onoff true to enable, false to disable
     */
    public void setVisualTimerEnabled(boolean onoff) {
        
        getVisualTimer().setEnabled(onoff);
    }
    
    /**
     * @return whether or not the visual timer is enabled
     */
    public boolean isVisualTimerEnabled() {
        
        return getVisualTimer().isEnabled();
    }
    
    /**
     * Starts the visual timer going. This is a no-op if the timer is not
     * enabled.
     */
    public void startVisualTimer() {
        
        getVisualTimer().start();
    }
    
    /**
     * Stops the visual timer. This is a no-op if the timer wasn't running
     * or the service is not enabled.
     */
    public void stopVisualTimer() {
        
        getVisualTimer().stop();
    }
    
    /**
     * Determines how the final exit status from the context is computed 
     * see {@link ExitStatus} for details
     * @param exitStatus How the exit status should be determined
     */
    public void setExitStatus (ExitStatus exitStatus) {
        
        this.exitStatus = exitStatus;
    }
    
    /**
     * @return an indication of the how the exit status will be computed when
     *   the context is exited.
     */
    public ExitStatus getExitStatus() {
        
        return this.exitStatus;
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

            return getConsole().getWidth();
        }
        
        return screenWidth;
    }
    
    /**
     * Sets the screen width to use when displaying output. Setting this
     * to a value &lt;= 0 indicates that jsqsh should automatically attempt to
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
    public SqshConsole getConsole() {

        if (console == null) {

            console = new SqshConsole(this);
        }

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
    public Session newSession(boolean isInteractive) {
        
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
    public Session newSession(BufferedReader in,
            PrintStream out, PrintStream err, boolean isInteractive) {

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
     * @param sessionId The id number for the session
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

            /*
             * End year in the copyright will always be right. I'm a sneaky
             * bastard.
             */
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            
            currentSession.out.println("Welcome to JSqsh " 
                + Version.getVersion());
            currentSession.out.println("Type \"\\help\" for help topics, \"quit\" to exit." );
            doneBanner = true;
            
            doWelcome(session);
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
                    
                    if (exitStatus == ExitStatus.LAST_FAILURE) {
                        
                        failCount = currentSession.getLastCommandResult();
                    }
                    else {
                        
                        failCount += currentSession.getCommandFailCount();
                    }
                    
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

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        executeStream(reader);
        
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
    }
    
    /**
     * Do all of the "Welcome to the wonderful world of jsqsh activities"
     */
    private void doWelcome(Session session) {
        
        if (! session.isInteractive()) {
            
            return;
        }
        
        File confDir = getConfigDirectory();
        File welcomeFile = new File(confDir, ".welcome");
        
        if (welcomeFile.exists()) {
            
            return;
        }
        
        /*
         * Create the welcome file since we are going to welcome them!
         */
        try {
                
            welcomeFile.createNewFile();
        }
        catch (IOException e) {
                
            System.err.println("WARNING: Failed to create " + welcomeFile 
                + ": " + e.getMessage());
            return;
        }
        
        /*
         * Here's a hack.  The ".welcome" file is a recent change to jsqsh, and
         * we don't want to do the whole welcome thing if an existing jsqsh user
         * already has a ~/.jsqsh but doesn't have a .welcome file in it. To avoid
         * this, check to see if drivers.xml is older than the date at which this
         * comment was written and don't do the setup activities.
         */
        File driversFile = new File(confDir, "drivers.xml");
        if (driversFile.exists())
        {
            long lastModified = driversFile.lastModified();
        
            /*
             * Directory was created prior to "now" (when I am typing this comment),
             * so pretend the welcome was done
             */
            if (lastModified < 1392411017075L) {
                
                return;
            }
        }
            
        HelpTopic welcome = helpManager.getTopic("welcome");
        System.out.println(welcome.getHelp());
        
        session.out.println();
        session.out.println("You will now enter the jsqsh setup wizard.");
        try {
            
            // getConsole().readline("Hit enter to continue: ", false);
            getConsole().readSingleLine("Hit enter to continue: ", ' ');
            Command command = session.getCommandManager().getCommand("\\setup");
            command.execute(session, new String [] { });
        }
        catch (Exception e) {
            
            /* NOT SURE WHAT TO DO HERE */
            e.printStackTrace();
        }
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
                
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(sqshrc)));
                
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
         * Populate our connection descriptors. Connection descriptions can be
         * generated by external programs. For example, maybe there is a way 
         * to determine the host/port for a given server, then a script can
         * generate a valid connection description automagically.
         */
        String connectionGenerator = System.getenv("JSQSH_CONN_PROG");
        if (connectionGenerator != null) {
            
            connDescMan.loadFromProgram(connectionGenerator);
        }
        
        /*
         * The connections.xml in the users home directory is read next which
         * can override anything defined previously.
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
    private void executeStream(BufferedReader input) {
        
        Session session = null;
        
        session = newSession(input, System.out, System.err, false);
        session.readEvalPrint();
        removeSession(session.getId());
    }
    
    /**
     * Saves off the state of sqsh into the configuration directory.
     */
    private void saveConfigDirectory() {
       
        File homedir = getConfigDirectory();
        
        /*
         * Sometimes we get conflicts if more than one jsqsh process is exiting
         * and stomps over the history of another, so we do an atomic rename here
         * to avoid such stompage.
         */
        File historyTmp = new File(homedir, "history.xml.tmp");
        File history = new File(homedir, "history.xml");
        bufferManager.save(historyTmp);
        
        if (! historyTmp.renameTo(history)) {
            
            System.out.println("WARNING: Failed to rename \"" + historyTmp 
                + "\" to \"" + history + "\"");
        }

        if (console != null) {

            try {

                console.saveHistory();
            }
            catch (IOException e) {

                System.err.println("WARNING: Failed to save console history: " + e.getMessage());
            }
        }
    }
    
    /**
     * A listener that waits for drivers to be made available to the
     * SQLDriverManager and, as a result, triggers extensions to be loaded.
     */
    private static class SqshContextDriverListener 
        implements SQLDriverListener {
        
        private SqshContext context;
        
        public SqshContextDriverListener (SqshContext context) {
            
            this.context = context;
        }

        @Override
        public void driverAvailable(SQLDriverManager dm, SQLDriver driver) {
            
            LOG.fine("Driver \"" + driver.getName()
                + "\" is available. Notifying interested parties");

            ExtensionManager em = context.getExtensionManager();
            Class<? extends Driver> jdbcDriver = driver.getDriver();

            try {

                em.triggerAutoExtensionsForDriver(
                    jdbcDriver != null ? driver.getClass().getClassLoader() : this.getClass().getClassLoader(),
                    driver.getName());
            }
            catch (ExtensionException e) {
                
                // TODO: I need a better way to deal with this
                Session session = context.getCurrentSession();
                if (session != null) {
                    
                    session.err.println(e.getMessage());
                }
                else {
                    
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
