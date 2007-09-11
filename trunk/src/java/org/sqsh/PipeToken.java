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
 * The PipeToken represents a pipline command following
 * the building sqsh command. That is, given the string:
 * 
 * <pre>
 *    hello how are you | blah blah
 * </pre>
 * 
 * The "blah blah" is the portion of the command line that
 * is considered the pipe.
 */
public class PipeToken
    extends Token {
    
    private String pipeCommand;
    
    /**
     * Creates a command line pipe token.
     * 
     * @param line The line from which the token came.
     * @param position The position on the line from which the token came.
     * @param pipeCommand The command that is to be piped to.
     */
    public PipeToken(String line, int position, String pipeCommand) {
        
        super(line, position);
        this.pipeCommand = pipeCommand;;
    }

    /**
     * Returns the command that is to be piped to.
     * @return the command that is to be piped to.
     */
    public String getPipeCommand () {
    
        return pipeCommand;
    }

    /**
     * Sets the command that is to be piped to.
     * @param pipeCommand the command that is to be piped to.
     */
    public void setPipeCommand (String pipeCommand) {
    
        this.pipeCommand = pipeCommand;
    }
    
    public String toString() {
        
        return pipeCommand;
    }
}
