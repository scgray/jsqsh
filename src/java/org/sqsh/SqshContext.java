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
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.sqsh.jni.ShellManager;
import org.sqsh.signals.SignalManager;

/**
 * This class represents the global sqsh environment, including all active
 * sessions.
 */
public class SqshContext {
    
    private static final Logger LOG = 
        Logger.getLogger("org.sqsh.SqshContext");
    
    /**
     * Path to the built-in file full of variable definitions.
     */
    private static final String CONTEXT_VARS
        = "org/sqsh/variables/GlobalVariables.xml";
    
    private static final String LOGGING_CONFIG
        = "org/sqsh/logging.properties";
    
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
     * The signal manager.
     */
    private SignalManager signalManager = SignalManager.getInstance();
    
    /**
     * The shell manager is responsible for executing command in a sub-shell
     * (such as during pipes and back-ticks).
     */
    private ShellManager shellManager = ShellManager.getInstance();
    
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
    
    public SqshContext() {
        
        configureLogging();
        
        InputStream in = 
            getClass().getClassLoader().getResourceAsStream(CONTEXT_VARS);
        
        /*
         * This object will be available as a bean called "global" in the
         * sqsh variables.
         */
        variableManager.addBean("global", this);
        
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
        
        boolean haveReadline = false;
        
        StringBuffer errors = new StringBuffer();
        try {
            
            Readline.load(ReadlineLibrary.GnuReadline);
            haveReadline = true;
        }
        catch (Throwable e) {
            
            errors.append("[GNU Readline: ").append(e.getMessage()).append("]");
        }
        
        if (haveReadline == false) {
            
            try {
            
            	Readline.load(ReadlineLibrary.Editline);
            	haveReadline = true;
        	}
        	catch (Throwable e) {
	            
        	    errors.append("[Editline: ").append(e.getMessage()).append("]");
        	}
        }
        
        if (haveReadline == false) {
            
            try {
            
            	Readline.load(ReadlineLibrary.Getline);
            	haveReadline = true;
        	}
        	catch (Throwable e) {
	            
        	    errors.append("[Getline: ").append(e.getMessage()).append("]");
        	}
        }
        
        if (haveReadline == false) {
            
            try {
                
                System.err.println("WARNING: Unable to load readline library. "
                    + "(Reasons: "
                    + errors.toString() 
                    + "). Run '\\help readline' for details.");
            
            	Readline.load(ReadlineLibrary.PureJava);
            	haveReadline = true;
        	}
        	catch (Throwable e) {
	            
        	    System.err.println("Couldn't load pure java readline library!: "
        	        + e.getMessage());
        	}
        }
        
        /*
         * Initialize our chosen readline implementation.
         */
        Readline.initReadline("JSqsh");
        
        /*
         * Install our tab completer. This could fail if the underlying
         * chosen implementation does not support completion.
         */
        try {
            
            Readline.setWordBreakCharacters(" \t,/.()<>=?");
        	Readline.setCompleter(new TabCompleter(this));
        }
        catch (Exception e) {
            
            /* IGNORED */
        }
        
        
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
        loadConfigDirectory();
        
        /*
         * Some of the activities above created sessions to do their
         * work. Just for the sake of "prettyness" to the user, I'll
         * re-set the session numbers so they start at 1.
         */
        nextSessionId = 1;
        prevSessionId = 1;
    }
    
    /**
     * @return The command manager.
     */
    public CommandManager getCommandManager () {
    
        return commandManager;
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
     * Returns the SignalManager.
     * @return The signal manager.
     */
    public SignalManager getSignalManager() {
        
        return signalManager;
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
     * Creates a new session that has its intput and output streams tied
     * to the System streams and sets the session as the current session.
     * 
     * @return A newly created session.
     */
    public Session newSession() {
        
        Session session = new Session(this, nextSessionId);
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
            
            if (curr.getId() == sessionId) {
                
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
     * Starts the context up and running. This method will not return until
     * the last session has exited.
     */
    public void run() {
        
        boolean done = false;
        
        if (currentSession == null && sessions.size() > 0) {
            
            currentSession = sessions.get(0);
        }
        
        String version = variableManager.get("version");
        if (currentSession.isInteractive()) {
            
            currentSession.out.println("JSqsh Release " + version 
                + ", Copyright (C) 2007-2008, Scott C. Gray");
            currentSession.out.println("Type \\help for available help topics");
        }
        
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
                    removeSession(currentSession.getId());
                }
                catch (SqshContextExitMessage e) {
                    
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
                        
                        for (Session session : sessions) {
                            
                            if (session != currentSession) {
                                
                                target = session;
                                break;
                            }
                        }
                    }
                    
                    if (e.isEndingCurrent()) {
                        
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
                                    ? currentSession.getSQLContext().getUrl()
                                            : "*no connection*") + ")");
                    }
                }
            }
        }
        
        saveConfigDirectory();
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
     * Loads configuration files that may be located in the users home 
     * directory to configure sqsh.
     */
    private void loadConfigDirectory() {
        
        String filesep = System.getProperty("file.separator");
        File homedir = new File(
            System.getProperty("user.home") + filesep + ".jsqsh");
        
        /*
         * Load our readline history.
         */
        loadReadlineHistory(homedir);
        
        /*
         * Then load any additional drivers that the user
         * may have defined in their .jsqsh/drivers.xml directory.
         */
        File drivers = new File(homedir, "drivers.xml");
        if (drivers.exists()) {
            
            driverManager.load(drivers);
        }
        
        /*
         * Next try to load the sqshrc file. We do this by forcing the
         * current session to take its input from the sqshrc file.
         */
        File sqshrc = new File(homedir, "sqshrc");
        if (sqshrc.exists()) {
            
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
        
        File buffers = new File(homedir, "history.xml");
        if (buffers.exists()) {
            
            bufferManager.load(buffers);
        }
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
        if (history.exists()) {
                
            try {
                    
                Readline.readHistoryFile(history.toString());
            }
            catch (Throwable e) {
                    
                System.err.println("WARNING: Failed to process readline" +
                        "history: " + e.getMessage());
            }
        }
    }
    
    /**
     * Attempts to load the readline history file.
     */
    private void saveReadlineHistory(File homedir) {
        
        File history = new File(homedir, "readline_history");
        if (homedir.exists()) {
            
            try {
                
                Readline.writeHistoryFile(history.toString());
            }
            catch (Throwable e) {
                
                System.err.println("WARNING: Failed to save readline" +
                        "history: " + e.getMessage());
            }
        }
    }
    
    /**
     * Saves off the state of sqsh into the configuration directory.
     */
    private void saveConfigDirectory() {
       
        String filesep = System.getProperty("file.separator");
        File homedir = new File(
            System.getProperty("user.home") + filesep + ".jsqsh");
        
        File history = new File(homedir, "history.xml");
        bufferManager.save(history);
        
        saveReadlineHistory(homedir);
    }
    
    /**
     * Reads in the java logging configuration information.
     */
    private void configureLogging() {
        
        InputStream in = 
            getClass().getClassLoader().getResourceAsStream(LOGGING_CONFIG);
        if (in == null) {
            
            System.err.println("WARNING: Cannot find resource " 
                + LOGGING_CONFIG);
            return;
        }
        
        try {
            
            LogManager logMan = LogManager.getLogManager();
            logMan.readConfiguration(in);
            in.close();
        }
        catch (IOException e) {
            
            System.err.println("WARNING: Unable to read logging "
                + "properties " + LOGGING_CONFIG + ": " + e.getMessage());
        }
    }
}
