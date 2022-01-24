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
package org.sqsh.shell;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The ShellManager allows the creation of external processes that receive their input from--or send their output to--
 * jsqsh. It is, for the most part, a simple wrapper around {@link ProcessBuilder}.
 */
public class ShellManager {
    private static final Logger LOG = Logger.getLogger(ShellManager.class.getName());

    private static ShellManager instance = null;

    /**
     * This array represents the command line that will be utilized to execute all shell commands, any entry
     * that contains a question mark (?) will be replaced with the command that is being executed. For example:
     *
     * <pre>
     *     [0] = /bin/sh
     *     [1] = -c
     *     [2] = ?
     * </pre>
     *
     * means that when a command, such as "ls -al" is executed, it will be executed as
     *
     * <pre>
     *    /bin/sh -c "ls -al"
     * </pre>
     */
    private String[] shellCommand;

    /**
     * Creates a shell manager.
     */
    private ShellManager() {
        final String os = System.getProperty("os.name");
        if (os.contains("Wind")) {
            shellCommand = new String[]{"cmd.exe", "/c", "?"};
        } else {
            shellCommand = new String[]{"/bin/sh", "-c", "?"};
        }
    }

    /**
     * Returns an instance of the ShellManager and initializes the JNI layer.
     *
     * @return The ShellManager.
     */
    public static synchronized ShellManager getInstance() {
        if (instance == null) {
            instance = new ShellManager();
        }

        return instance;
    }

    /**
     * Assigns the command that will be utilized when executing a command shell. This is to be a comma delimited
     * set of arguments with a question mark (?) acting as a place holder for the command to be executed within
     * the shell, for example:
     *
     * <pre>
     *     bin/sh,-c,?
     * </pre>
     *
     * means that when a command, such as "ls -al" is executed, it will be executed as
     *
     * <pre>
     *    /bin/sh -c "ls -al"
     * </pre>
     *
     * @param cmd Comma delimited list of arguments.
     */
    public void setShellCommand(String cmd) {
        shellCommand = cmd.split(",");
    }

    /**
     * Returns the current shell command string.
     *
     * @return The current shell command string.
     */
    public String getShellCommand() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < shellCommand.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(shellCommand[i]);
        }

        return sb.toString();
    }

    /**
     * Given a command to be executed in a sub-shell generates the full shell command execution arguments that
     * needs to be executed.
     *
     * @param cmd The command string to be executed.
     * @return The full shell command arguments.
     */
    private String[] getShellExecution(String cmd) {
        final String[] args = new String[shellCommand.length];
        for (int i = 0; i < shellCommand.length; i++) {
            if ("?".equals(shellCommand[i])) {
                args[i] = cmd;
            } else {
                args[i] = shellCommand[i];
            }
        }

        return args;
    }

    /**
     * Spawns a command string as a sub-process that expects to receive its input from the JVM. The stdout and
     * stderr of the sub-process is sent to the calling terminal.
     *
     * @param cmd The command to be executed in the sub-shell
     * @return A handle to the spawned process.
     */
    public Process pipeShell(String cmd) throws ShellException {
        final String[] argv = getShellExecution(cmd);

        final Process p;
        try {
            return new ProcessBuilder()
                    .command(argv)
                    .redirectInput(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
        } catch (Exception e) {
            throw new ShellException(e.getMessage());
        }
    }

    /**
     * Spawns a shell in which the stdin, stdout, and stderr are all inherited from the terminal in which
     * the JVM is running.
     *
     * @param cmd The command to be executed in the sub-shell
     * @return A handle to the spawned process.
     */
    public Process detachShell(String cmd) throws ShellException {
        final String[] argv = getShellExecution(cmd);

        final Process pty;

        try {
            return new ProcessBuilder()
                    .command(argv)
                    .inheritIO()
                    .start();
        } catch (IOException e) {
            throw new ShellException(e.getMessage());
        }
    }

    /**
     * Spawns a shell to execute a command line as a sub-process that expects to receive its input from the console.
     * The stdout of the process is available to the caller.
     *
     * @param cmd The command to be executed in the sub-shell
     * @return A handle to the spawned process.
     */
    public Process readShell(String cmd, boolean combineError) throws ShellException {
        String[] argv = getShellExecution(cmd);

        final Process p;
        try {
            final ProcessBuilder pb = new ProcessBuilder()
                    .command(argv)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT);

            if (combineError) {
                pb.redirectErrorStream(true);
            } else {
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            }

            return pb.start();
        } catch (Exception e) {
            throw new ShellException(e.getMessage());
        }
    }
}
