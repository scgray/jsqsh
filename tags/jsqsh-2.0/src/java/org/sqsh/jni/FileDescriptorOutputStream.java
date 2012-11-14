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
