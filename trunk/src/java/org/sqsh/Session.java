/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
package org.sqsh;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.readline.Readline;
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
        Logger.getLogger("org.sqsh.Session");
    
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
     * Effectively equivalent of {@link System#err}, however the context
     *   is strictly local to this session.
     */
    public PrintStream err = System.err;
    private PrintStream origErr = err;
    
    /**
     * Effectively equivalent of {@link System#in}, however the context
     *   is strictly local to this session.
     */
    public InputStream in = System.in;
    private InputStream origIn = in;
    
    /**
     * Effectively equivalent of {@link System#out}, however the context
     *   is strictly local to this session.
     */
    public PrintStream out = System.out;
    private PrintStream origOut = out;
    
    /**
     * Indicates whether or not this is an interactive session.
     */
    private boolean interactive = true;
    
    /**
     * The parent context that owns this session.
     */
    private SqshContext sqshContext;
    
    /**
     * The database connection used by the session and the URL that was
     * used to create it
     */
    private SQLContext sqlContext = null;
    
    /**
     * The session has a local variable manager.
     */
    private VariableManager variableManager;
    
    /**
     * Creates a new session.
     * 
     * @param sqshContext Handle back to the instance of sqsh that
     *   owns this context.
     * @param sessionId Identifier for this session.
     */
    protected Session(SqshContext sqshContext, int sessionId) {
        
        this.sessionId = sessionId;
        this.sqshContext = sqshContext;
        
        
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
        
        this.in = in;
        this.origIn = in;
        this.out = out;
        this.origOut = out;
        this.err = err;
        this.origErr = err;
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
     */
    public void setOut(PrintStream out) {
        
        this.out.flush();
        
        /*
         * Close the current stream iff: 
         *    1. The stream isn't a reference to the original output stream
         *       that the session was created with.
         *    2. The stream isn't a reference to the original error stream
         *       that the session was created with.
         *    3. The output stream isn't a reference to the current error
         *       stream (closing it would leave errors with no place left
         *       to go--picture an Officer and a Gentleman, here).
         */
        if (this.out != origOut && this.out != origErr
                && this.out != this.err) {
            
            if (LOG.isLoggable(Level.FINEST)) {
                
                LOG.finest("setOut: Closing current output stream.");
            }
            
            this.out.close();
        }
        else {
            
            if (LOG.isLoggable(Level.FINEST)) {

                LOG.finest("setOut: Leaving current output stream un-touched: "
                    + ((this.out == origOut) ? "it's the original stream"
                            : ((this.out == origErr) ? "it's the original error stream"
                                    : "it's a reference to the current error stream")));
            }
        }
        
        this.out = out;
    }
    
    /**
     * Changes the error stream.
     * 
     * @param out The new error stream.
     */
    public void setErr(PrintStream err) {
        
        this.err.flush();
        
        /*
         * If the current output is not the original output or the
         * original error, then close it.
         */
        if (this.err != origOut && this.err != origErr
                && this.err != this.out) {
            
            if (LOG.isLoggable(Level.FINEST)) {
                
                LOG.finest("setErr: Closing current error stream.");
            }
            
            this.err.close();
        }
        else {
            
            if (LOG.isLoggable(Level.FINEST)) {

                LOG.finest("setErr: Leaving current error stream un-touched: "
                    + ((this.out == origOut) ? "it's the original output stream"
                            : ((this.out == origErr) ? "it's the original error stream"
                                    : "it's a reference to the current output stream")));
            }
        }
        
        this.err = err;
    }
    
    /**
     * Changes the input stream.
     * 
     * @param out The new input stream.
     */
    public void setIn(InputStream in) {
        
        if (this.in != origIn) {
            
            if (LOG.isLoggable(Level.FINEST)) {
                
                LOG.finest("setIn: Closing current input stream.");
            }
            
            try {
                
                this.in.close();
            }
            catch (IOException e) {
                
                /* IGNORE */
            }
        }
        else {
            
            if (LOG.isLoggable(Level.FINEST)) {

                LOG.finest("setIn: Leaving current input stream intact: "
                    + "it's the original input stream");
            }
        }
        
        this.in = in;
    }
    
    /**
     * Restores all of the input/output file descriptors to where
     * they were when the session was created.
     */
    public void restoreIO() {
        
        setOut(origOut);
        setErr(origErr);
        setIn(origIn);
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
     * Returns the SQL context for this session. This will be null if no
     * connection has been established yet.
     * 
     * @return The SQL context or null if no connection is available.
     */
    public SQLContext getSQLContext() {
        
        return sqlContext;
    }
    
    /**
     * Sets the SQL context for this session. If there is already a 
     * context established, then the current one is closed and discarded.
     * 
     * @param context The new context.
     */
    public void setSQLContext(SQLContext context) {
        
        if (this.sqlContext != null) {
            
            this.sqlContext.close();
        }
        
        this.sqlContext = context;
    }
    
    /**
     * A shorthand way of issueing getSQLContext().getConnection().
     * 
     * @return A connection or null if no connection is available.
     */
    public Connection getConnection() {
        
        if (sqlContext != null) {
            
            return sqlContext.getConnection();
        }
        
        return null;
    }
    
    /**
     * Returns the string expander for this session.
     * @return The string expander for the session.
     */
    public StringExpander getStringExpander() {
        
        return sqshContext.getStringExpander();
    }
    
    /**
     * Returns whether or not this session has a connection.
     * 
     * @return whether or not the sessoin has a connection.
     */
    public boolean isConnected() {
        
        return sqlContext != null;
    }
    
    /**
     * Closes the connection utilized by this session.
     */
    public void close() {
        
        if (sqlContext != null) {
            
            sqlContext.close();
        }
        
        sqlContext = null;
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
     * Returns the signal manager.
     * @return The signal manager.
     */
    public SignalManager getSignalManager() {
        
        return sqshContext.getSignalManager();
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
    
        return interactive;
    }
    
    /**
     * @param interactive Sets whether or not this is an interactive session.
     */
    public void setInteractive (boolean interactive) {
    
        this.interactive = interactive;
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
    public String expand(Map variables, String str) {
        
        return getStringExpander().expand(this, variables, str);
    }
    
    /**
     * Returns the result code from the last command executed.
     * @return the result code from the last command executed.
     */
    public int getLastCommandResult() {
        
        return commandReturn;
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
        getSignalManager().push(sigHandler);
        
        try {
            
            while (!done) {
                
                sigHandler.clear();
                    
                line = readLine();
            	if (line == null) {
                    
            	    done = true;
                }
                else {
                
                    if (sigHandler.isTriggered() == false) {
                            
                        evaluate(line);
                    }
                    else if (!interactive) {
                            
                        /*
                         * If the user hit CTRL-C and we aren't interactive
                         * then we are finished and can return.
                         */
                        done = true;
                    }
                }
            }
        }
        finally {
            
            getSignalManager().pop();
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
            if (isTerminated(curBuf)) {
                
                Command go = getCommandManager().getCommand("\\go");
                runCommand(go, "\\go");
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
            
            restoreIO();
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
     */
    private String readLine() {
        
        String line = null;
        
        try {
            
            if (interactive) {
                            
                String prompt = getVariableManager().get("prompt");
                prompt =  (prompt == null)  
            	    ? ">" : getStringExpander().expand(this, prompt);
                            
                line = Readline.readline(prompt + " ", true);
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
                    
                    return null;
                }
                
                return sb.toString();
            }
        }
        catch (IOException e) {
            
            /* EOF */
        }
        
        return line;
    }
    
    /**
     * Used by evaluate to determine if the current buffer contains
     * our ${terminator} character and that the current statement
     * should be executed.
     * 
     * @param buffer The buffer to evaluate.
     * @return True if the buffer is terminated. If it is, then the
     *   terminator character is removed from the buffer in the process.
     */
    private boolean isTerminated(Buffer buffer) {
        
        String s = getVariable("terminator");
        if (s == null || getSQLContext() == null) {
            
            return false;
        }
        
        char terminator = s.charAt(0);
        
        /*
         * First we'll do a simplistic check to see if our buffer ends
         * with a terminator at all.
         */
        String sql = buffer.toString();
        int idx = sql.length() - 1;
        while (idx >= 0 && Character.isWhitespace(sql.charAt(idx))) {
            
            --idx;
        }
        
        if (idx < 0 || sql.charAt(idx) != terminator) {
            
            return false;
        }
        
        /*
         * We have a terminator at the end, its worth doing an in-depth
         * analysis at this point.
         */
        SQLAnalyzer analyzer = getSQLContext().getAnalyzer();
        if (analyzer.isTerminated(sql, terminator) == false) {
            
            return false;
        }
        
        /*
         * It is terminated, so we are going to trim up our buffer
         * a tad to remove the terminator.
         */
        buffer.setLength(idx);
        return true;
    }
    
    
    /**
     * Called when the current SQL buffer needs to be redrawn.
     */
    private void redrawBuffer() {
        
        if (interactive == false) {
            
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
            if (this.interactive) {
                
                Readline.addToHistory(str);
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
        Tokenizer tokenizer = new Tokenizer(str);
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
                tokenizer = new Tokenizer(cmd);
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
        
        try {
            
            commandLine = 
                getStringExpander().expandWithQuotes(this, commandLine);
        
        	Tokenizer tokenizer = new Tokenizer(commandLine);
            
            /*
             * We can safely skip the first word in the command line because
             * we have already determined that it is the name of the command
             * itself.
             */
            tokenizer.next();
            
            List<String> argv = new ArrayList<String>();
            token = tokenizer.next();
            while (token != null) {
                
                if (token instanceof RedirectOutToken) {
                    
                    doRedirect((RedirectOutToken) token);
                }
                else if (token instanceof FileDescriptorDupToken) {
                    
                    doDup((FileDescriptorDupToken) token);
                }
                else if (token instanceof PipeToken) {
                    
                    pipeShell = doPipe((PipeToken) token);
                }
                else {
                    
                    argv.add(token.toString());
                }
                
                token = tokenizer.next();
            }
            
            /*
             * Sweet, it parsed! Now run that bad boy.
             */
            commandReturn =
                command.execute(this, argv.toArray(new String[0]));
        }
        catch (IOException e) {
                
            /*
             * We are going to assume that our IO exception is due
             * to the
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
        catch (Exception e) {
                    
            e.printStackTrace(err);
        }
        finally {
            
            restoreIO();
            
            if (pipeShell != null) {
                
                try {
                    
                    pipeShell.waitFor();
                }
                catch (InterruptedException e) {
                    
                }
            }
        }
    }
    
    /**
     * Handles creating a pipeline to another process.
     * 
     * @param token The pipeline that needs to be created.
     * @throws CommandLineSyntaxException Thrown if there is a problem
     *   creating the pipeline.
     */
    private Shell doPipe(PipeToken token)
        throws CommandLineSyntaxException {
        
        try {
            
            Shell shell =  sqshContext.getShellManager().pipeShell(
                token.getPipeCommand());
            
            setOut(new PrintStream(shell.getStdin()));
            
            return shell;
        }
        catch (ShellException e) {
            
            throw new CommandLineSyntaxException(
                "Failed to execute '" + token.getPipeCommand() 
                + ": " + e.getMessage(),  token.getPosition(), token.getLine());
        }
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
                
                setOut(newStream);
            }
            else {
                
                setErr(newStream);
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
            
            setOut(err);
        }
        else {
            
            setErr(out);
        }
    }
    
}
