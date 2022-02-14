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

import org.sqsh.CannotSetValueError;
import org.sqsh.ColumnDescription;
import org.sqsh.ColumnDescription.Alignment;
import org.sqsh.ColumnDescription.OverflowBehavior;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.Variable;
import org.sqsh.VariableManager;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.NONE;

/**
 * Implements the 'set' command in sqsh.
 */
public class Set extends Command {

    // Used to contain the command line options that were passed in by the caller.
    private static class Options extends SqshOptions {
        @OptionProperty(option = 'x', longOption = "export", arg = NONE,
                description = "Export the variable")
        public boolean doExport = false;

        @OptionProperty(option = 'l', longOption = "local", arg = NONE,
                description = "Define the variable locally to this session")
        public boolean isLocal = false;

        @Argv(program = "\\set", min = 0, max = 3, usage = "[-x] [-l] [var=value]")
        public List<String> arguments = new ArrayList<>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;

        // Gotta have at least one option (case of "x=y");
        if (options.arguments.size() == 0) {
            VariableManager varMan = session.getVariableManager();
            Variable[] vars = varMan.getVariables(true);
            Arrays.sort(vars);
            ColumnDescription[] columns = new ColumnDescription[2];
            columns[0] = new ColumnDescription("Variable", -1, Alignment.LEFT, OverflowBehavior.WRAP, false);
            columns[1] = new ColumnDescription("Value", -1);
            Renderer renderer = session.getRendererManager().getCommandRenderer(session);
            renderer.header(columns);
            for (int i = 0; i < vars.length; i++) {
                String[] row = new String[2];
                row[0] = vars[i].getName();
                row[1] = vars[i].toString();
                renderer.row(row);
            }
            renderer.flush();
            return 0;
        }

        // We will allow the user to be a little lazy and specity the set as any one of the following:
        //    x=y
        //    x = y
        //    x =y
        String name;
        StringBuilder value = new StringBuilder();
        int argIdx;
        String s = options.arguments.get(0);
        int equalIdx = s.indexOf('=');

        // If we've got an equals contained in argv[0]...
        if (equalIdx > 0) {

            // Everything left of the equals is the variable name.
            name = s.substring(0, equalIdx);

            // If there is anything following the equals, then use it.
            if (equalIdx < s.length() - 1) {
                value.append(s.substring(equalIdx + 1));
            }
            argIdx = 1;
        } else {
            name = options.arguments.get(0);

            // No equals in argv[0], it better be in argv[1];
            if (options.arguments.size() < 2) {
                printUsage(session.err);
                return 1;
            }

            // Get the next argument, and make sure it either is or begins with an equals.
            s = options.arguments.get(1);
            if (!s.startsWith("=")) {
                printUsage(session.err);
                return 1;
            }
            if (!s.isEmpty()) {
                value.append(s.substring(1));
            }
            argIdx = 2;
        }
        for (; argIdx < options.arguments.size(); argIdx++) {
            if (value.length() > 0) {
                value.append(' ');
            }
            value.append(options.arguments.get(argIdx));
        }
        try {
            if (options.isLocal || session.getVariableManager().containsLocal(name)) {
                session.getVariableManager().put(name, value.toString(), options.doExport);
            } else {
                session.getContext().getVariableManager().put(name, value.toString(), options.doExport);
            }
        } catch (CannotSetValueError e) {
            session.err.println(e.getMessage());
            return 1;

        }
        return 0;
    }
}
