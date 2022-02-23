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
package org.sqsh.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessUtils {

    /**
     * Helper class to consume the output of a process. This spawns a thread that will read from an input of a process
     * until there is either an error or until the end of the stream is reached.  The output will be accumulated in an
     * in-memory buffer that can be retrieved when the process has completed execution.
     */
    public static class Consumer extends Thread {
        private final BufferedReader in;
        private final StringBuilder buffer;

        public Consumer(InputStream stream) {
            in = new BufferedReader(new InputStreamReader(stream));
            this.buffer = new StringBuilder();
        }

        public String getOutput() {
            return buffer.toString();
        }

        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    if (buffer.length() > 0) {
                        buffer.append("\n");
                    }
                    buffer.append(line);
                }
            } catch (IOException e) {
                // IGNORED
            } finally {
                try {
                    in.close();
                } catch (IOException e) { /* IGNORED */ }
            }
        }
    }
}
