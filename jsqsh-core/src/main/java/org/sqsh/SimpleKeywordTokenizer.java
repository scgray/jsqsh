/*
 * Copyright 2007-2022 Scott C. Gray
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
    
    private CharSequence sql;
    private int len;
    private int idx;
    private char terminator;
    private boolean toUpperCase = true;
    
    private Stack<String> tokens = new Stack<String>();
    
    public SimpleKeywordTokenizer (CharSequence sql, char terminator, boolean toUpperCase) {
        
        this.sql = sql;
        this.len = sql.length();
        this.idx = 0;
        this.terminator = terminator;
        this.toUpperCase = toUpperCase;
    }
    
    public SimpleKeywordTokenizer (CharSequence sql, char terminator) {
        
        this(sql, terminator, true);
    }
    
    /**
     * Returns the next token. 
     * 
     * @return The next available token or null if there is nothing else to
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
            return Character.toString(terminator);
        }
        
        StringBuilder sb = new StringBuilder();
        boolean done = false;
        
        while (!done && idx < len) {
            
            char ch = sql.charAt(idx);
            if (Character.isLetter(ch)
                    || Character.isDigit(ch)
                    || ch == '_') {
                
                sb.append(toUpperCase ? Character.toUpperCase(ch) : ch);
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
        
        if (token != null) {
            
            tokens.push(token);
        }
    }
    
    /**
     * Peek ahead one token
     * @return The next token or null if there is no next token
     */
    public String peek() {
        
        String t = next();
        if (t != null) {
            
            tokens.push(t);
        }
        
        return t;
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
                    
                    idx = SQLParseUtil.skipDashComment(sql, len, idx);
                }
                if (ch == '/') {
                    
                    idx = SQLParseUtil.skipComment(sql, len, idx);
                }
                if (ch == '\'' || ch == '"') {
                    
                    idx = SQLParseUtil.skipQuotedString(sql, len, idx);
                }
                else if (ch == '[') {
                    
                    idx = SQLParseUtil.skipBrackets(sql, len, idx);
                }
                else if (ch == '@') {
                    
                    idx = SQLParseUtil.skipVariable(sql, len, idx);
                }
                else {
                
                    ++idx;
                }
            }
        }
    }
}
