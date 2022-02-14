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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the \buf-edit command.
 */
public class Edit extends Command {

    private static class Options extends SqshOptions {
        @Argv(program = "\\buf-edit", min = 0, max = 2, usage = "[read-buf [write-buf]]")
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
        String readBuffer = "!.";
        String writeBuffer = "!.";

        if (options.arguments.size() > 2) {
            session.err.println("Use: \buf-edit [read-buf [write-buf]]");
            return 1;
        }

        BufferManager bufMan = session.getBufferManager();

        // If no arguments are provided then we check the current buffer, if it is empty we try to edit the previous buffer.
        if (options.arguments.size() == 0) {
            Buffer current = bufMan.getCurrent();
            if (current.getLineNumber() == 1 && bufMan.getBuffer("!..") != null) {
                readBuffer = "!..";
            }
        } else {
            readBuffer = options.arguments.get(0);
            if (options.arguments.size() == 2) {
                writeBuffer = options.arguments.get(1);
            }
        }
        Buffer readBuf = bufMan.getBuffer(readBuffer);
        if (readBuf == null) {
            session.err.println("Buffer '" + readBuffer + "' does not exist");
            return 1;
        }
        Buffer writeBuf = bufMan.getBuffer(writeBuffer);
        if (writeBuf == null) {
            session.err.println("Buffer '" + writeBuffer + "' does not exist");
            return 1;
        }

        String editor = getEditor(session);
        File tmpFile = null;
        Process s;
        try {
            tmpFile = File.createTempFile("jsqsh", ".sql");
            readBuf.save(tmpFile);
            s = session.getShellManager().detachShell(editor + " " + tmpFile);
            try {
                s.waitFor();
            } catch (InterruptedException e) {
                // IGNORED
            }
            writeBuf.load(tmpFile);
        } catch (IOException e) {
            session.err.println(e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        if (writeBuf == bufMan.getCurrent()) {
            throw new SessionRedrawBufferMessage();
        }
        return 0;
    }

    /**
     * Determines which editor to use.
     *
     * @param session The session used to retrieve variables
     * @return The editor.
     */
    public String getEditor(Session session) {
        String editor = session.getVariable("EDITOR");
        if (editor == null) {
            editor = session.getVariable("VISUAL");
        }
        if (editor == null) {
            String os = System.getProperty("os.name");
            if (os.indexOf("Wind") > 0) {
                editor = "notepad.exe";
            } else {
                editor = "vi";
            }
        }
        return editor;
    }
}
