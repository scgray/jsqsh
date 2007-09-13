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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.gnu.readline.ReadlineCompleter;

/**
 * Responsible for doing completion when the user hits the completion key.
 * This class only works if you are using the native readline.
 */
public class TabCompleter
    implements ReadlineCompleter {

    SqshContext sqshContext;
    Iterator<String> iter = null;
    
    public TabCompleter(SqshContext ctx) {
        
        this.sqshContext = ctx;
    }
    
    /**
     * Called when the user hits the completion key. See the javadoc 
     * for {@link org.gnu.readline.ReadlineCompleter} for how this
     * method is induced.
     * 
     * @param line The text up to the word break character.
     * @param state 0=initial search request, >1 the request for the N'th
     *    completion entry.
     */
    public String completer (String line, int state) {
        
        Session session = sqshContext.getCurrentSession();
        if (line == null || session.getConnection() == null) {
            
            iter = null;
        }
        
        /*
         * A state greater than 0 indicates that the caller is in the
         * process of iterating over our current result set.
         */
        if (state > 0) {
            
            if (iter == null || iter.hasNext() == false) {
                
                return null;
            }
            
            return iter.next();
        }
        
        /*
         * Upon setup, Readline was configured (in SqshContext) to use a
         * "\n" as the word separator character. This was done so we can
         * get a look at the whole line.
         */
        String []parts = doFindObject(line);
        for (int i = 0; i < parts.length; i++) {
            
            System.out.println(parts[i]);
        }
        return null;
    }
    
    /**
     * Processes the current line of input looking for the last object
     * name on the line (or at least what could possibly be an object
     * name). 
     * @param line The line to be processed.
     * @return The object name broken into its component parts. If there
     * was no object found an empty list will be returned.
     */
    private String[] doFindObject(String line) {
        
        int idx = 0;
        int len = line.length();
        List<String> parts = new ArrayList<String>();
        
        /*
         * Ok, here's what we are doing. When we hit a 
         * word, "quoted word", or [bracketed word] we take that
         * word and append it to our list of object name parts and
         * we continue to do so as long as there is a '.' between
         * those word. As soon as we hit something that is not a '.'
         * we clear out our current list and keep moving on.
         */
        while (idx < len) {
            
            char ch = line.charAt(idx);
            if (Character.isLetter(ch) || ch == '_') {
                
                idx = doWord(parts, line, idx);
            }
            else if (ch == '"') {
                
                idx = doQuotedWord(parts, line, idx);
            }
            else if (ch == '[') {
                
                idx = doBracketedWord(parts, line, idx);
            }
            else if (ch == '.') {
                
                ++idx;
            }
            else {
                
                if (parts.size() > 0) {
                    
                    parts.clear();
                }
                
                ++idx;
            }
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Called by doFindObject() when it has landed on the beginning of 
     * a "word" in a line.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
        word.append(line.charAt(idx));
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = false;
        while (!done) {
            
            if (idx >= len) {
                
                done = true;
            }
            else {
                
                char ch = line.charAt(idx);
                if (Character.isLetter(ch)
                            || Character.isDigit(ch)
                            || ch == '_') {
                    
                    word.append(ch);
                    ++idx;
                }
                else {
                    
                    done = true;
                }
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
    
    /**
     * Called by doFindObject() when it has landed on the beginning of 
     * a quoted word.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doQuotedWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = (idx >= len);
        while (idx < len && !done) {
                        
            char ch = line.charAt(idx);
            if (ch == '"') {
                
                ++idx;
                if (idx < (len - 1) && line.charAt(idx+1) == '"') {
                    
                    word.append('"');
                }
                else {
                    
                    done = true;
                }
            }
            else {
                
                word.append(ch);
                ++idx;
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
    
    /**
     * Called by doBracketedWord() when it has landed on the beginning of 
     * a bracketed word.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doBracketedWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = (idx >= len);
        while (idx < len && !done) {
                        
            char ch = line.charAt(idx);
            if (ch == ']') {
                
                ++idx;
                done = true;
            }
            else {
                
                ++idx;
                word.append(ch);
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
}
        