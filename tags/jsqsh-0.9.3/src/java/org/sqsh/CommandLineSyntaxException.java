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
 * Reprsents an exception encountered while parsing a command line
 */
public class CommandLineSyntaxException
    extends Exception {
    
    /*
     * The number of characters into the command line that the problem
     * was encountered.
     */
    private int position;
    
    /*
     * The line that was being parsed.
     */
    private String line;

    /**
     * Creates a new exception.
     * 
     * @param msg The syntax error that was encountered.
     * @param position The position in the string being parsed that it encountered.
     * @param line The string that was being parsed.
     */
    public CommandLineSyntaxException(String msg, int position, String line) {

        super(msg);
        this.position = position;
        this.line = line;
    }

    
    /**
     * Returns the command line that was being parsed.
     * @return the line that was being parsed.
     */
    public String getLine () {
    
        return line;
    }

    
    /**
     * Returns the position into the line that was being parsed
     * 
     * @return the position The position (character index) into th eline
     *   that was being parsed.
     */
    public int getPosition () {
    
        return position;
    }
}
