/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
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
