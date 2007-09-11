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
package org.sqsh.commands;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.Command;
import org.sqsh.SQLAnalyzer;
import org.sqsh.SQLContext;
import org.sqsh.SQLDriver;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.analyzers.ANSIAnalyzer;

public class Connect
    extends Command {
    
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
        
        @Option(name="-n",usage="Create a new session for the connection")
            public boolean newSession = false;
        
        @Option(name="-d",usage="Name of driver to utilize for the connection")
            public String driverName = null;
        
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        Options options = new Options();
        
        int rc = parseOptions(session, argv, options);
        if (rc != 0) {
            
            return rc;
        }
        
        if (options.arguments.size() > 1) {
            
            session.err.println("Use: \\connect [options] [url]");
            printUsage(session, options);
            return 1;
        }
        
        /*
         * Turn command line options into properties.
         */
        if (options.driverName == null) {
            
            options.driverName = session.getVariable("driver");
            if (options.driverName == null) {
                
                options.driverName = "generic";
            }
        }
        
        if (options.driverName == null && options.arguments.size() == 0) {
            
            session.err.println("You must supply a driver name via -d or the "
                + "$driver variable or provide an explicit JDBC URL as the "
                + "last argument to \\connect");
            return 1;
        }
        
        Map<String, String> properties = getProperties(options);
        if (options.driverClass != null) {
            
            try {
                
                Class.forName(options.driverClass);
            }
            catch (Exception e) {
                
                session.err.println("Cannot load JDBC driver class '"
                    + options.driverClass + "'");
                return 1;
            }
        }
        
        /*
         * First we will try to use our driver smarts.
         */
        
        try {
            
            SQLContext sqlContext = null;
            String url = null;
            
            if (options.arguments.size() > 0) {
                
                url = options.arguments.get(0);
            }
            
            sqlContext = session.getDriverManager().connect(
                options.driverName, session, properties, url);
            
            /*
             * If we are asked to create a new session, then we will do so.
             */
            if (options.newSession) {
                
                Session newSession = session.getContext().newSession(
                    session.in, session.out, session.err);
                newSession.setInteractive(session.isInteractive());
                
                exportProperties(newSession, properties);
                newSession.setSQLContext(sqlContext);
                
                /*
                 * This magic exception is caught by the owning sqsh context
                 * to tell it that we want to switch to the newly created
                 * session.
                 */
                throw new SqshContextSwitchMessage(session, newSession);
            }
            else {
                
                exportProperties(session, properties);
                session.setSQLContext(sqlContext);
            }
        }
        catch (SQLException e) {
            
            printException(session.err, e);
        }
        
        return 0;
    }
    
    /**
     * Exports the properties that were used to establish the connection
     * out to the session.
     * 
     * @param properties The properties.
     */
    private void exportProperties(Session session,
            Map<String, String> properties) {
        
        for (String name : properties.keySet()) {
            
            session.setVariable(name, properties.get(name));
        }
    }
    
    private Map<String, String> getProperties(Options options) {
        
        Map<String, String> properties = new HashMap<String, String>();
        if (options.server != null) {
            
            properties.put(SQLDriver.SERVER_PROPERTY, options.server);
        }
        
        if (options.port != null) {
            
            properties.put(SQLDriver.PORT_PROPERTY, options.port);
        }
        
        if (options.username != null) {
            
            properties.put(SQLDriver.USER_PROPERTY, options.username);
        }
        
        if (options.password != null) {
            
            properties.put(SQLDriver.PASSWORD_PROPERTY, options.password);
        }
        
        if (options.SID != null) {
            
            properties.put(SQLDriver.SID_PROPERTY, options.SID);
        }
        
        if (options.database != null) {
            
            properties.put(SQLDriver.DATABASE_PROPERTY, options.SID);
        }
        
        return properties;
    }

}
