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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * This implements the public command line interface to kicking off
 * sqsh.
 */
public class JSqsh {
    
    private static class Options {
        
       @Option(name="-S",usage="Name of the server to connect to")
           public String server = null;
   
       @Option(name="-p",usage="Port that the server is listening on")
           public String port = null;
   
       @Option(name="-D",usage="Database (catalog) context to use")
           public String database = null;
   
       @Option(name="-U",usage="Username to connect with")
           public String username = null;
   
       @Option(name="-P",usage="Password to connect with")
           public String password = null;
   
       @Option(name="-s",usage="Oracle SID to connect to")
           public String SID = null;
   
       @Option(name="-c",usage="JDBC driver class to utilize")
           public String driverClass = null;
       
       @Option(name="-d",usage="Sqsh driver name to utilize for connection")
           public String driverName = null;
       
       @Option(name="-i",usage="Read input from external file")
           public String inputFile = null;
       
       @Option(name="-o",usage="Write output to external file")
           public String outputFile = null;
       
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }
   
    public static void main (String argv[]) {
        
        Options options = new Options();
        boolean doConnect = false;
        
        CmdLineParser parser = new CmdLineParser(options);
        
        try {
            
            parser.parseArgument(argv);
        }
        catch (CmdLineException e) {
            
            System.err.println(e.getMessage());
            System.err.println("Use: org.sqsh.JSqsh [options] [driver-name]");
            parser.printUsage(System.err);
            
            System.exit(1);
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
        
        StringBuilder connect = new StringBuilder();
        boolean doConnect = false;
        connect.append("\\connect ");
        
        if (options.server != null) {
            
            doConnect = true;
            session.setVariable(SQLDriver.SERVER_PROPERTY, options.server);
        }
        
        if (options.port != null) {
            
            doConnect = true;
            session.setVariable(SQLDriver.PORT_PROPERTY, options.port);
        }
        
        if (options.database != null) {
            
            doConnect = true;
            connect.append("-D \"").append(options.database).append("\" ");
        }
        
        if (options.username != null) {
            
            doConnect = true;
            session.setVariable(SQLDriver.USER_PROPERTY, options.username);
        }
        
        if (options.password != null) {
            
            doConnect = true;
            session.setVariable(SQLDriver.PASSWORD_PROPERTY, options.password);
        }
        
        if (options.SID != null) {
            
            doConnect = true;
            session.setVariable(SQLDriver.SID_PROPERTY, options.SID);
        }
        
        if (options.driverClass != null) {
            
            doConnect = true;
            connect.append("-c \"").append(options.driverClass).append("\" ");
        }
        
        if (options.driverName != null) {
            
            doConnect = true;
            session.setVariable("driver", options.driverName);
        }
        
        if (doConnect) {
            
            session.evaluate(connect.toString());
            return session.getLastCommandResult() == 0;
        }
        
        return true;
    }
}
