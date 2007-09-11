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
