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
