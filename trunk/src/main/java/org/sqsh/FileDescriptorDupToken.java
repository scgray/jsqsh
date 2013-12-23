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
package org.sqsh;

import java.util.logging.Logger;


/**
 * Token that represents a file descriptor duplication (dup2()) operation. This are
 * of the form of N&amp;&gt;M where N is the file descriptor to be duplicated 
 * and M is the file descriptor to be closed and duplicated into.
 */
public class FileDescriptorDupToken
    extends Token {

    private static final Logger LOG = Logger
            .getLogger(FileDescriptorDupToken.class.getName());
    
    /**
     * The file descriptor that we are duplicating.
     */
    private int oldFd;
    
    /**
     * The file descriptor that we are copying into.
     */
    private int newFd;
    
    /**
     * Creates a token representing a file descriptor duplication
     * operation.
     * 
     * @param line The line from which the token came.
     * @param position The position on the line from which the token came.
     * @param oldFd The original file descriptor.
     * @param newFd The new descriptor that is being created (and closed
     *    if necessary).
     */
    public FileDescriptorDupToken(String line, int position, 
            int oldFd, int newFd) {
        
        super(line, position);
        
        this.oldFd = oldFd;
        this.newFd = newFd;
    }
    
    /**
     * @return the newFd
     */
    public int getNewFd () {
    
        return newFd;
    }

    
    /**
     * @param newFd the newFd to set
     */
    public void setNewFd (int newFd) {
    
        this.newFd = newFd;
    }

    
    /**
     * @return the oldFd
     */
    public int getOldFd () {
    
        return oldFd;
    }
    
    /**
     * @param oldFd the oldFd to set
     */
    public void setOldFd (int oldFd) {
    
        this.oldFd = oldFd;
    }
    
    /**
     * Returns the redirection operation as a string.
     */
    public String toString() {
        
        StringBuffer sb = new StringBuffer(10);
        
        sb.append(oldFd).append(">&").append(newFd);
        
        return sb.toString();
    }
}
