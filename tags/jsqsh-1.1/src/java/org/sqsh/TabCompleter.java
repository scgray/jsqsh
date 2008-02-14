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

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;
import org.sqsh.completion.Completer;
import org.sqsh.completion.DatabaseObjectCompleter;

/**
 * Responsible for doing completion when the user hits the completion key.
 * This class only works if you are using the native readline.
 */
public class TabCompleter
    implements ReadlineCompleter {
    
    private SqshContext sqshContext;
    private Completer completer = null;
    
    public TabCompleter(SqshContext ctx) {
        
        this.sqshContext = ctx;
    }
    
    /**
     * Called when the user hits the completion key. See the javadoc 
     * for {@link org.gnu.readline.ReadlineCompleter} for how this
     * method is induced.
     * 
     * @param word The text up to the word break character.
     * @param state 0=initial search request, >1 the request for the N'th
     *    completion entry.
     */
    public String completer (String word, int state) {
        
        /*
         * A state greater than 0 indicates that the caller is in the
         * process of iterating over our current result set.
         */
        if (state > 0) {
            
            if (completer != null) {
                
                String c =  completer.next();
                if (c == null) {
                    
                    completer = null;
                }
                
                return c;
            }
            
            return null;
        }
        
        /*
         * The word that was found by our caller is not nearly enough
         * to figure out the whole object name, so we will use the
         * entire line to do our work.
         */
        String wholeLine = Readline.getLineBuffer();
        int position = wholeLine.length();
        
        /*
         * Now for the hard part. We need to try to figure out where 
         * the user is currently sitting in the line. There is a native
         * readline call to do this, but it hasn't been exposed via JNI, so
         * I'll try to make a guess m'self.
         */
        if (word != null && word.length() > 0) {
            
            /*
             * Our logic to find the location of our word is to start at the
             * end of the entire line and search backwards looking for it.
             * When we find the word we ensure that the character previous
             * to the word is one of the word separators that we declared
             * in SqshContext.java. If it is not one of those characters 
             * we keep moving backwards.
             */
            boolean done = false;
            int idx = wholeLine.lastIndexOf(word);
            while (!done && idx >= 0) {
                
                if (idx == 0
                   || " \t,/.()<>=?".indexOf(wholeLine.charAt(idx-1)) >= 0) {
                    
                    done = true;
                }
                else {
                
                    idx = wholeLine.lastIndexOf(word, idx - 1);
                }
            }
            
            if (idx >= 0) {
                
                idx += word.length();
                position = idx;
            }
        }
        
        Session session = sqshContext.getCurrentSession();
        if (session.getConnection() != null) {
            
            completer = new DatabaseObjectCompleter(session,
                wholeLine, position, word);
        }
        else {
            
            completer = null;
        }
        
        if (completer == null) {
            
            return null;
        }
        
        return completer.next();
    }
}
        