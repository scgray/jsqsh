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
