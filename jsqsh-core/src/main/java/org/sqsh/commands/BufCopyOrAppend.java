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
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SessionRedrawBufferMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the \buf-append command.
 */
public class BufCopyOrAppend extends Command {

    private static class Options extends SqshOptions {
        @Argv(program = "\\buf-copy", min = 1, max = 3, usage = "[-G] [alias=command_line")
        public List<String> arguments = new ArrayList<>();
    }

    /**
     * Returns the set of options required for this command.
     */
    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;
        if (options.arguments.size() < 1 || options.arguments.size() > 2) {
            session.err.println("Use: " + getName() + " src-buf [dst-buf]");
            return 1;
        }

        String srcBuf = options.arguments.get(0);
        String destBuf = "!.";

        if (options.arguments.size() == 2) {
            destBuf = options.arguments.get(1);
        }

        BufferManager bufMan = session.getBufferManager();

        Buffer src = bufMan.getBuffer(srcBuf);
        if (src == null) {
            session.err.println("The specified source buffer '" + srcBuf + "' does not exist");
            return 1;
        }

        Buffer dst = bufMan.getBuffer(destBuf);
        if (dst == null) {
            session.err.println("The specified destination buffer '" + destBuf + "' does not exist");
            return 1;
        }

        // If this is the "buf-copy" command, then clear the destination buffer before proceeding.
        if (getName().contains("copy")) {
            dst.clear();
        }
        boolean doRedraw = (dst == bufMan.getCurrent());
        dst.add(src.toString());

        // If we just manipulated the current buffer, then ask the session to redraw the current buffer.
        if (doRedraw) {
            throw new SessionRedrawBufferMessage();
        }

        return 0;
    }
}
