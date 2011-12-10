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
package org.sqsh.jaql;

import java.io.IOException;

import org.sqsh.ConnectionContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.input.completion.Completer;

import com.ibm.jaql.io.converter.JsonToStream;
import com.ibm.jaql.json.type.JsonArray;
import com.ibm.jaql.json.type.JsonRecord;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.lang.JaqlQuery;

/**
 * Used to allow a Jaql instance to be used like a JDBC connection (well,
 * sort of).
 */
public class JaqlConnection
    extends ConnectionContext {
    
    private JaqlQuery engine;
    private JaqlFormatter formatter;
    
    public JaqlConnection (JaqlQuery engine, JaqlFormatter formatter) {
 
        this.engine      = engine;
        this.formatter   = formatter;
    }
    
    /**
     * @return The Jaql execution engine.
     */
    public JaqlQuery getEngine() {
        
        return engine;
    }
    

    /**
     * Performs very very rudamentary parsing of Jaql code looking to see
     * that a terminator is at the end of the current line and isn't contained
     * in a quoted string or comment.
     * 
     * @param batch The current batch string.
     * @param terminator The terminator character
     */
    @Override
    public boolean isTerminated(String batch, char terminator) {
        
        int len = batch.length();
        int idx = 0;
        
        while (idx < len) {
            
            char ch = batch.charAt(idx);
            if (ch == '"' || ch == '\'') {
                
                idx = skipString(batch, len, idx+1, ch);
            }
            else if (ch == '/') {
                
                ++idx;
                if (idx < len) {
                    
                    if (batch.charAt(idx) == '/') {
                    
                        for (++idx; idx < len 
                            && batch.charAt(idx) != '\n'; ++idx) {
                            /* EMPTY */
                        }
                            
                        ++idx;
                    }
                    else if (batch.charAt(idx) == '*') {
                        
                        ++idx;
                        idx = skipComment(batch, len, idx);
                    }
                }
            }
            else if (ch == terminator) {
                
                for (++idx; idx < len 
                    && Character.isWhitespace(batch.charAt(idx)); ++idx);
                
                if (idx >= len)
                    return true;
            }
            else {
                
                ++idx;
            }
        }
        
        return false;
    }
    
    
    /**
     * Returns true if the terminator is not the Jaql terminator.
     */
    @Override
    public boolean isTerminatorRemoved(char terminator) {

        return terminator != ';';
    }
    
    /**
     * Returns a Jaql tab completer. This completer attempts to complete
     * the current word from the set of Jaql global variables.
     */
    @Override
    public Completer getTabCompleter(Session session, String line,
                    int position, String word) {

        return new JaqlCompleter(engine, session, line, position, word);
    }

    /**
     * Given a quoted string, skip it.
     * @param s The string being parsed
     * @param len The length of the string being parsed
     * @param idx The current location. This should be just after the 
     *   initial quote
     * @param quoteType Type of quote
     * @return The location immediately after the closing quote
     */
    private int skipString(String s, int len, int idx, char quoteType) {
        
        while (idx < len) {
            
            char ch = s.charAt(idx);
            if (ch == quoteType) {
                
                ++idx;
                break;
            }
            else if (ch == '\\') {
                
                idx += 2;
            }
            else {
                
                ++idx;
            }
        }
        
        return idx;
    }
    
    /**
     * Skip to the end of a C-style comment 
     * @param s The string being parsed.
     * @param len The length of the string.
     * @param idx The current index into the string, just after the
     *   opening comment location
     * @return The end of the comment (after the close)
     */
    private int skipComment(String s, int len, int idx) {
        
        while (idx < len) {
            
            char ch = s.charAt(idx);
            if (ch == '*') {
                
                ++idx;
                ch = s.charAt(idx);
                if (ch == '/') {
                    
                    ++idx;
                    break;
                }
            }
            else {
                
                ++idx;
            }
        }
        
        return idx;
    }
    
    @Override
    public void eval(String batch, Session session, SQLRenderer renderer)
        throws Exception {
        
        engine.setQueryString(batch);
        
        while (engine.moveNextQuery()) {
            
            formatter.write(engine.currentQuery());
        }
    }

    @Override
    public String toString() {

        return "Jaql";
    }

    @Override
    public void close() {

        try {
            
            engine.close();
            engine = null;
        }
        catch (IOException e)
        {
            /* IGNORED */
        }
    }
}
