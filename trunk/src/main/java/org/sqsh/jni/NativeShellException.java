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

/**
 * This method is thrown by most methods in the JNI layer to indicate
 * that a call to a POSIX function has failed. 
 */
public class NativeShellException
    extends ShellException {
    
    private int errno;
    
    /**
     * Creates a posix exception. 
     * @param msg The text of the error message.
     * @param errno The posix errno.
     */
    public NativeShellException (String msg, int errno) {
        
        super(msg);
        this.errno = errno;
    }
    
    /**
     * Returns the posix error number related to the exception.
     * 
     * @return The posix error number of the exception.
     */
    public int getErrno() {
        
        return errno;
    }
}
