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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

import org.sqsh.commands.Help;
import org.sqsh.commands.Jaql;
import org.sqsh.input.ConsoleLineReader;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;
import org.sqsh.options.OptionException;
import org.sqsh.options.OptionProcessor;

import static org.sqsh.options.ArgumentRequired.REQUIRED;
import static org.sqsh.options.ArgumentRequired.NONE;

/**
 * This implements the public command line interface to kicking off
 * sqsh.
 */
public class JSqsh {

    private static final String LOGGING_CONFIG
        = "org/sqsh/logging.properties";
    
    private static class Options
        extends ConnectionDescriptor {
        
       @OptionProperty(
           option='i', longOption="input-file", arg=REQUIRED, argName="file",
           description="Name of file to read as input. This option may be repeated")
       public List<String> inputFiles = new ArrayList<String>();
       
       @OptionProperty(
           option='o', longOption="output-file", arg=REQUIRED, argName="file",
           description="Name of file send output instead of stdout")
       public String outputFile = null;
       
       @OptionProperty(
           option='e', longOption="echo", arg=NONE, 
           description="Echoes all input back. Useful for running scripts.")
       public boolean isInputEchoed = false;
       
       @OptionProperty(
           option='n', longOption="non-interactive", arg=NONE,
           description="Disables recording of input history, and line editing functionality")
       public boolean nonInteractive = false;
       
       @OptionProperty(
           option='j', longOption="jaql", arg=NONE,
           description="Start the session in Jaql mode")
       public boolean jaqlMode = false;
       
       @OptionProperty(
           option='a', longOption="jaql-path", arg=REQUIRED,
           description="Colon separated list of jaql module search directories")
       public String jaqlSearchPath = null;
       
       @OptionProperty(
           option='J', longOption="jaql-jars", arg=REQUIRED,
           description="Comma separated list of jars to be used by the jaql shell")
       public String jaqlJars = null;
       
       @OptionProperty(
           option='b', longOption="debug", arg=REQUIRED, argName="class",
           description="Turn on debugging for a java class or package")
       public List<String> debug = new ArrayList<String>();
       
       @OptionProperty(
           option='r', longOption="readline", arg=REQUIRED, argName="method",
           description="Readline method "
                     + "(readline,editline,getline,jline,purejava)")
       public String readline = null;

       @OptionProperty(
           option='C', longOption="config-dir", arg=REQUIRED, argName="dir",
           description="Configuration directory in addition to $HOME/.jsqsh.")
       public List<String> configDirectories = new ArrayList<String>();

       @OptionProperty(
           option='R', longOption="drivers", arg=REQUIRED, argName="file",
           description="Specifies additional drivers.xml files to be loaded")
       public List<String> driverFiles = new ArrayList<String>();
       
       @OptionProperty(
           option='v', longOption="var", arg=REQUIRED, argName="name=value",
           description="Sets a jsqsh variable. This option may be repeated")
       public List<String> vars = new ArrayList<String>();
       
       @OptionProperty(
           option='W', longOption="width", arg=REQUIRED, argName="cols",
           description="Sets the display width of output")
       public int width = -1;
       
       @OptionProperty(
           option='t', longOption="topic", arg=REQUIRED, argName="topic",
           description="Displays detailed help on specific topics")
       public String topic = null;
       
       @Argv(program="jsqsh", min=0, max=1, usage="[options] [connection-name]")
       public List<String> arguments = new ArrayList<String>();
    }
   
    public static void main (String argv[]) {
        
        Options options = new Options();
        OptionProcessor optParser = new OptionProcessor(options);
        
        try {
            
            optParser.parseOptions(argv);
        }
        catch (OptionException e) {
            
            System.err.println(e.getMessage());
            System.err.println(optParser.getUsage());
            System.err.println("Type \"help jsqsh\" at the jsqsh prompt for "
                + "additional information");
            
            System.exit(1);
        }
        
        /*
         * Display help if requested.
         */
        if (options.doHelp) {
            
            System.out.println(optParser.getUsage());
            System.out.println("Type \"help jsqsh\" at the jsqsh prompt for "
                + "additional information");
            System.exit(0);
        }
        
        
        configureLogging(options.debug);
        
        /*
         * No input files, then add a "null" input file.
         */
        if (options.inputFiles.size() == 0) {
            
            options.inputFiles.add(null);
        }
        
        PrintStream out = getOutputStream(options);
        PrintStream err = System.err;
        
        if (out == null || err == null) {
            
            System.exit(1);
        }
        
        /*
         * In non-interative mode, we force pure-java input. I have found
         * all sorts of conflicts when using jline against input that doesn't
         * come from the console.
         */
        if (options.nonInteractive) {
            
            options.readline = ConsoleLineReader.NONE;
        }
        
        /*
         * If the first input is a file, then we don't use a line reader
         */
        SqshContext sqsh = new SqshContext();
        
        int rc = 0;
        
        if (options.width > 0) {
            
            sqsh.setScreenWidth(options.width);
        }
        
        for (String dir : options.configDirectories) {

            sqsh.addConfigurationDirectory(dir);
        }

        for (String file : options.driverFiles) {

            sqsh.addDriverFile(file);
        }
        
        sqsh.setInputEchoed(options.isInputEchoed);
        
        setVariables(sqsh, options.vars);
        
        InputStream in = null;
        try {
            
            Session session = sqsh.newSession();
            
            if (options.topic != null) {
            
                System.exit(Help.displayHelpText(session, options.topic));
            }
            
            if (options.jaqlMode 
                    || options.jaqlJars != null
                    || options.jaqlSearchPath != null) {
                
                if (!doJaql(session, options)) {
                    
                    rc = 1;
                }
            }
            else if (!doConnect(session, options)) {
                
                rc = 1;
            }
            
            if (rc != 0) {
                
                System.exit(rc);
            }
            
            for (int i = 0; i < options.inputFiles.size(); i++) {
                
                if (i > 0) {
                    
                    session.getBufferManager().getCurrent().clear();
                }
                
                in = getInputStream(options.inputFiles.get(i));
                if (in == null) {
                    
                    break;
                }
                
                session.setIn(in, (options.inputFiles != null), (in == System.in));
                session.setOut(out, options.outputFile != null);
                
                /*
                 * If we are forcibly non-interactive then just leave the context
                 * untouched--it is that way by default.
                 */
                if (!options.nonInteractive) {
                    
                    /*
                     * It is possible that we will implicitly switch between 
                     * non-interactive and active mode when there is more than 
                     * one input.  The use of stdin is our trigger for 
                     * interactive mode.
                     */
                    if (in == System.in) {
                        
                        sqsh.setReader(options.readline, true);
                    }
                    else {
                        
                        sqsh.setReader(ConsoleLineReader.NONE, false);
                    }
                }
                
                int curRc = sqsh.run(session);
                if (curRc != 0) {
                    
                    rc = curRc;
                }
                
                if (in != System.in) {
                    
                    in.close();
                    in = null;
                }
            }
        }
        catch (Throwable e) {
            
            e.printStackTrace(System.err);
            rc = 1;
        }
        finally {
            
            out.flush();
            err.flush();
            
            if (out != System.out && out != System.err) {
                
                out.close();
            }
            if (err != System.err && err != System.out) {
                
                err.close();
            }
            
            if (in != null && in != System.in) {
                
                try {
                    
                    in.close();
                }
                catch (IOException e) {
                    
                    /* IGNORED */
                }
            }

            sqsh.close();
        }
        
        System.exit(rc);
    }

    static private void configureLogging(List<String> loggers) {
        
        InputStream in = 
            JSqsh.class.getClassLoader().getResourceAsStream(LOGGING_CONFIG);
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

        /*
         * Turn on debugging if requested.
         */
        for (String logger : loggers) {
            
            Logger log = Logger.getLogger(logger);
            if (log != null) {
                
                log.setLevel(Level.FINE);
                System.out.println("Debugging level for '"
                    + log.getName() + "' is now '"
                    + log.getLevel().getName() + "'");
            }
            else {
                
                System.err.println("--debug: Unable to find logger '"
                    + logger + "'");
                System.exit(1);
            }
        }
    }
    
    /**
     * Sets any variables that were assigned with the "-v" argument.
     * @param session The session to apply them to
     * @param vars The variables to set
     */
    private static void setVariables(SqshContext ctx, List<String> vars) {
        
        if (vars == null || vars.size() == 0) {
            
            return;
        }
        
        for (int i = 0; i < vars.size(); i++) {
            
            String v = vars.get(i);
            int idx = v.indexOf('=');
            String name;
            String value = null;
            
            if (idx < 0) {
                
                name = v;
            }
            else {
                
                name  = v.substring(0, idx);
                if (idx < (v.length() - 1)) {
                    
                    value = v.substring(idx+1);
                }
            }
            
            ctx.getVariableManager().put(name, value);
        }
    }
    
    /**
     * Returns the input stream to be used by the session.
     * 
     * @param options Configuration options.
     * @return The input stream or null if the requested one cannot be
     *    opened.
     */
    private static InputStream getInputStream(String filename) {
        
        if (filename != null && !"-".equals(filename)) {
            
            try {
                
                InputStream in = new BufferedInputStream(
                    new FileInputStream(filename));
                
                return in;
            }
            catch (IOException e) {
                
                System.err.println("Unable to open input file '" 
                    + filename + "' for read: "
                    + e.getMessage());
                
                return null;
            }
        }
        
        return System.in;
    }
    
    /**
     * Returns the output stream to be used by the session.
     * 
     * @param options Configuration options.
     * @return The input stream or null if the requested one cannot be
     *    opened.
     */
    private static PrintStream getOutputStream(Options options) {
        
        if (options.outputFile != null) {
            
            try {
                
                PrintStream out = new PrintStream(options.outputFile);
                return out;
            }
            catch (IOException e) {
                
                System.err.println("Unable to open output file '" 
                    + options.outputFile + "' for write: "
                    + e.getMessage());
                
                return null;
            }
        }
        
        return System.out;
    }
    
    /**
     * If --jaql was provided, sets up the session to be a jaql session.
     * 
     * @param session The session
     * @param options The options provided to start jsqsh
     * @return true if teh jaql session was successfully started, false
     *   otherwise.
     */
    private static boolean doJaql(Session session, Options options) {
        
        Jaql.Options jaqlOptions = new Jaql.Options();
        Jaql cmd = new Jaql();
        
        if (options.jaqlJars != null)
            jaqlOptions.jars = options.jaqlJars;
        if (options.jaqlSearchPath != null)
            jaqlOptions.jaqlPath = options.jaqlSearchPath;
        
        try {
            
            if (cmd.execute(session, jaqlOptions) != 0) {
                
                return false;
            }
        }
        catch (Exception e) {
            
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * 
     * Ok, I'm lazy. Since most of the command line options are there
     * to allow the caller to pre-connect sqsh, I am actually just
     * going to build a connect command and execute it rather than
     * messing with API calls to do the dirty work for me.
     * 
     * @param options The command line options that were passed it.
     * @return A string containing a connect command, or null if no
     *   connection is necessary.
     */
    private static boolean doConnect(Session session, Options options) {
        
        ConnectionDescriptor connDesc = (ConnectionDescriptor)options;
        
        String autoConnect = session.getVariable("autoconnect");
        boolean ok = true;
        
        /*
         * If any one of our options having to do with establish a connection
         * have been provided, then connect!
         */
        if ((autoConnect != null && ! "false".equals(autoConnect))
                || options.getServer() != null
                || options.getPort() != -1
                || options.getCatalog() != null
                || options.getUsername() != null
                || options.getPassword() != null
                || options.getSid() != null
                || options.getJdbcClass() != null
                || options.getDriver() != null
                || options.getDomain() != null
                || options.arguments.size() > 0) {
            
            String connName = null;
            if (options.arguments.size() > 0) {
                
                connName = options.arguments.get(0);
            }
            else if (autoConnect != null 
                && !"true".equals(autoConnect)
                && !"false".equals(autoConnect)) {
                
                connName = autoConnect;
            }
            
            if (connName != null) {
                
                ConnectionDescriptorManager connDescMan = 
                    session.getConnectionDescriptorManager();
                
                ConnectionDescriptor savedOptions = connDescMan.get(connName);
                if (savedOptions == null) {
                    
                    session.err.println("There is no saved connection "
                        + "information named '" + connName + "'.");
                    
                    ok = false;
                }
                else {
                
                    connDesc = connDescMan.merge(savedOptions, connDesc);
                }
            }
            
            if (ok) {
                
	            try {
	                
	                ConnectionContext ctx = 
	                    session.getDriverManager().connect(session, connDesc);
	                session.setConnectionContext(ctx);
	            }
	            catch (SQLException e) {
	                
	                SQLTools.printException(session, e);
	                ok = false;
	            }
	        }
        }
        
        if (!ok)
        {
	        session.err.println("Unable to establish connection.");
	        session.err.println("  For general command line option help use: jsqsh --help");
	        session.err.println("  For detailed command line option help use: jsqsh --topic=jsqsh");
	        
	        if (autoConnect != null && ! "false".equals(autoConnect)) {
	            
	            session.err.println();
	            session.err.println("  Autoconnect is enabled. Use -vautoconnect=false to disable. This");
	            session.err.println("  will present you with the jsqsh prompt where the \"\\help\" command");
	            session.err.println("  can be used to view more jsqsh help topics");
	        }
        }
	        
        return ok;
    }
}
