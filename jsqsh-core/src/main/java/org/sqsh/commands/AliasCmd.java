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

import org.sqsh.Alias;
import org.sqsh.AliasManager;
import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.NONE;

public class AliasCmd extends Command {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {
        @OptionProperty(option = 'G', longOption = "global", arg = NONE,
                description = "Make the alias global (apply to whole line)")
        public boolean isGlobal = false;

        @Argv(program = "\\alias", min = 0, max = 3, usage = "[-G] [alias=command_line")
        public List<String> arguments = new ArrayList<>();
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

        if (options.arguments.size() == 0) {
            AliasManager aliasMan = session.getAliasManager();
            Alias[] aliases = aliasMan.getAliases();
            Arrays.sort(aliases);
            ColumnDescription[] columns = new ColumnDescription[3];
            columns[0] = new ColumnDescription("Alias", false);
            columns[1] = new ColumnDescription("Global?", false);
            columns[2] = new ColumnDescription("Text", true);
            Renderer renderer = session.getRendererManager().getCommandRenderer(session);
            renderer.header(columns);
            for (Alias alias : aliases) {
                String[] row = new String[3];
                row[0] = alias.getName();
                row[1] = alias.isGlobal() ? "Y" : "N";
                row[2] = alias.getText();
                renderer.row(row);
            }
            renderer.flush();
            return 0;
        }

        // We will allow the user to be a little lazy and specify the alias as any one of the following:
        //    x=y
        //    x = y
        //    x =y
        String name;

        StringBuilder value = new StringBuilder();
        int argIdx = 1;
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
            if (s.length() > 0) {
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

        session.getAliasManager().addAlias(new Alias(name, value.toString(), options.isGlobal));
        return 0;
    }
}
