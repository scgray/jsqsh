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

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.OPTIONAL;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sqsh.Command;
import org.sqsh.ConnectionDescriptor;
import org.sqsh.SQLContext;
import org.sqsh.SQLDriver;
import org.sqsh.SQLDriverManager;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

/**
 * Implements the \connect command.
 */
public class Connect
    extends Command {
    
    private static class Options
       extends ConnectionDescriptor {
        
        @Option(
            option='n', longOption="new-session", arg=NONE,
            description="Create a new session for the connection")
        public boolean newSession = false;
        
        @Argv(program="\\connect", min=0, max=1, usage="[options] [jdbc-url]")
        public List<String> arguments = new ArrayList<String>();
    }
   
    @Override
   	public SqshOptions getOptions() {
       
        return new Options();
   	}

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        
        /*
         * Turn command line options into properties.
         */
        if (options.getDriver() == null) {
            
            options.setDriver(session.getVariable("dflt_driver"));
            if (options.getDriver() == null) {
                
                options.setDriver("generic");
            }
        }
        
        if (options.getDriver() == null
                && options.arguments.size() == 0) {
            
            session.err.println("You must supply a driver name via -d or the "
                + "$driver variable or provide an explicit JDBC URL as the "
                + "last argument to \\connect");
            return 1;
        }
        
        Map<String, String> properties = getProperties(session, options);
        if (options.getJdbcClass() != null) {
            
            try {
                
                session.getDriverManager().loadClass(
                    options.getJdbcClass());
            }
            catch (Exception e) {
                
                session.err.println("Cannot load JDBC driver class '"
                    + options.getJdbcClass() + "': " + e.getMessage());
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
                
                sqlContext = session.getDriverManager().connect(
                    options.getJdbcClass(), options.arguments.get(0), session,
                    properties);
            }
            else {
                
                sqlContext = session.getDriverManager().connect(
                	options.getDriver(), session, properties);
            }
            
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
                session.setVariable("driver", options.getDriver());
                session.setSQLContext(sqlContext);
            }
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.err, e);
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
    
    /**
     * Given the options that the user passed in, creates a map of
     * properties that are required by {@link SQLDriverManager#connect(String, Session, Map)}
     * in order to establish a connection. For a given property, such as
     * {@link SQLDriver#SERVER_PROPERTY}, the value is established by the
     * first of the following that is available:
     * 
     * <ol>
     *   <li> Using the option provided on the command line (e.g. "-S")
     *   <li> Looking the in the session for variable of the name
     *        "dflt_&lt;property&gt;" (e.g. "dflt_server"). 
     * </ol>
     * 
     * If none of those is available, then the property is not passed in 
     * and it is up to the {@link SQLDriver} to provide a default.
     * 
     * @param session The session used to look up the properties.
     * @param options The options the user provided.
     * @return A map of properties.
     */
    private Map<String, String> getProperties(
            Session session, Options options) {
        
        Map<String, String> properties = new HashMap<String, String>();
        
        setProperty(properties, session,
            SQLDriver.SERVER_PROPERTY, options.getServer());
        setProperty(properties, session,
            SQLDriver.PORT_PROPERTY, 
                (options.getPort() == -1
                        ? null : Integer.toString(options.getPort())));
        setProperty(properties, session,
            SQLDriver.USER_PROPERTY, options.getUsername());
        setProperty(properties, session,
            SQLDriver.PASSWORD_PROPERTY, options.getPassword());
        setProperty(properties, session,
            SQLDriver.SID_PROPERTY, options.getSid());
        setProperty(properties, session,
            SQLDriver.DATABASE_PROPERTY, options.getCatalog());
        setProperty(properties, session,
            SQLDriver.DOMAIN_PROPERTY, options.getDomain());
        
        return properties;
    }
    
    private void setProperty(Map<String, String>map, Session session,
            String name, String value) {
        
        if (value == null) {
            
            value = session.getVariable("dflt_" + name);
        }
        
        if (value != null) {
            
            map.put(name, value);
        }
    }
}
