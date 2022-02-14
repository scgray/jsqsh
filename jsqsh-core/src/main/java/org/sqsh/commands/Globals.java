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
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the "\globals" command to display all global variables (or anything else the driver says is "global").
 */
public class Globals extends Command {

    private static class Options extends SqshOptions {
        @Argv(program = "\\globals", min = 0, max = 0)
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions options) throws Exception {
        ConnectionContext conn = session.getConnectionContext();
        if (conn == null) {
            session.err.println("No connection established");
            return 1;
        }

        List<String> vars = conn.getGlobals();
        Collections.sort(vars);
        ColumnDescription[] columns = new ColumnDescription[1];
        columns[0] = new ColumnDescription("Global", -1);
        Renderer renderer = session.getRendererManager().getCommandRenderer(session);
        renderer.header(columns);
        for (String var : vars) {
            String row[] = new String[1];
            row[0] = var;
            renderer.row(row);
        }

        renderer.flush();
        return 0;
    }
}
