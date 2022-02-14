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

import org.sqsh.Buffer;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SessionRedrawBufferMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.NONE;

/**
 * Implements the \buf-load command.
 */
public class BufLoad extends Command {

    private static class Options extends SqshOptions {
        @OptionProperty(option = 'a', longOption = "append", arg = NONE,
                description = "Appends file contents to specified buffer")
        public boolean doAppend = false;

        @Argv(program = "\\buf-load", min = 1, max = 2, usage = "[-a] filename [dest-buf]")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;
        String filename = options.arguments.get(0);
        String destBuf = "!.";

        if (options.arguments.size() == 2) {
            destBuf = options.arguments.get(1);
        }

        Buffer buf = session.getBufferManager().getBuffer(destBuf);
        if (buf == null) {
            session.err.println("Specified destination buffer '" + destBuf + "' does not exist");
            return 1;
        }

        File file = new File(filename);
        if (!file.exists()) {
            session.err.println("File '" + filename + "' does not exist");
            return 1;
        }

        if (!options.doAppend) {
            buf.clear();
        }

        buf.load(file);
        if (buf == session.getBufferManager().getCurrent()) {
            throw new SessionRedrawBufferMessage();
        }

        return 0;
    }
}
