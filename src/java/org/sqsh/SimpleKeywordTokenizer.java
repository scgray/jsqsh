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
package org.sqsh;

import java.util.Stack;

/**
 * The SimpleKeywordTokenizer attempts to iterate through potential SQL keywords,
 * skipping over quoted strings, punctuation (except for semicolon), comments,
 * and variable names so that hopefully only keywords, or objects names that
 * couldn't be confused with keywords are returned. 
 */
public class SimpleKeywordTokenizer {
    
    private String sql;
    private int len;
    private int idx;
    private char terminator;
    
    private Stack<String> tokens = new Stack<String>();
    
    public SimpleKeywordTokenizer (String sql, char terminator) {
        
        this.sql = sql;
        this.len = sql.length();
        this.idx = 0;
        this.terminator = terminator;
    }
    
    /**
     * Returns the next token. 
     * 
     * @return The next availabe token or null if there is nothing else to
     *   process. All tokens are returned in upper case.
     */
    public String next() {
        
        if (tokens.isEmpty() == false) {
            
            return tokens.pop();
        }
        
        doSkip();
        
        if (idx >= len) {
            
            return null;
        }
        
        if (sql.charAt(idx) == terminator) {
            
            ++idx;
            return "" + terminator;
        }
        
        StringBuilder sb = new StringBuilder();
        boolean done = false;
        
        while (!done && idx < len) {
            
            char ch = sql.charAt(idx);
            if (Character.isLetter(ch)
                    || Character.isDigit(ch)
                    || ch == '_') {
                
                sb.append(Character.toUpperCase(ch));
                ++idx;
            }
            else {
                
                done = true;
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Allows the user to un-read a previously read token.
     * 
     * @param token The token to unget.
     */
    public void unget(String token) {
        
        tokens.push(token);
    }
    
    /**
     * Skips sequences of characters that we just don't care about. In
     * reality we only care about key words  and the semicolon character,
     * but we also need to watch for comments.
     */
    private void doSkip() {
        
        boolean done = false;
        
        while (!done && idx < len) {
            
            char ch = sql.charAt(idx);
            
            if (Character.isLetter(ch) || ch == terminator) {
                
                done = true;
            }
            else {
                
                if (ch == '-') {
                    
                    doSkipDashComment();
                }
                if (ch == '/') {
                    
                    doSkipComment();
                }
                if (ch == '\'' || ch == '"') {
                    
                    doSkipQuotes(ch);
                }
                else if (ch == '[') {
                    
                    doSkipBrackets();
                }
                else if (ch == '@') {
                    
                    doSkipVariable();
                }
                else {
                
                    ++idx;
                }
            }
        }
    }
    
    /**
     * Called by doSkip() when a dash (-) is encountered. This method checks
     * to see if the next character is also a dash and, thus, is a single line
     * SQL comment, e.g. -- this is a comment. If so, it skips to the end of 
     * the line.
     */
    private void doSkipDashComment() {
        
        ++idx;
        if (idx < len && sql.charAt(idx) == '-') {
            
            ++idx;
            while (idx < len && sql.charAt(idx) != '\n') {
                
                ++idx;
            }
        }
    }
    
    /**
     * Called by doSkip() when a slash (/) is encountered. This method checks
     * to see if it is part of a C-style comment. If so, it continues eating
     * characters until the end of the comment is reached.
     */
    private void doSkipComment() {
        
        ++idx;
        if (idx < len && sql.charAt(idx) == '*') {
            
            ++idx;
            boolean done = false;
            while (!done && idx < len) {
                
                if (sql.charAt(idx) == '*'
                    && (idx+1) < len && sql.charAt(idx + 1) == '/') {
                    
                    ++idx;
                    done = true;
                }
                
                ++idx;
            }
        }
    }
    
    /**
     * Called by doSkip() when a single or double quote is encountered and
     * consumes everything contained in the quotes.
     * 
     * @param quote The quote type that was hit (a ' or ").
     */
    private void doSkipQuotes(char quote) {
        
        boolean done = false;
        
        ++idx;
        while (!done && idx < len) {
            
            char ch = sql.charAt(idx);
            if (ch == quote) {
                
                ++idx;
                
                /*
                 * If we hit a doubled quote (e.g. '' or "") then it
                 * is escaped and we aren't done yet.
                 */
                if (idx < len && sql.charAt(idx) == quote) {
                    
                    ++idx;
                }
                else {
                    
                    done = true;
                }
            }
            else {
                
                ++idx;
            }
        }
    }
    
    /**
     * Called by doSkip() when we hit a bracketed object name (e.g.
     * [MY TABLE].[MY COLUMN], then we skip it over.
     */
    private void doSkipBrackets() {
        
        ++idx;
        while (idx < len && sql.charAt(idx) != ']') {
            
            ++idx;
        }
        
        if (idx < len) {
            
            ++idx;
        }
    }
    
    /**
     * If we hit a variable name (starting with a @), then skip it.
     */
    private void doSkipVariable() {
        
        boolean done = false;
        
        ++idx;
        while (!done && idx < len) {
            
            char ch = sql.charAt(idx);
            if (Character.isLetter(ch)
                    || Character.isDigit(ch)
                    || ch == '_') {
                
                ++idx;
            }
            else {
                
                done = true;
            }
        }
    }
    
}
