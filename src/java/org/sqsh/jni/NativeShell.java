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
 * A handle to a UNIX process representing a subshell.
 */
public class NativeShell
    implements Shell {
    
    private OutputStream stdin = null; 
    private InputStream stdout = null; 
    private InputStream stderr = null; 
    
    /**
     * SUPER HACK ALERT!! Because our "pid" may actually be a "handle"
     * on windows (or, presumably some other platform), we will reserve
     * an entire long to hold its value. In the JNI layer we will stuff
     * a pointer into this value rather than the actual pid. This is
     * gross, I know.
     */
    private long pid;
    
    /**
     * Creates a handle to a UNIX process that has been spawned in a such
     * a fashion as only the stdin is available (e.g. popen()).
     * 
     * @param pid The process id of the shell.
     * @param stdin The stdin of the shell.
     */
    public NativeShell (long pid, long stdin) {
        
        this.pid = pid;
        this.stdin = new FileDescriptorOutputStream(stdin);
    }
    
    /**
     * Creates a handle to a UNIX process that has been spawned in such 
     * a fashion as no stream handles area available (e.g. exec()).
     * 
     * @param pid The process id of the shell.
     */
    public NativeShell (long pid) {
        
        this.pid = pid;
    }
    
    public InputStream getStderr () {

        return stderr;
    }

    public OutputStream getStdin () {

        return stdin;
    }

    public InputStream getStdout () {

        return stdout;
    }

    public int waitFor ()
        throws InterruptedException {

        return ShellManager.waitPid(pid);
    }
}
