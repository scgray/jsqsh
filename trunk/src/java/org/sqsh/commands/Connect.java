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
package org.sqsh.commands;

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.ConnectionDescriptor;
import org.sqsh.ConnectionDescriptorManager;
import org.sqsh.DataFormatter;
import org.sqsh.Renderer;
import org.sqsh.SQLConnectionContext;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

/**
 * Implements the \connect command.
 */
public class Connect
    extends Command {
    
    private static class Options
       extends ConnectionDescriptor {
        
        @OptionProperty(
            option='n', longOption="new-session", arg=NONE,
            description="Create a new session for the connection")
        public boolean newSession = false;
        
        @OptionProperty(
            option='a', longOption="add", arg=REQUIRED,
            description="Save the command line as a named connection")
        public String add = null;
        
        @OptionProperty(
            option='l', longOption="list", arg=NONE,
            description="List all defined connections")
        public boolean list = false;
        
        @OptionProperty(
            option='r', longOption="remove", arg=NONE,
            description="Remove a connection definition")
        public boolean remove = false;

        @OptionProperty(
            option='x', longOption="show-password", arg=NONE,
            description="Displays connect password in clear-text")
        public boolean showPassword = false;
        
        @Argv(program="\\connect", min=0, max=1,
              usage="[options] [named-connection]")
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
        ConnectionDescriptor connDesc = (ConnectionDescriptor) options;
        
        if (options.remove && options.arguments.size() < 1) {
            
            session.err.println("A saved connection name must be provided"
                + "with the --remove option");
            return 1;
        }
        
        /*
         * If the current set of options include the "name" argument
         * then this is a saved connection profile.
         */
        if (options.arguments.size() > 0) {
            
            String name = options.arguments.get(0);
            ConnectionDescriptorManager connDescMan = 
                session.getConnectionDescriptorManager();
            
            ConnectionDescriptor savedOptions = connDescMan.get(name);
            if (savedOptions == null) {
                
                session.err.println("There is no saved connection information "
                    + "named '" + name + "'. Use --list to show available "
                    + "connections");
                return 1;
            }
            
            if (options.remove) {
                
                connDescMan.remove(name);
                connDescMan.save();
                return 0;
            }
            
            /*
             * If we had previously saved away some options, then we take
             * those options and merge in any additional options that the
             * caller may have requested.
             */
            if (savedOptions != null) {
                
                connDesc = connDescMan.merge(savedOptions, connDesc);
            }
        }
        
        /*
         * If this is the first time that we have seen this name
         * or the user has added different options, then update
         * and re-save our settings.
         */
        if (options.add != null) {
            
            ConnectionDescriptorManager connDescMan = 
                session.getConnectionDescriptorManager();
                
            connDesc.setName(options.add);
            connDescMan.put(connDesc);
            connDescMan.save();
            return 0;
        }
        
        /*
         * If we were asked to list the connections then do so and
         * just return.
         */
        if (options.list) {
            
            doList(session, options.showPassword);
            return 0;
        }
        
        /*
         * First we will try to use our driver smarts.
         */
        try {
            
            SQLConnectionContext sqlContext = null;
            
            sqlContext = session.getDriverManager().connect(
                session, connDesc);
            
            /*
             * If we are asked to create a new session, then we will do so.
             */
            if (options.newSession) {
                
                Session newSession = session.getContext().newSession(
                    session.in, session.out, session.err);
                newSession.setInteractive(session.isInteractive());
                newSession.setConnectionContext(sqlContext);
                
                /*
                 * This magic exception is caught by the owning sqsh context
                 * to tell it that we want to switch to the newly created
                 * session.
                 */
                throw new SqshContextSwitchMessage(session, newSession);
            }
            else {
                
                session.setConnectionContext(sqlContext);
            }
        }
        catch (SQLException e) {
            
            SQLTools.printException(session, e);
        }
        
        return 0;
    }
    
    private void doList(Session session, boolean showPassword) {
        
        ColumnDescription []columns = new ColumnDescription[10];
        columns[0] = new ColumnDescription("Name", -1);
        columns[1] = new ColumnDescription("Driver", -1);
        columns[2] = new ColumnDescription("Server", -1);
        columns[3] = new ColumnDescription("Port", -1);
        columns[4] = new ColumnDescription("SID", -1);
        columns[5] = new ColumnDescription("Username", -1);
        columns[6] = new ColumnDescription("Password", -1);
        columns[7] = new ColumnDescription("Domain", -1);
        columns[8] = new ColumnDescription("Class", -1);
        columns[9] = new ColumnDescription("URL", -1);
        
        DataFormatter formatter =
            session.getDataFormatter();
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        renderer.header(columns);
        
        ConnectionDescriptor []connDescs = 
            session.getConnectionDescriptorManager().getAll();
        
        Arrays.sort(connDescs, new Comparator<ConnectionDescriptor>() {
                
                public int compare(ConnectionDescriptor c1,
                        ConnectionDescriptor c2) {
                    
                    return c1.getName().compareTo(c2.getName());
                }
            });
        
        for (ConnectionDescriptor connDesc : connDescs) {
            
            String row[] = new String[10];
            row[0] = connDesc.getName();
            row[1] = (connDesc.getDriver() == null ?
                        formatter.getNull() : connDesc.getDriver());
            row[2] = (connDesc.getServer() == null ?
                        formatter.getNull() : connDesc.getServer());
            row[3] = (connDesc.getPort() < 0 ?
                        formatter.getNull() : Integer.toString(connDesc.getPort()));
            row[4] = (connDesc.getSid() == null ?
                        formatter.getNull() : connDesc.getSid());
            row[5] = (connDesc.getUsername() == null ?
                        formatter.getNull() : connDesc.getUsername());

            if (showPassword) {

                row[6] = (connDesc.getPassword() == null ?
                            formatter.getNull() : connDesc.getPassword());
            }
            else {

                row[6] = (connDesc.getPassword() == null ?
                            formatter.getNull() : "*******");
            }

            row[7] = (connDesc.getDomain() == null ?
                        formatter.getNull() : connDesc.getDomain());
            row[8] = (connDesc.getJdbcClass() == null ?
                        formatter.getNull() : connDesc.getJdbcClass());
            row[9] = (connDesc.getUrl() == null ? 
                        formatter.getNull() : connDesc.getUrl());
            
            renderer.row(row);
        }
        
        renderer.flush();
    }
}
