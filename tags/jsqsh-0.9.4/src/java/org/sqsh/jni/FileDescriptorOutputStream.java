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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Used to write to a file represented by a UNIX file descriptor
 */
public class FileDescriptorOutputStream
    extends OutputStream {
    
    /**
     * HACK!! We are using a long here because on some platforms (windows)
     * the JNI layer is actually stuffing the address of the file handle
     * into this location rather than a handy ol' file descriptor.
     */
    private long fd;
    
    /**
     * Creates the output stream.
     * @param fd The file descriptor to use for writes.
     */
    public FileDescriptorOutputStream (long fd) {
        
        this.fd = fd;
    }
    
    /**
     * Writes a byte to the output stream.
     * 
     * @param b the byte to write.
     */
    public void write (int b)
        throws IOException {
        
        ShellManager.writeByte (fd, b);
    }
    
    /**
     * Writes bytes to the output stream
     * 
     * @param bytes the bytes to write
     * @param offset the offset to start at
     * @param len the number of bytes to write.
     */
    public void write (byte []bytes, int offset, int len)
        throws IOException {
        
        ShellManager.writeBytes (fd, bytes, offset, len);
    }
    
    public void close()
        throws IOException {
        
        ShellManager.close(fd);
    }
}
