/*
 * Copyright 2007-2022 Scott C. Gray
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

/**
 * Implements the \connect command.
 */
public class Connect extends Command {

    private static class Options extends ConnectionDescriptor {

        @OptionProperty(option = 'n', longOption = "new-session", arg = NONE,
                description = "Create a new session for the connection")
        public boolean newSession = false;

        @OptionProperty(option = 'a', longOption = "add", arg = REQUIRED,
                description = "Save the command line as a named connection")
        public String add = null;

        @OptionProperty(option = 'R', longOption = "update", arg = NONE,
                description = "Update a named connection with the current settings")
        public boolean update = false;

        @OptionProperty(option = 'l', longOption = "list", arg = NONE,
                description = "List all defined connections")
        public boolean list = false;

        @OptionProperty(option = 'r', longOption = "remove", arg = NONE,
                description = "Remove a connection definition")
        public boolean remove = false;

        @OptionProperty(option = 'x', longOption = "show-password", arg = NONE,
                description = "Displays connect password in clear-text")
        public boolean showPassword = false;

        @OptionProperty(option = 'e', longOption = "setup", arg = NONE,
                description = "Enters the connection setup wizard")
        public boolean doSetup = false;

        @Argv(program = "\\connect", min = 0, max = 1, usage = "[options] [named-connection]")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;

        ConnectionDescriptor connDesc = options;
        if (options.remove && options.arguments.size() < 1) {
            session.err.println("A saved connection name must be provided with the --remove option");
            return 1;
        }

        // Sanity checking.
        if (options.update && options.add != null) {
            session.err.println("The --update (-x) and --add (-a) options are mutually exclusive");
            return 1;
        }
        if (options.doSetup) {
            Command setup = session.getCommandManager().getCommand("\\setup");
            return setup.execute(session, new String[]{"connections"});
        }

        // If the current set of options include the "name" argument then this is a saved connection profile.
        if (options.arguments.size() > 0) {
            String name = options.arguments.get(0);
            ConnectionDescriptorManager connDescMan = session.getConnectionDescriptorManager();

            // An update is actually a re-adding of an existing connection.
            if (options.update) {
                options.add = name;
            }

            ConnectionDescriptor savedOptions = connDescMan.get(name);
            if (savedOptions == null) {
                session.err.println("There is no saved connection information named '"
                        + name + "'. Use --list to show available " + "connections");
                return 1;
            }

            if (options.remove) {
                connDescMan.remove(name);
                connDescMan.save();
                return 0;
            }

            // If we had previously saved away some options, then we take those options and merge in any additional
            // options that the caller may have requested.
            if (savedOptions != null) {
                connDesc = connDescMan.merge(savedOptions, connDesc);
            }

            if (options.toggleAutoconnect) {
                connDesc.setAutoconnect(!connDesc.isAutoconnect());
            }
        } else {
            if (options.update) {
                session.err.println("The --update (-x) option may only be provided " + "in conjunction with an existing named connection");
                return 1;
            }
        }

        // If this is the first time that we have seen this name or the user has added different options, then update
        // and re-save our settings.
        if (options.add != null) {
            ConnectionDescriptorManager connDescMan = session.getConnectionDescriptorManager();
            connDesc.setName(options.add);
            connDescMan.put(connDesc);
            connDescMan.save();
            return 0;
        }

        // If we were asked to list the connections then do so and just return.
        if (options.list) {
            doList(session, options.showPassword);
            return 0;
        }

        // First we will try to use our driver smarts.
        try {
            SQLConnectionContext sqlContext = null;
            sqlContext = session.getDriverManager().connect(session, connDesc);

            // If we are asked to create a new session, then we will do so.
            if (options.newSession) {
                Session newSession = session.getContext().newSession(session.in, session.out, session.err, session.isInteractive());
                newSession.setConnectionContext(sqlContext);

                // This magic exception is caught by the owning sqsh context to tell it that we want to switch
                // to the newly created session.
                throw new SqshContextSwitchMessage(session, newSession);
            } else {
                session.setConnectionContext(sqlContext);
            }
        } catch (SQLException e) {
            SQLTools.printException(session, e);
        }
        return 0;
    }

    private void doList(Session session, boolean showPassword) {
        ColumnDescription[] columns = new ColumnDescription[12];
        boolean hasStar = false;
        columns[0] = new ColumnDescription("Name", false);
        columns[1] = new ColumnDescription("Driver", false);
        columns[2] = new ColumnDescription("Server");
        columns[3] = new ColumnDescription("Port");
        columns[4] = new ColumnDescription("Database");
        columns[5] = new ColumnDescription("Username");
        columns[6] = new ColumnDescription("Password");
        columns[7] = new ColumnDescription("Domain");
        columns[8] = new ColumnDescription("Class");
        columns[9] = new ColumnDescription("URL");
        columns[10] = new ColumnDescription("Properties");
        columns[11] = new ColumnDescription("URL Vars");

        DataFormatter formatter = session.getDataFormatter();
        Renderer renderer = session.getRendererManager().getCommandRenderer(session);
        renderer.header(columns);

        ConnectionDescriptor[] connDescs = session.getConnectionDescriptorManager().getAll();
        Arrays.sort(connDescs, Comparator.comparing(ConnectionDescriptor::getName));

        for (ConnectionDescriptor connDesc : connDescs) {
            String[] row = new String[columns.length];
            String name = connDesc.getName();
            if (connDesc.isAutoconnect()) {
                name = "*" + name;
                hasStar = true;
            }

            int idx = 0;
            row[idx++] = name;
            row[idx++] = (connDesc.getDriver() == null ? formatter.getNull() : connDesc.getDriver());
            row[idx++] = (connDesc.getServer() == null ? formatter.getNull() : connDesc.getServer());
            row[idx++] = (connDesc.getPort() < 0 ? formatter.getNull() : Integer.toString(connDesc.getPort()));
            row[idx++] = (connDesc.getCatalog() == null ? formatter.getNull() : connDesc.getCatalog());
            row[idx++] = (connDesc.getUsername() == null ? formatter.getNull() : connDesc.getUsername());

            if (showPassword) {
                row[idx++] = (connDesc.getPassword() == null ? formatter.getNull() : connDesc.getPassword());
            } else {
                row[idx++] = (connDesc.getPassword() == null ? formatter.getNull() : "*******");
            }

            row[idx++] = (connDesc.getDomain() == null ? formatter.getNull() : connDesc.getDomain());
            row[idx++] = (connDesc.getJdbcClass() == null ? formatter.getNull() : connDesc.getJdbcClass());
            row[idx++] = (connDesc.getUrl() == null ? formatter.getNull() : connDesc.getUrl());
            row[idx] = null;

            List<String> props = connDesc.getProperties();
            if (props.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < props.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(props.get(i));
                }
                row[idx] = sb.toString();
            }
            ++idx;

            row[idx] = null;

            List<String> vars = connDesc.getUrlVariables();
            if (vars.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < vars.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(vars.get(i));
                }
                row[idx] = sb.toString();
            }

            renderer.row(row);
        }
        renderer.flush();

        if (hasStar) {
            session.out.println();
            session.out.println("* = Connection is set for autoconnect");
            session.out.println();
        }
    }
}
