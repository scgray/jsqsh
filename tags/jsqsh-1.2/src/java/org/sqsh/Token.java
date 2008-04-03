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

/**
 * Represents an tokenized of a command line. A token can be a string
 * literal.
 */
abstract class Token {
    
    private int position = -1;
    private String line = null;
    
    /**
     * Noarg constructor.
     */
    public Token() {
        
    }
    
    /**
     * Create a token.
     * 
     * @param line Line from which the token came.
     * @param position Position on the line the taken began at.
     */
    public Token (String line, int position) {
        
        this.line = line;
        this.position = position;
    }
    
    /**
     * The line from which the token came.
     * @return The line from which the token came.
     */
    public String getLine () {
    
        return line;
    }
    
    /**
     * The position in the line at which the token began.
     * @return position in the line at which the token began.
     */
    public int getPosition () {
    
        return position;
    }
    
    /**
     * Every token must be able to convert itself back into a string.
     * 
     * @return The string version of the token 
     */
    public abstract String toString();
}
