/*
 * Copyright 2007-2012 Scott C. Gray
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
package org.sqsh.jni;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is similar to the {@link java.lang.Process} class and represents
 * a handle to a process that was just spawned.
 */
public interface Shell {
    
    /**
     * Returns a handle to the called processes input stream (that is,
     * anything written to this stream will be send to the input
     * of the process).
     * 
     * @return The input stream of the process or null if the process
     *   was opened in a manner that does not provide access to its input
     *   stream.
     */
    OutputStream getStdin();
    
    /**
     * Returns a handle to the output stream of the shell that was created.
     * 
     * @return The output stream of the shell that was created. Null is
     *   returned if the shell was created in a fashion in which the
     *   stdout is not available to the shell.
     */
    InputStream getStdout();
    
    /**
     * Returns a handle to the error stream of the shell that was created.
     * 
     * @return The error stream of the shell that was created. Null is
     *   returned if the shell was created in a fashion in which the
     *   stderr is not available to the shell.
     */
    InputStream getStderr();
    
    /**
     * causes the current thread to wait, if necessary, until the process 
     * represented by this Shell object has terminated. This method 
     * returns immediately if the subprocess has already terminated. 
     * If the subprocess has not yet terminated, the calling thread will 
     * be blocked until the subprocess exits.
     * 
     * @return The exit value of the shell.
     */
    int waitFor()
        throws InterruptedException;
}
