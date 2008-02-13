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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private static class Options {
        
       @Option(
           option='h', longOption="help", arg=NONE,
           description="Display help on command line arguments and exit")
       public boolean doHelp = false;
        
       @Option(
           option='S', longOption="server", arg=REQUIRED, argName="server",
           description="Name of the database server to connect to")
        public String server = null;
   
       @Option(
           option='p', longOption="port", arg=REQUIRED, argName="port",
           description="Listen port for the server to connect to")
       public int port = -1;
   
       @Option(
           option='D', longOption="database", arg=REQUIRED, argName="db",
           description="Database (catalog) context to use upon connection")
       public String database = null;
   
       @Option(
           option='U', longOption="user", arg=REQUIRED, argName="user",
           description="Username utilized for connection")
       public String username = null;
   
       @Option(
           option='P', longOption="password", arg=REQUIRED, argName="pass",
           description="Password utilized for connection")
       public String password = null;
       
       @Option(
           option='w', longOption="domain", arg=REQUIRED, argName="domain",
           description="Windows domain to be used for authentication")
           public String domain = null;
   
       @Option(
           option='s', longOption="sid", arg=REQUIRED, argName="SID",
           description="Instance id (e.g. Oracle SID) to utilize")
       public String SID = null;
   
       @Option(
           option='c', longOption="jdbc-class", arg=REQUIRED, argName="driver",
           description="JDBC driver class to utilize")
       public String driverClass = null;
       
       @Option(
           option='d', longOption="driver", arg=REQUIRED, argName="driver",
           description="Name of jsqsh driver to be used for connection")
       public String driverName = null;
       
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
       public String debug = null;
       
       @Argv(program="jsqsh", min=0, max=1, usage="[options] [jdbc-url]")
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
        
        /*
         * Turn on debugging if requested.
         */
        if (options.debug != null) {
            
            Logger log = Logger.getLogger(options.debug);
            if (log != null) {
                
                log.setLevel(Level.FINE);
                System.out.println("Debugging class '" + options.debug + "'");
            }
            else {
                
                System.err.println("--debug: Unable to find logger '"
                    + options.debug + "'");
                System.exit(1);
            }            
        }
        
        InputStream in = getInputStream(options);
        PrintStream out = getOutputStream(options);
        PrintStream err = System.err;
        
        if (in == null || out == null || err == null) {
            
            System.exit(1);
        }
        
        
        SqshContext sqsh = new SqshContext();
        int rc = 0;
        
        try {
            
            Session session = sqsh.newSession(in, out, err);
            if (!doConnect(session, options)) {
                
                rc = 1;
            }
            
            sqsh.run();
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
        }
        
        System.exit(rc);
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
        
        ArrayList<String> argv = new ArrayList<String>();
        if (options.server != null) {
            
            argv.add("-S");
            argv.add(options.server);
        }
        
        if (options.port != -1) {
            
            argv.add("-p");
            argv.add(Integer.toString(options.port));
        }
        
        if (options.database != null) {
            
            argv.add("-D");
            argv.add(options.database);
        }
        
        if (options.username != null) {
            
            argv.add("-U");
            argv.add(options.username);
            argv.add("-P");
            if (options.password != null) {
                
                argv.add(options.password);
            }
        }
        
        if (options.SID != null) {
            
            argv.add("-s");
            argv.add(options.SID);
        }
        
        if (options.driverClass != null) {
            
            argv.add("-c");
            argv.add(options.driverClass);
        }
        
        if (options.driverName != null) {
            
            argv.add("-d");
            argv.add(options.driverName);
        }
        
        if (options.domain != null) {
            
            argv.add("-w");
            argv.add(options.domain);
        }
        
        if (options.arguments.size() > 0) {
            
            argv.add(options.arguments.get(0));
        }
        
        if (argv.size() > 0) {
            
            if (session.execute("\\connect",
                    argv.toArray(new String[0])) != 0) {
                
                return false;
            }
        }
        
        return true;
    }
}
