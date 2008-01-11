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
package org.sqsh.completion;

import org.sqsh.Session;

/**
 * A completer is capable of coming up with a set of words that server
 * has suitable completions for the word that the user is currently 
 * sitting on.
 */
public abstract class Completer {
    
    protected Session session;
    
    /**
     * The entire line of input that the user was working on
     */
    protected String line;
    
    /**
     * The position in the line of input at which the user hit the
     * completion key.
     */
    protected int position;
    
    /**
     * The "word" that the cursor was sitting on when the tab was completed.
     */
    protected String word;
    
    public Completer (Session session,
            String line, int position, String word) {
        
        this.session = session;
        this.line = line;
        this.position = position;
        this.word = word;
    }
    
    /**
     * Returns the line of input the user was entering when they hit the
     * completion key.
     * 
     * @return The line of input the user was entering when they hit the
     *   completion key.
     */
    public String getLine() {
        
        return line;
    }
    
    /**
     * Returns the position in the input line at which the user hit the
     * completion key.
     * 
     * @return The position.
     */
    public int getPosition() {
        
        return position;
    }
    
    /**
     * Returns the word that the user was sitting on when they hit the
     * tab key.
     * 
     * @return the word that the user was sitting on when they hit the
     */
    public String getWord() {
        
        return word;
    }
    
    /**
     * Called to return the next word that can be used to complete the
     * users current input.
     * 
     * @return The next available completion word or null if no more
     *   words are to be found.
     */
    public abstract String next();
}
