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
import org.sqsh.SqshContext;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.REQUIRED;

/**
 * Implements the \eval command.
 */
public class Eval extends Command {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {
        @OptionProperty(option = 'n', longOption = "repeat", arg = REQUIRED, argName = "count",
                description = "Repeates the query execution count times")
        public int repeat = 1;

        @Argv(program = "\\eval", min = 1, max = 1, usage = "filename")
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
        if (options.arguments.size() != 1) {
            session.err.println("use: \\eval filename");
            return 1;
        }

        File filename = new File(options.arguments.get(0));
        if (!filename.canRead()) {
            session.err.println("Cannot open '" + filename + "' for read'");
            return 1;
        }

        SqshContext context = session.getContext();
        for (int i = 0; i < options.repeat; i++) {

            // Just like the \go command we set a variable that the SQL in the session can refer to know
            // what iteration it is in.
            if (options.repeat > 1) {
                session.setVariable("iteration", Integer.toString(i));
            }

            // Pay close attention, kiddies...first, we open our input file.
            InputStream in = new FileInputStream(filename);

            // Next, make the input of our session, this file. Note that we mark it as auto-close (the "true").
            // This will cause the session to close this descriptor after the command is finished executing
            // and we mark it as non-interactive (the "false").
            session.setIn(in, true, false);

            // Now we will recurse back into the context and ask it to execute the contents of this session. This
            // will return when the EOF is reached on the input stream.   Now, this session's input stream will still
            // be set to be the file we opened, but we don't really need to worry about that as the default behavior
            // of the session is to restore all input/output streams to the original state after the execution of
            // every command.
            context.run(session);
        }
        return 0;
    }
}
