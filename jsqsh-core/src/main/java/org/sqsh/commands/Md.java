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

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.MarkdownFormatter;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.util.ArrayList;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.REQUIRED;

/*
This is some sample text that will wrap some and 
we will make sure it looks good

 * Bullet #1

And this starts another paragraph that is also
going to wrap and wrap
\md -w 20
 */

/**
 * Command to test the markdown formatter.
 */
public class Md extends Command {

    /**
     * Used to contain the command line options that were passed in by the caller.
     */
    private static class Options extends SqshOptions {
        @OptionProperty(option = 'w', longOption = "width", arg = REQUIRED, argName = "width",
                description = "Sets the screen width for display")
        public int width = 80;

        @Argv(program = "\\md", min = 0, max = 0)
        public List<String> arguments = new ArrayList<>();
    }

    public SqshOptions getOptions() {
        return new Options();
    }

    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;
        BufferManager bufferMan = session.getBufferManager();
        String text = bufferMan.getCurrent().toString();

        if (session.isInteractive()) {
            bufferMan.newBuffer();
        } else {
            bufferMan.getCurrent().clear();
        }

        MarkdownFormatter formatter = new MarkdownFormatter(options.width, session.out);
        formatter.format(text);

        return 0;
    }
}
