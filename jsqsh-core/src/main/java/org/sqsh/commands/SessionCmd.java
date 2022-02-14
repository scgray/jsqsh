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
import org.sqsh.ConnectionContext;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

import java.util.ArrayList;
import java.util.List;

public class SessionCmd extends Command {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {
        @Argv(program = "\\session", min = 0, max = 1, usage = "[session_id]")
        public List<String> arguments = new ArrayList<String>();
    }

    /**
     * Return our overridden options.
     */
    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;
        if (options.arguments.size() > 1) {
            session.err.println("Use: \\session [session_id]");
            return 1;
        }
        SqshContext ctx = session.getContext();

        // If a context id is supplied, then try to switch to it.
        if (options.arguments.size() == 1) {
            boolean ok = true;
            Session newSession = null;
            int id;

            // The special id of "-" will cause a null session id to be thrown back to the SqshContext which will request
            // it to switch to the previous session.
            if (!options.arguments.get(0).equals("-")) {
                try {
                    id = Integer.parseInt(options.arguments.get(0));
                    newSession = ctx.getSession(id);
                    if (newSession == null) {
                        ok = false;
                    }
                } catch (Exception e) {
                    ok = false;
                }

                if (!ok) {
                    session.err.println("Invalid session number '" + options.arguments.get(0) + "'. Run \\session with no arguments to " + "see a list of available sessions.");
                    return 1;
                }
            }
            throw new SqshContextSwitchMessage(session, newSession);
        }

        Session[] sessions = ctx.getSessions();
        ColumnDescription[] columns = new ColumnDescription[3];

        columns[0] = new ColumnDescription("Id", -1);
        columns[1] = new ColumnDescription("Username", -1);
        columns[2] = new ColumnDescription("URL", -1);

        Renderer renderer = session.getRendererManager().getCommandRenderer(session);

        renderer.header(columns);

        for (int i = 0; i < sessions.length; i++) {
            String[] row = new String[3];
            if (sessions[i] == ctx.getCurrentSession()) {
                row[0] = "* " + sessions[i].getId();
            } else {
                row[0] = "  " + sessions[i].getId();
            }
            ConnectionContext connection = sessions[i].getConnectionContext();
            if (connection == null) {
                row[1] = "-";
                row[2] = "-";
            } else {
                row[1] = sessions[i].getVariable("user");
                row[2] = connection.toString();
            }
            renderer.row(row);
        }
        renderer.flush();
        return 0;
    }

}
