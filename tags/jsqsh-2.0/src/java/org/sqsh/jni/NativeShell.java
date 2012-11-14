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
