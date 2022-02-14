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

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.Variable;
import org.sqsh.VariableManager;
import org.sqsh.options.Argv;

import java.util.ArrayList;
import java.util.List;


/**
 * Implements the \\unset command.
 */
public class Unset extends Command {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {
        @Argv(program = "\\unset", min = 1, max = 99, usage = "variable")
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
            session.err.println("Use: \\unset var_name [var_name ...]");
            return 1;
        }

        int ok = 0;
        VariableManager varMan = session.getVariableManager();
        for (int i = 0; i < options.arguments.size(); i++) {
            String varName = options.arguments.get(i);
            Variable var = varMan.getVariable(varName);

            if (var == null) {
                continue;
            }

            if (!var.isRemoveable()) {
                session.err.println("Variable \"" + varName + "\" is a configuration variable and cannot be unset");
                ok = 1;
            } else {
                varMan.remove(varName);
            }
        }

        return ok;
    }
}
