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

import org.sqsh.options.Argv;
import org.sqsh.options.Option;
import org.sqsh.options.OptionException;
import org.sqsh.options.OptionProcessor;

import static org.sqsh.options.ArgumentRequired.REQUIRED;
import static org.sqsh.options.ArgumentRequired.OPTIONAL;
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
        
       @Option(
           option='i', longOption="input-file", arg=REQUIRED, argName="file",
           description="Name of file to read as input instead of stdin")
       public String inputFile = null;
       
       @Option(
           option='o', longOption="output-file", arg=REQUIRED, argName="file",
           description="Name of file send output instead of stdout")
       public String outputFile = null;
       
       @Option(
           option='n', longOption="non-interactive", arg=NONE,
           description="Force the session to be non-interactive "
                           + "(not yet implemented)")
        public boolean nonInteractive = false;
       
       @Option(
           option='b', longOption="debug", arg=REQUIRED, argName="class",
           description="Turn on debugging for a java class or package")
       public List<String> debug = new ArrayList<String>();
       
       @Option(
           option='r', longOption="readline", arg=REQUIRED, argName="method",
           description="Readline method "
                     + "(readline,editline,getline,jline,purejava)")
       public String readline = null;

       @Option(
           option='C', longOption="config-dir", arg=REQUIRED, argName="dir",
           description="Configuration directory in addition to $HOME/.jsqsh.")
       public List<String> configDirectories = new ArrayList<String>();

       @Option(
           option='R', longOption="drivers", arg=REQUIRED, argName="file",
           description="Specifies additional drivers.xml files to be loaded")
       public List<String> driverFiles = new ArrayList<String>();
       
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
            
            System.exit(1);
        }
        
        /*
         * Display help if requested.
         */
        if (options.doHelp) {
            
            System.out.println(optParser.getUsage());
            System.exit(0);
        }

        configureLogging(options.debug);
        
        
        InputStream in = getInputStream(options);
        PrintStream out = getOutputStream(options);
        PrintStream err = System.err;
        
        if (in == null || out == null || err == null) {
            
            System.exit(1);
        }
        
        SqshContext sqsh = new SqshContext(options.readline);
        int rc = 0;

        for (String dir : options.configDirectories) {

            sqsh.addConfigurationDirectory(dir);
        }

        for (String file : options.driverFiles) {

            sqsh.addDriverFile(file);
        }
        
        try {
            
            Session session = sqsh.newSession();
            session.setIn(in, (options.inputFile != null), 
                (options.inputFile == null));
            session.setOut(out, options.outputFile != null);
            
            if (!doConnect(session, options)) {
                
                rc = 1;
            }
            else {
            
                rc = sqsh.run();
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
            
            if (in != System.in) {
                
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
     * Returns the input stream to be used by the session.
     * 
     * @param options Configuration options.
     * @return The input stream or null if the requested one cannot be
     *    opened.
     */
    private static InputStream getInputStream(Options options) {
        
        if (options.inputFile != null) {
            
            try {
                
                InputStream in = new BufferedInputStream(
                    new FileInputStream(options.inputFile));
                
                return in;
            }
            catch (IOException e) {
                
                System.err.println("Unable to open input file '" 
                    + options.inputFile + "' for read: "
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
        
        /*
         * If any one of our options having to do with establish a connection
         * have been provided, then connect!
         */
        if (options.getServer() != null
                || options.getPort() != -1
                || options.getCatalog() != null
                || options.getUsername() != null
                || options.getPassword() != null
                || options.getSid() != null
                || options.getJdbcClass() != null
                || options.getDriver() != null
                || options.getDomain() != null
                || options.arguments.size() > 0) {
            
            if (options.arguments.size() > 0) {
                
                String name = options.arguments.get(0);
                ConnectionDescriptorManager connDescMan = 
                    session.getConnectionDescriptorManager();
                
                ConnectionDescriptor savedOptions = connDescMan.get(name);
                if (savedOptions == null) {
                    
                    session.err.println("There is no saved connection "
                        + "information named '" + name + "'.");
                    
                    return false;
                }
                
                connDesc = connDescMan.merge(savedOptions, connDesc);
            }
            
            try {
                
                SQLContext ctx = 
                    session.getDriverManager().connect(session, connDesc);
                session.setSQLContext(ctx);
            }
            catch (SQLException e) {
                
                SQLTools.printException(session.err, e);
                return false;
            }
        }
        
        return true;
    }
}
