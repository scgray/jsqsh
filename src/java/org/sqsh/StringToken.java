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
 * Represents an arbitrary string that was parsed from the command line.
 */
public class StringToken
    extends Token {
    
    private static final Logger LOG =
        Logger.getLogger(RedirectOutToken.class.getName());
    
    private String str;
    
    /**
     * Creates a string token.
     * 
     * @param line The line from which the token came.
     * @param position The position on the line from which the token came.
     * @param str The string represented by the token.
     */
    public StringToken(String line, int position, String str) {
        
        super(line, position);
        this.str = str;
    }
    
    /**
     * Returns the string that was parsed.
     * @return the string that was parsed.
     */
    public String getString() {
        
        return str;
    }
    
    /**
     * Sets the string that was parsed.
     * @param str the string that was parsed.
     */
    public void setString(String str) {
        
        this.str = str;
    }
    
    public String toString() {
        
        return str;
    }
}
