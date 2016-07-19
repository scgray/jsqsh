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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.input.ConsoleEOFException;
import org.sqsh.input.ConsoleException;
import org.sqsh.input.ConsoleInterruptedException;
import org.sqsh.jni.Shell;
import org.sqsh.jni.ShellException;
import org.sqsh.jni.ShellManager;
import org.sqsh.signals.FlaggingSignalHandler;
import org.sqsh.signals.SignalManager;

/**
 * Represents an active session in sqsh. A session is the complete
 * context of the work that you are doing right now, such as the
 * connection to the database, the query history, variable settings, etc.
 */
public class Session
    implements Cloneable {
    
    private static final Logger LOG = 
        Logger.getLogger(Session.class.getName());

    /**
     * Path to the built-in file full of variable definitions.
     */
    private static final String SESSION_VARS
        = "org/sqsh/variables/SessionVariables.xml";

    /**
     * This ID is assigned by the SqshContext and is used to reference
     * which session is which.
     */
    private int sessionId;
    
    /**
     * Last value returned from a command execution.
     */
    private int commandReturn = 0;
    
    /**
     * This contains the total number of commands that have been executed
     * that failed to return a 0 return value (i.e. that failed).
     */
    private int commandFailCount = 0;
    
    /**
     * Manages I/O handles.
     */
    private InputOutputManager ioManager = new InputOutputManager();
    
    /**
     * Effectively equivalent of {@link System#err}, however the context
     *   is strictly local to this session.
     */
    public PrintStream err = ioManager.getErr();
    
    /**
     * Effectively equivalent of {@link System#in}, however the context
     *   is strictly local to this session.
     */
    public InputStream in = ioManager.getIn();
    
    /**
     * Effectively equivalent of {@link System#out}, however the context
     *   is strictly local to this session.
     */
    public PrintStream out = ioManager.getOut();
    
    /**
     * The parent context that owns this session.
     */
    private SqshContext sqshContext;
    
    /**
     * This is a place where commands can place arbitrary objects to 
     * maintain state between calls. See {@link SessionObject}.
     */
    private Map<String, SessionObject> sessionObjects = 
        new HashMap<String, SessionObject>();
    
    /**
     * The session has a local variable manager.
     */
    private VariableManager variableManager;
    
    /**
     * Command line tokenizer.
     */
    private Tokenizer tokenizer = new Tokenizer();
    
    /**
     * This is the number of rows-per-fetch to ask the driver (Statement
     * object) to use when fetching data.
     */
    private int fetchSize = -1;
    
    /**
     * Enables or disables whether or not commands that are tagged for 
     * auto-pagination (e.g. piping output through "more") are actually
     * paged.
     */
    private boolean autoPager = true;
    
    /**
     * The database connection used by the session and the URL that was
     * used to create it. This will never be null, but will contain a 
     * DisconnectedConnectionContext when there is no connection.
     */
    private ConnectionContext connection;
    
    /**
     * The last exception that was displayed. This is retained for the 
     * \stack command to use.
     */
    private Throwable lastException = null;    
    
    /**
     * Creates a new session.
     * 
     * @param sqshContext Handle back to the instance of sqsh that
     *   owns this context.
     * @param sessionId Identifier for this session.
     */
    protected Session(SqshContext sqshContext, int sessionId) {
        
        this.sessionId   = sessionId;
        this.sqshContext = sqshContext;
        this.connection  = new DisconnectedConnectionContext(this);
        
        /*
         * This object will be available as a bean called "global" in the
         * sqsh variables.
         */
        variableManager = new VariableManager(sqshContext.getVariableManager());
        variableManager.addBean("session", this);
        
        InputStream in = getClass().getClassLoader().getResourceAsStream(
            SESSION_VARS);
        if (in == null) {
            
            throw new IllegalStateException("Cannot locate internal sqsh "
                + "variable definition file: " + SESSION_VARS);
        }
        
        variableManager.load(SESSION_VARS, in);
        
        try {
            
            in.close();
        }
        catch (IOException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * Creates a session that is tied to the I/O handles provided.
     * 
     * @param sqshContext Handle back to the instance of sqsh that
     *   owns this context.
     * @param sessionId Identifier for this session.
     * @param in The input handle.
     * @param out The output handle.
     * @param err The error handle.
     */
    protected Session(SqshContext sqshContext, int sessionId,
            InputStream in, PrintStream out, PrintStream err) {
        
        this(sqshContext, sessionId);
        
        setIn(in, false, true);
        setOut(out, false);
        setErr(err, false);
    }
    
    /**
     * Returns the id number of this session.
     * @return The id number of this session.
     */
    public int getId() {
        
        return sessionId;
    }
    
    /**
     * Returns a handle to the parent context.
     * @return A handle to the parent context.
     */
    public SqshContext getContext() {
        
        return sqshContext;
    }
    
    /**
     * Changes the output stream.
     * 
     * @param out The new output stream.
     * @param autoClose If true, then the output stream will automatically
     *   be closed when it is no longer used by the session.
     */
    public void setOut(PrintStream out, boolean autoClose) {
        
        ioManager.setOut(out, autoClose);
        this.out = out;
    }
    
    /**
     * Changes the error stream.
     * 
     * @param out The new error stream.
     * @param autoClose If true, then the error stream will automatically
     *   be closed when it is no longer used by the session.
     */
    public void setErr(PrintStream err, boolean autoClose) {
        
        ioManager.setErr(err, autoClose);
        this.err = err;
    }
    
    /**
     * Changes the input stream.
     * 
     * @param out The new input stream.
     * @param autoClose If true, then the input stream will automatically
     *   be closed when it is no longer used by the session.
     * @param isInteractive If true, then the input stream is assumed to
     *   be interactive and a prompt will be displayed for input.
     */
    public void setIn(InputStream in, boolean autoClose,
            boolean isInteractive) {
        
        ioManager.setIn(in, autoClose, isInteractive);
        this.in = in;
    }
    
    
    /**
     * Returns the SQL renderer that is to be used by the session.
     * 
     * @return the SQL renderer that is to be used by the session.
     */
    public SQLRenderer getSQLRenderer() {
        
        return sqshContext.getSQLRenderer();
    }
    
    /**
     * Returns the driver manager used by this session.
     * @return The driver manager used by this session
     */
    public SQLDriverManager getDriverManager() {
        
        return sqshContext.getDriverManager();
    }
    
    /**
     * Returns the help manager used by this session.
     * 
     * @return the help manager used by this session.
     */
    public HelpManager getHelpManager() {
        
        return sqshContext.getHelpManager();
    }
    
    /**
     * Returns the connection context for this session. This will be null if no
     * connection has been established yet.
     * 
     * @return The SQL context or null if no connection is available.
     */
    public ConnectionContext getConnectionContext() {
        
        return connection;
    }
    
    /**
     * Sets the connection context for this session. If there is already a 
     * context established, then the current one is closed and discarded.
     * 
     * @param context The new context. A null may be used to disconnect the
     *   session, but leave it running.
     * @param doClose If false, then the existing context is not closed
     */
    public void setConnectionContext(ConnectionContext context, boolean doClose) {
        
        if (context == null) {
            
            context = new DisconnectedConnectionContext(this);
        }
        
        if (doClose) {
            
            connection.close();
        }
        
        connection = context;
    }

    /**
     * Sets the connection context for this session. If there is already a 
     * context established, then the current one is closed and discarded.
     * 
     * @param context The new context.
     */
    public void setConnectionContext(ConnectionContext context) {
        
        setConnectionContext(context, true);
    }
    
    /**
     * @return If the session is currently connection to a JDBC datasource,
     *  this will return the connection to that source, otherwise null.
     */
    public Connection getConnection() {
        
        if (connection instanceof SQLConnectionContext) {
            
            return ((SQLConnectionContext)connection).getConnection();
        }
        
        return null;
    }
    
    /**
     * Given an identifier that may be surrounded with quotes (quoted identifier)
     * normalize the identifier according to the databases identifier normalization
     * rules.
     * 
     * @param identifier The identifier to normalizer
     * @return The normalized identifier.
     */
    public String normalizeIdentifier(String identifier) {
        
        if (connection instanceof SQLConnectionContext) {
            
            return ((SQLConnectionContext)connection).normalizeIdentifier(identifier);
        }
        
        return identifier;
    }
    
    /**
     * Returns the current display style. If no connection is established,
     * then the display style is considered the default style that the
     * RenderManager is configured with, otherwise the connection is 
     * consulted to determine the style. 
     * 
     * @return The name of the display style.
     */
    public String getStyle() {
        
        return connection.getStyle().getName();
    }
    
    /**
     * Sets the display style. Setting the style is a bit of a complex
     * operation under the hood.  If there is a connection established,
     * then the style is set for the connection - this is done because
     * some styles apply to one type of connection and other style to
     * another type.  If there is no connection, the style is set for
     * the RenderManager.  
     * 
     * @param name The name of the style
     * @throws CannotSetValueError If the style name does not exist.
     */
    public void setStyle (String name) {
        
        connection.setStyle(name);
    }
    
    /**
     * @return whether or not commands that are tagged for auto-paging (see
     *  {@link PagedCommand}) are honored.
     */
    public boolean isAutoPager() {
    
        return autoPager;
    }

    /**
     * Enables or disables auto-paging for commands that are tagged with the
     * functionality.
     * 
     * @param autoPager If true, then auto-paging will be honored.
     */
    public void setAutoPager(boolean autoPager) {
    
        this.autoPager = autoPager;
    }

    /**
     * @return The number of rows-per-fetch that will be requested of the driver
     *   when rows are fetched from the server.  Note that the driver may not
     *   necessarily obey this setting! A value of -1, indicates that the driver
     *   will run with its default value.
     */
    public int getFetchSize() {
    
        return fetchSize;
    }
    
    /**
     * Sets the number of rows-per-fetch that will be requested of the driver
     *   when rows are fetched from the server.  Note that the driver may not
     *   necessarily obey this setting! 
     * @param fetchSize The rows per fetch. A value of -1, indicates that the
     *   driver should use its default value.
     */
    public void setFetchSize(int fetchSize) {
    
        this.fetchSize = fetchSize;
    }
    
    /**
     * Adds an object to the session.  This is intended primarily for use
     * by commands wishing to maintain some form of state between calls.
     * 
     * @param name The name of the object. If an object by that name already
     *   exists its {@link SessionObject#close()} method is called and it is
     *   replaced.
     * @param obj The object to add.
     */
    public void addObject (String name, SessionObject obj)
    {
        SessionObject oldObj = sessionObjects.get(name);
        if (oldObj != null)
        {
            oldObj.close();
        }
        sessionObjects.put(name, obj);
    }
    
    /**
     * Looks up a session object.
     * 
     * @param name The name of the session object.
     * @return The session object that was stored under the provided name,
     *   otherwise null is returned.
     */
    public SessionObject getObject (String name)
    {
        return sessionObjects.get(name);
    }
    
    /**
     * Removes a session object from the session.
     * 
     * @param name The name of the session object.
     * @param doClose If true, the {@link SessionObject#close()} method
     *   is called.
     * @return The object that was removed. If the object was not found,
     *   null is returned.
     */
    public SessionObject removeObject (String name, boolean doClose)
    {
        SessionObject o = sessionObjects.remove(name);
        if (o != null && doClose)
        {
            o.close();
        }
        return o;
    }

    /**
     * Returns the string expander for this session.
     * @return The string expander for the session.
     */
    public StringExpander getStringExpander() {
        
        return sqshContext.getStringExpander();
    }
    
    /**
     * @return whether or not exceptions printed with the printException()
     * method will include the name of the exception class in the output.
     */
    public boolean isPrintExceptionClass() {
    
        return sqshContext.isPrintExceptionClass();
    }
    
    /**
     * This is a utility method that should be used by any command attempting
     * to print an exception. It prints the exception to the sessions error
     * output, and honors the session settings for {@link SqshContext#setPrintExceptionClass(boolean)}
     * and {@link SqshContext#setPrintStackTrace(boolean)}.
     * 
     * @param e The exception to print.
     */
    public void printException (Throwable e) {
        
        setException(e);
        sqshContext.printException(err, e);
    }

    /**
     * Sets the last exception that was encountered. This is used by the 
     * "\stack" command to show the stack trace from the last exception.
     * 
     * @param e The exception
     */
    public void setException (Throwable e) {
        
        lastException = e;
    }
    
    /**
     * Retrieves the last exception that was displayed.
     * @return The last exception that was displayed or null if there was
     *  no exception.
     */
    public Throwable getLastException () {
        
        return lastException;
    }
    
    /**
     * Returns whether or not this session has a connection.
     * 
     * @return whether or not the session has a connection.
     */
    public boolean isConnected() {
        
        return !(connection instanceof DisconnectedConnectionContext);
    }
    
    /**
     * Closes the connection utilized by this session.
     */
    public void close() {
        
        if (!(connection instanceof DisconnectedConnectionContext)) {
            
            connection.close();
            connection = new DisconnectedConnectionContext(this);
        }
        
        for (SessionObject o : sessionObjects.values()) {
            
            o.close();
        }
        
        sessionObjects.clear();
    }
    
    /**
     * Returns the manager that is responsible for generating renderers.
     * 
     * @return the manager that is responsible for generating renderers.
     */
    public RendererManager getRendererManager() {
        
        return sqshContext.getRendererManager();
    }
    
    /**
     * Returns the object that manages the connections.xml file.
     * @return the object that manages the connections.xml file.
     */
    public ConnectionDescriptorManager getConnectionDescriptorManager() {
        
        return sqshContext.getConnectionDescriptorManager();
    }
    
    /**
     * Returns the column formatter for this session.
     * 
     * @return the dataFormatter for this session.
     */
    public DataFormatter getDataFormatter () {
    
        return sqshContext.getDataFormatter();
    }
    
    /**
     * Get the command manager for this session.
     * 
     * @return The command manager for this session.
     */
    public CommandManager getCommandManager() {
        
        return sqshContext.getCommandManager();
    }
    
    /**
     * Get the alias manager for this session.
     * 
     * @return The alias manager for this session.
     */
    public AliasManager getAliasManager() {
        
        return sqshContext.getAliasManager();
    }
    
    /**
     * Retrieves the buffer manager for the session.
     * 
     * @return The buffer manager for the session.
     */
    public BufferManager getBufferManager() {
        
        return sqshContext.getBufferManager();
    }
    
    /**
     * Returns the variable manager for the session.
     * 
     * @return The variable manager for the session.
     */
    public VariableManager getVariableManager() {
        
        return variableManager;
    }
    
    /**
     * Returns the shell manager to be used for this session.
     * 
     * @return The shell manager for the session.
     */
    public ShellManager getShellManager() {
        
        return sqshContext.getShellManager();
    }
    
    /**
     * @return The display width of the screen.
     */
    public int getScreenWidth() {
        
        return sqshContext.getScreenWidth();
    }
    
    /**
     * Returns the value of a sqsh variable. If the variable is defined
     * locally, then the local value is returned, otherwise the global
     * variable is returned.
     * 
     * @param name The name of the variable to retrieve.
     * @return The value of the variable or null if the variable is 
     *    not defined.
     */
    public String getVariable(String name) {
        
        return variableManager.get(name);
    }
    
    /**
     * Sets the value of a variable local to this session.
     * 
     * @param name The name of the variable.
     * @param value The value of the variable.
     */
    public void setVariable(String name, String value) {
        
        variableManager.put(name, value);
    }
    
    /**
     * @return Whether or not this is an interactive session.
     */
    public boolean isInteractive () {
    
        return ioManager.isInteractive();
    }
    
    /**
     * Marks the input stream as interactive or non-interactive.
     * 
     * @param isInteractive
     */
    public void setInteractive(boolean isInteractive) {
        
        ioManager.setInteractive(isInteractive);
    }
    
    /**
     * Expands a string of its variables.
     * 
     * @param str The string to expand.
     * @return The expanded string.
     */
    public String expand(String str) {
        
        return getStringExpander().expand(this, str);
    }
    
    /**
     * Expands a string of its variables.
     * 
     * @param variables Additional variables available during the expansion.
     * @param str The string to expand.
     * @return The expanded string.
     */
    public String expand(Map<String, String> variables, String str) {
        
        return getStringExpander().expand(this, variables, str);
    }
    
    /**
     * @return Returns the number of commands that have been executed that 
     * failed (i.e. returned a value other than 0).
     */
    public int getCommandFailCount() {
    
        return commandFailCount;
    }

    /**
     * Sets the count of the number of commands that have been executed
     * by the session that returned a value other than 0 (i.e. that failed).
     * This main purpose of this method is to allow you to reset the count
     * to zero, however if you want to force the count to indicate a failure
     * you can set it to whatever you wish.
     * 
     * @param commandFailCount the number of failures you wish to indicate.
     */
    public void setCommandFailCount(int commandFailCount) {
    
        this.commandFailCount = commandFailCount;
    }

    /**
     * Returns the result code from the last command executed.
     * @return the result code from the last command executed.
     */
    public int getLastCommandResult() {
        
        return commandReturn;
    }
    
    /**
     * Convenience function to start the visual timer facility. The timer
     * will only actually start if the timer service is enabled and if the
     * session is interactive, otherwise this method is a no-op.
     */
    public void startVisualTimer() {
        
        if (isInteractive()) {
            
            sqshContext.startVisualTimer();
        }
    }
    
    /**
     * Convenience function to stop the visual timer facility.
     */
    public void stopVisualTimer() {
        
        if (isInteractive()) {
            
            sqshContext.stopVisualTimer();
        }
    }
    
    /**
     * This is the master read-eval-print loop that is responsible for
     * prompting the user, reading input, building the SQL string, etc.
     */
    public void readEvalPrint()
        throws SqshContextMessage {
        
        String line;
        boolean done = false;
        
        /*
         * Ensure that we are working on a clean buffer when we start.
         */
        if (getBufferManager().getCurrent() == null) {
            
            getBufferManager().newBuffer();
        }
        
        /*
         * Create our signal handler. This will send this thread an
         * interrupt when a signal is received.
         */
        FlaggingSignalHandler sigHandler = new FlaggingSignalHandler();
        SignalManager sigMan = SignalManager.getInstance();
        sigMan.push(sigHandler);
        
        try {
            
            while (!done) {
                
                sigHandler.clear();
                    
                try {

                    line = readLine();

                    if (line == null) {
                        
                        done = true;
                    }
                    else {
                    
                        if (sigHandler.isTriggered() == false) {
                                
                            evaluate(line);
                        }
                        else if (!ioManager.isInteractive()) {
                                
                            /*
                             * If the user hit CTRL-C and we aren't interactive
                             * then we are finished and can return.
                             */
                            done = true;
                        }
                    }
                }
                catch (ConsoleInterruptedException e) {
                    
                    /*
                     * If the user hit ^C, then we abort the current buffer, saving it
                     * away, and start a new one all a-fresh-like:
                     */
                    if (getBufferManager().getCurrent().length() > 0) {

                        getBufferManager().newBuffer();
                    }
                }
                catch (ConsoleException e) {
                    
                    err.println("Error reading input: " + e.getMessage());
                    e.printStackTrace(err);
                    done = true;
                }
            }
        }
        finally {
            
            sigMan.pop();
        }
    }
                
    /**
     * Evaluates a single line of input in the context of the session.
     * @param line The line of context.
     * @throws SqshContextMessage Thrown if the line contained a command
     *    that wants to ask the SqshContext to do something.
     */
    public void evaluate(String line)
        throws SqshContextMessage {
        
        /*
         * Process the user's input to see if it had any aliases in it.
         */
        line = getAliasManager().process(line);
        
        Command cmd = getCommand(line);
        if (cmd != null) {
                
            runCommand(cmd, line);
        }
        else if (line.startsWith("!")) {
            
            BufferManager bufMan = getBufferManager();
            Buffer buf = bufMan.getBuffer(line);
            if (buf == null) {
                
                bufMan.getCurrent().addLine(line);
            }
            else {
                
                bufMan.getCurrent().add(buf.toString());
                redrawBuffer();
            }
        }
        else if (!line.startsWith("##")) {
            
            /*
             * Add the current line to the buffer.
             */
            Buffer curBuf = getBufferManager().getCurrent();
            curBuf.addLine(line);
            
            /*
             * Check to see if the buffer is terminated with our terminator
             * character. If it is, then we need to execute the "go" 
             * command on behalf of the user.
             */
            String args = isTerminated(curBuf);
            if (args != null) {
                
                Command go = getCommandManager().getCommand("\\go");
                
                if (args.length() > 0) {
                    
                    runCommand(go, "\\go " + args);
                }
                else {
                    
                    runCommand(go, "\\go");
                }
            }
        }
    }
    
    /**
     * Explicitly executes a jsqsh command.
     * 
     * @param command The name of the command to execute.
     * @param argv The argument array for the command.
     * 
     * @return The return code from the command, if the command
     *   did not exist or threw an exception a -1 is returned.
     */
    public int execute(String command, String []argv) {
        
        /*
         * Save away the current I/O state.
         */
        saveInputOutput();
        
        try {
            
            Command cmd = getCommand(command);
            if (cmd == null) {
                
                err.println("There is no command '" + command + "'");
                return -1;
            }
            
            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("Executing: " + command);
                for (int i = 0; i < argv.length; i++) {
                    
                    LOG.fine("   Arg #" + (i+1) + ": " + argv[i]);
                }
            }
            
            int rc = cmd.execute(this, argv);
            return rc;
        }
        catch (Exception e) {
                
            e.printStackTrace(err);
        }
        finally {
            
            restoreInputOutput();
        }
        
        return -1;
    }
    
    /**
     * Compares two sessions to see if they are the same.
     */
    public boolean equals(Object o) {
        
        if (!(o instanceof Session)) {
            
            return false;
        }
        
        return (((Session) o).getId() == this.getId());
    }
    
    /**
     * Reads a line of input from the user.
     * 
     * @param input Where to read input.
     * @return The line read or null upon EOF.
     * @throws ConsoleException If the user hits ^C ({@link ConsoleInterruptedException}),
     *   or an error is encountered on input.
     */
    private String readLine() 
        throws ConsoleException {
        
        String line = null;
        try {
                
            if (ioManager.isInteractive()) {
                                
                String prompt = getVariableManager().get("prompt");
                prompt =  (prompt == null)  
                    ? ">" : getStringExpander().expand(this, prompt);
                                
                line = sqshContext.getConsole().readline(prompt + " ", true);
                if (line == null) {
                        
                    line = "";
                }
            }
            else {
                        
                StringBuilder sb = new StringBuilder();
                int ch = in.read();
                while (ch >= 0 && ch != '\n') {
                        
                    sb.append((char) ch);
                    ch = in.read();
                }
                    
                if (ch == -1 && sb.length() == 0) {
                        
                    line = null;
                }
                else {
                        
                    line = sb.toString();
                }
            }
        }
        catch (ConsoleInterruptedException e) {
            
            throw e;
        }
        catch (ConsoleEOFException e) {
            
            line = null;
        }
        catch (EOFException e) {
            
            line = null;
        }
        catch (Throwable e) {
            
            throw new ConsoleException(e.getMessage(), e);
        }
        
        if (line != null && sqshContext.isInputEchoed()) {
            
            out.println(line);
        }
            
        return line;
    }
    
    /**
     * Internally this should be used instead of directly calling 
     * ioManager.restore() because we need to make sure that the
     * public I/O handles are exposed properly.
     */
    private void restoreInputOutput() {
        
        ioManager.restore();
        this.in = ioManager.getIn();
        this.out = ioManager.getOut();
        this.err = ioManager.getErr();
    }
    
    /**
     * Internally used to save away the current set of Input/Output
     * handles.
     */
    private void saveInputOutput() {
        
        ioManager.save();
    }
    
    /**
     * Used by evaluate to determine if the current buffer contains
     * our ${terminator} character and that the current statement
     * should be executed.
     * 
     * @param buffer The buffer to evaluate.
     * @return If the buffer was not terminated then null is returned, 
     *   otherwise the arguments that follow the terminator are returned.
     */
    private String isTerminated(Buffer buffer) {
        
        ConnectionContext conn = getConnectionContext();
        int terminator = sqshContext.getTerminator();
        
        /*
         * The connection determines whether or not the buffer is terminated,
         * so if there is no connection, then we cannot make that decision.
         */
        if (conn == null || terminator < 0) {
            
            return null;
        }
        
        /*
         * Try to find the terminator
         */
        int idx = buffer.length() - 1;
        while (idx >= 0 && buffer.charAt(idx) != terminator) {
            
            --idx;
        }
        
        /*
         * Either we didn't find the terminator, or the characters after
         * the terminator don't appear to be valid input to the "go" command.
         */
        if (idx < 0 || ! looksLikeArgumentsOrWhitespace(buffer, idx+1)) {
            
            return null;
        }
        
        /*
         * When asking the connection if this input is terminated, then
         * make sure we trim off everything after the semicolon.
         */
        CharSequence trimmedSql = buffer;
        if (idx < trimmedSql.length()-1) {
            
            trimmedSql = trimmedSql.subSequence(0, idx+1);
        }
        
        /*
         * We have a terminator at the end, its worth doing an in-depth
         * analysis at this point.
         */
        if (conn.isTerminated(trimmedSql, (char) terminator) == false) {
            
            return null;
        }
        
        /*
         * Pull off the contents after the semicolon as our residuals.
         */
        String residuals = "";
        if (idx < buffer.length()-1) {
            
            residuals = buffer.substring(idx+1, buffer.length());
            buffer.setLength(idx+1);
        }
        
        /*
         * It is terminated, so we are going to trim up our buffer
         * a tad to remove the terminator.
         */
        if (conn.isTerminatorRemoved((char) terminator)) {
            
            buffer.setLength(idx);
        }
        
        return residuals;
    }
    
    /**
     * Checks the characters following a semicolon to see if they are either 
     * all whitespace or if they look like they may contain command line
     * arguments to be passed to "go". The check is rather crude, simply
     * looking to see if what follows the semicolon is an "-a" style argument
     * or "--arg" style argument.
     * 
     * @param str The string to check
     * @param idx The index immediately following the semicolon
     * @return true if what appears after the semicolon is just whitespace or 
     *   command line arguments.
     */
    private boolean looksLikeArgumentsOrWhitespace(CharSequence str, int idx) {
        
        final int len = str.length();
        int i = idx;
        while (i < len) {
            
            char ch = str.charAt(i);
            if (! Character.isWhitespace(ch)) {
                
                // Do we have -arg
                if (ch == '-') {
                    
                    ++i;
                    // Do we have --arg?
                    if (i < len && str.charAt(i) == '-') {
                        
                        ++i;
                        if (i < len 
                            && (str.charAt(i) >= 'a' || str.charAt(i) <= 'z')) {
                            
                            return true;
                        }
                    }
                    else if (i < len 
                            && (str.charAt(i) >= 'a' || str.charAt(i) <= 'z')) {
                        
                        return true;
                    }
                    
                    return false;
                }
                else {
                    
                    return false;
                }
            }
            
            ++i;
        }
        
        return true;
    }
    
    /**
     * Called when the current SQL buffer needs to be redrawn.
     */
    private void redrawBuffer() {
        
        if (ioManager.isInteractive() == false) {
            
            return;
        }
        
        String prompt = getVariableManager().get("prompt");
        Buffer buf = getBufferManager().getCurrent();
        TruncatingLineIterator iter = new TruncatingLineIterator(
            buf.toString(), -1);
        
        buf.clear();
        
        /*
         * Because the $lineno comes from the current buffer's length
         * I need to clear the current contents and re-add it as we
         * go.
         */
        while (iter.hasNext()) {
            
            String str = iter.next();
            String expandedPrompt = expand(prompt);
            
            out.print(expandedPrompt);
            out.print(' ');
            out.println(str);
            
            /*
             * As we display the buffer, make sure the readline history
             * is kept in sync.
             */
            if (ioManager.isInteractive()) {
                
                sqshContext.getConsole().addToHistory(str);
            }
            
            buf.addLine(str);
        }
    }
    
    /**
     * Given a line of input from a user, attempts to determine if it is a
     * command execution. If it is, then return the command being executed.
     * 
     * @param str The string.
     * @return the command being executed or null if no command is
     * being executed.
     */
    private Command getCommand(String str) {
        
        /*
         * Next we will attempt to parse the line.
         */
        tokenizer.reset(str, sqshContext.getTerminator(), false);
        Token token = null;
            
        /*
         * We snag the first token from the tokenizer to see if it
         * is a command or an alias.
         */
        try {
                
            token = tokenizer.next();
        }
        catch (CommandLineSyntaxException e) {
                
            /*
             * This *should* be safe to ignore. The tokenizer is lazy
             * is that it only evaluates the command line as next() is
             * called and therefor if we get a syntax exception here
             * then the first token most certainly wasn't a command.
             */
            return null;
        }
            
        /*
         * If we have a string token, then we want to check to see if
         * it is a command, if so we will parse and execute it.
         */
        if (token != null 
                && token instanceof StringToken) {
            
            /*
             * Get the command name...or is it?
             */
            String cmd = ((StringToken)token).getString();
            
            /*
             * If the command appears to contain a '$', then we will
             * try to expand it.
             */
            if (cmd.indexOf('$') >= 0) {
                
                cmd = getStringExpander().expand(this, str);
                
                /*
                 * And re-parse it because the expansion may have
                 * produced more than one token.
                 */
                tokenizer.reset(cmd, sqshContext.getTerminator(), false);
                try {
                        
                    token = tokenizer.next();
                }
                catch (CommandLineSyntaxException e) {
                        
                    return null;
                }
                
                if (token == null || !(token instanceof StringToken)) {
                    
                    return null;
                }
                
                cmd = ((StringToken)token).getString();
            }
            
            Command command = 
                sqshContext.getCommandManager().getCommand(token.toString());
            return command;
        }
        
        return null;
    }
    
    /**
     * Used internally to execute a command.
     * 
     * @param command The command that is to be executed.
     * @param tokenizer The command line tokenizer. This is expected to
     *    have already processed the first token (the command name itself).
     */
    private void runCommand(Command command, String commandLine)
        throws SqshContextMessage {
        
        Token token = null;
        Shell pipeShell = null;
           
        SessionRedirectToken sessionRedirect = null;
        File sessionOutput = null;
           
        /*
         * Because our commandline may redirect I/O via >file, 1>&2,
         * or a pipe, we want to save away the state of our file descriptors
         * prior to doing this redirection, so that we can restore the
         * state of the world when we are done...
         */
        saveInputOutput();
        
        try {
            
            commandLine = 
                getStringExpander().expandWithQuotes(this, commandLine);
        
            tokenizer.reset(commandLine, sqshContext.getTerminator(),
                command.keepDoubleQuotes());
            
            /*
             * We can safely skip the first word in the command line because
             * we have already determined that it is the name of the command
             * itself.
             */
            tokenizer.next();
            
            List<String> argv = new ArrayList<String>();
            boolean haveTerminator = false;
            token = tokenizer.next();
            while (token != null) {
                
                if (haveTerminator) {
                    
                    throw new CommandLineSyntaxException(
                        "Input is not allowed after the command terminator "
                            + "\"" + sqshContext.getTerminatorString() + "\"",
                        token.getPosition(),
                        token.getLine());
                }
                
                if (token instanceof TerminatorToken) {
                    
                    haveTerminator = true;
                }
                else if (token instanceof RedirectOutToken) {
                    
                    doRedirect((RedirectOutToken) token);
                }
                else if (token instanceof SessionRedirectToken) {
                    
                    sessionRedirect = (SessionRedirectToken) token;
                }
                else if (token instanceof FileDescriptorDupToken) {
                    
                    doDup((FileDescriptorDupToken) token);
                }
                else if (token instanceof PipeToken) {
                    
                    /*
                     * If the output of this session is being redirected
                     * to another session, then we don't want to give the
                     * pipe program control over the terminal. Instead we
                     * want to read its output so we can send it to the
                     * re-direct file.
                     */
                    pipeShell = doPipe((PipeToken) token,
                        sessionRedirect != null);
                }
                else {
                    
                    argv.add(token.toString());
                }
                
                token = tokenizer.next();
            }
            
            /*
             * If the user asked us to redirect to another session, then
             * do so now.  Note that we deferred this until after the
             * command line was parsed so that we could know whether or
             * not we needed to redirect the output of our command or
             * the output of the shell that our command was piped to.
             */
            if (sessionRedirect != null) {
                
                sessionOutput = doSessionRedirect(sessionRedirect, pipeShell);
            }
            
            /*
             * If the command requested that its output be paged, then lets 
             * attempt to do so.
             */
            if (autoPager 
                && command instanceof PagedCommand 
                && pipeShell == null
                && out == System.out) {
                
                pipeShell = getPager();
            }
            
            /*
             * Sweet, it parsed! Now run that bad boy.
             */
            commandReturn =
                command.execute(this, argv.toArray(new String[0]));
            
            if (commandReturn != 0) {
                
                ++commandFailCount;
                
                /*
                 * Was the "exit_on" variable used to register this command to
                 * cause an exit if it fails?
                 */
                if (sqshContext.shouldExitOnFailure(command.getName())) {
                    
                    throw new SqshContextExitMessage(this);
                }
            }
        }
        catch (IOException e) {
                
            /*
             * We are going to assume that our IO exception is due
             * to the pipe to a shell process ending.
             */
                
        }
        catch (SessionRedrawBufferMessage e) {
                    
            redrawBuffer();
        }
        catch (SqshContextMessage e) {
                    
            /*
             * Messages that need to go back to the context will 
             * get thrown from here.
             */
            throw e;
        }
        catch (CommandLineSyntaxException e) {
            
            err.println(e.getMessage());
        }
        catch (Throwable e) {
                    
            e.printStackTrace(err);
        }
        finally {
            
            restoreInputOutput();
            if (pipeShell != null) {
                
                try {
                    
                    pipeShell.waitFor();
                }
                catch (InterruptedException e) {
                    
                }
            }
        }
        
        /*
         * If the session output was redirected to another session, then
         * we need to take the current output and execute it through
         * another session.
         */
        if (sessionRedirect != null) {
            
            executeSessionRedirect(sessionOutput, 
                sessionRedirect.getSessionId(), sessionRedirect.isAppend());
            sessionOutput.delete();
        }
    }
    
    /**
     * Attempts to launch a process to page output of a command through a 
     * pager command, such as "more" or "less". 
     * @return The piped shell or null if one could not be created.
     */
    private Shell getPager() {
        
        ShellManager shellMan = sqshContext.getShellManager();
        
        /*
         * No auto-paging in non-interactive mode. Also if the shell
         * manager cannot do JNI, then it can't open a proper pipe anyway
         * so we can't page.
         */
        if (!isInteractive() || !shellMan.isJNI()) {
            
            return null;
        }
        
        String pager = variableManager.get("PAGER");
        if (pager == null) {
            
            String os = System.getProperty("os.name");
            if (os.startsWith("Linux") || os.startsWith("Mac")) {

                pager = "less -R";
            }
            else {
                
                pager = "more";
            }
                
        }
        
        try {
            
            Shell shell = sqshContext.getShellManager().pipeShell(pager);
            setOut(new PrintStream(shell.getStdin()), true);
            return shell;
        }
        catch (ShellException e) {
            
            err.println("Could not launch pager \"" + pager + "\". Set $PAGER "
                + "variable to specify an alternative pager");
        }
        
        return null;
    }
    
    
    /**
     * Handles creating a pipeline to another process.
     * 
     * @param token The pipeline that needs to be created.
     * @param doRead If true, then the pipe needs to be opened in such a
     *    way as the its input can be read. If false, then the process
     *    spawned's output will be sent directly to the console
     * @throws CommandLineSyntaxException Thrown if there is a problem
     *   creating the pipeline.
     */
    private Shell doPipe(PipeToken token, boolean doRead)
        throws CommandLineSyntaxException {
        
        try {
            
            Shell shell;
            
            if (doRead) {
                
                shell = sqshContext.getShellManager().readShell(
                    token.getPipeCommand(), false);
            }
            else {
                
                shell = sqshContext.getShellManager().pipeShell(
                    token.getPipeCommand());
            }
            
            setOut(new PrintStream(shell.getStdin()), true);
            
            return shell;
        }
        catch (ShellException e) {
            
            throw new CommandLineSyntaxException(
                "Failed to execute '" + token.getPipeCommand() 
                + ": " + e.getMessage(),  token.getPosition(), token.getLine());
        }
    }
    
    /**
     * Called to perform a session redirection action. This method
     * will take the current session's output handle and redirect it
     * to a temporary file. The file handle is then returned to the
     * caller so that it can process the contents after the current
     * command line has completed.
     * 
     * @param token The token just parsed.
     * @param pipeShell If not null, then instead of the output of
     *    the command being redirected to a temporary file, the
     *    output of the command is left sending to the piped shell
     *    specified and the output of the shell is redirected to
     *    the file instead.
     * @throws CommandLineSyntaxException Thrown if the session to 
     *   be redirected to doesn't exist.
     */
    private File doSessionRedirect(SessionRedirectToken token, Shell pipeShell)
        throws CommandLineSyntaxException {
        
        if (sessionId > 0 && sqshContext.getSession(sessionId) == null) {
            
            throw new CommandLineSyntaxException(
                "Session id #" + sessionId + " does not exist. Use "
                + "the \\session command to view active sessions",
                token.getPosition(), token.getLine());
        }
        
        try {
            
            File tmpFile = File.createTempFile("jsqsh_",".tmp");
            
            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("Redirecting output of session #"
                    + sessionId + " to " + tmpFile.toString());
            }
            
            PrintStream out = new PrintStream(
                new FileOutputStream(tmpFile, false));
            
            /*
             * If we are supposed to be processing the output of a shell,
             * then spawn a magical thread that will forward all of the
             * output of the shell to our temporary file, otherwise just
             * attach the session's output to the directly file.
             */
            if (pipeShell != null) {
                
                InputStream in = pipeShell.getStdout();
                IORelayThread relay = new IORelayThread(in, out);
                relay.start();
            }
            else {
                
                setOut(out, true);
            }
            
            return tmpFile;
        }
        catch (IOException e) {
            
            throw new CommandLineSyntaxException(
                token.toString() 
                + ": Failed to create temporary file for session redirection: "
                + e.getMessage(), token.getPosition(), token.getLine());
        }
    }
    
    /**
     * Implements the execution of a session redirection (e.g. '>>+2').
     * A session direction takes place by diverting the output of the
     * current session to a temporary file, and the going back and 
     * making that file the input to a target session.
     * 
     * @param source The file containing the output of this session.
     * @param sessionId The session id of the target session.
     * @param append True if the output is to be appended to the
     *   current SQL buffer.
     */
    private void executeSessionRedirect(File source, int sessionId,
            boolean append) {
        
        Session targetSession = this;
        if (sessionId > 0) {
                
            targetSession = sqshContext.getSession(sessionId);
        }
        
        if (targetSession == null) {
            
            err.println("The session " + sessionId + " targeted for "
                + "redirection no longer exists");
            return;
        }
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Sending output of session "
                + sessionId + ", contained in " 
                + source.toString() + " to session "
                + targetSession.getId());
        }
        
        InputStream in = null;
        try {
            
            in = new FileInputStream(source);
        }
        catch (IOException e) {
            
            err.println("Unable to open temporary output file "
                + source.toString() + " for redirection to session "
                + targetSession.getId());
            return;
        }
            
        targetSession.saveInputOutput();
        targetSession.setIn(in, true, false);
        sqshContext.run(targetSession);
        targetSession.restoreInputOutput();
    }
    
    /**
     * Perform file descriptor redirection.
     * 
     * @param token The token representing the redirection request.
     * 
     * @throws CommandLineSyntaxException Thrown if there is a problem
     *   with the redirection.
     */
    private void doRedirect(RedirectOutToken token)
        throws CommandLineSyntaxException {
        
        if (token.getFd() < 1 || token.getFd() > 2) {
            
            throw new CommandLineSyntaxException("Redirection can only "
                + "be performed on file descriptors 1 (stdout) and 2 (stderr)",
                token.getPosition(), token.getLine());
        }
        
        try {
            
            PrintStream newStream = new PrintStream(
                new FileOutputStream(token.getFilename(), token.isAppend()));
            
            if (token.getFd() == 1) {
                
                setOut(newStream, true);
            }
            else {
                
                setErr(newStream, true);
            }
        }
        catch (IOException e) {
            
            throw new CommandLineSyntaxException("Cannot redirect to file \""
                + token.getFilename() + "\": " + e.getMessage(),
                token.getPosition(), token.getLine());
        }
    }
    
    /**
     * Performs file descriptor duplication requests.
     * 
     * @param token The duplication request.
     * 
     * @throws CommandLineSyntaxException Thrown if something goes wrong.
     */
    private void doDup(FileDescriptorDupToken token)
        throws CommandLineSyntaxException {
        
        if (token.getOldFd() < 1 || token.getOldFd() > 2
                || token.getNewFd() < 1 || token.getNewFd() > 2) {
            
            throw new CommandLineSyntaxException("Redirection can only "
                + "be performed on file descriptors 1 (stdout) and 2 (stderr)",
                token.getPosition(), token.getLine());
        }
        
        /*
         * Ignore request to duplicate to self.
         */
        if (token.getOldFd() == token.getNewFd()) {
            
            return;
        }
        
        if (token.getOldFd() == 1) {
            
            setOut(err, false);
        }
        else {
            
            setErr(out, false);
        }
    }
    
    /**
     * This thread is used to consume the output of a process spawned for
     * pipe(). It reads from the spawned processes output stream and 
     * writes everything it reads to the sessions output stream.
     */
    private static class IORelayThread
        extends Thread {
        
        private static final Logger LOG = 
            Logger.getLogger(Session.class.getName());
        
        private InputStream in;
        private PrintStream out;
        
        public IORelayThread (InputStream in, PrintStream out) {
            
            this.in = in;
            this.out = out;
            setName("IORelayThread");
        }
        
        public void run() {
            
            int count = 0;
            
            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("Starting relay thread");
            }
            
            try {
                    
                int c = in.read();
                while (c >= 0) {
                    
                    ++count;
                    
                    out.write(c);
                    c = in.read();
                }
            }
            catch (IOException e) {
                    
                /* IGNORED */
            }
            
            out.flush();
            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("Relayed " + count + " characters");
            }
        }
    }
}
