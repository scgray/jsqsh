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
package org.sqsh.parser;

import java.util.Stack;

/**
 * The SimpleSQLTokenizer is used to iterate through keywords and syntax
 * items contained in common SQL. It is NOT a smart tokenizer, but 
 * can be useful to look for snippets of SQL that may be useful
 * for keyword completion or that sort of thing.
 */
public class SimpleSQLTokenizer {
    
    private String sql;
    private int len;
    private int idx;
    
    /**
     * How far nested in parentheses are we?
     */
    private int parenCount = 0;
    
    private Stack<String> tokens = new Stack<String>();
    
    public SimpleSQLTokenizer (String sql) {
        
        this.sql = sql;
        this.len = sql.length();
        this.idx = 0;
    }
    
    /**
     * Returns the next token. 
     * 
     * @return The next available token or null if there is nothing else to
     *   process. All tokens are returned in upper case.
     */
    public String next() {
        
        if (tokens.isEmpty() == false) {
            
            String s = tokens.pop();
            if (s.equals("(")) {
                
                ++parenCount;
            }
            else if (s.equals(")")) {
                
                --parenCount;
            }
            
            return s;
        }
        
        /*
         * Skip until the beginning of the next item to be returned
         * to the caller.
         */
        doSkip();
        
        /*
         * If we fell off the end, then we are done.
         */
        if (idx >= len) {
            
            return null;
        }
        
        /*
         * By peeking at the next character we can figure out what
         * sort of "word" we are dealing with.
         */
        char ch = sql.charAt(idx);
        if (ch == '@' && isNameBeginChar(nextChar())) {
            
            return doVariable();
        }
        else if (ch == '\'' || ch == '\"') {
            
            return doQuotedString();
        }
        else if (ch == '[') {
            
            return doBracketedString();
        }
        else if (Character.isDigit(ch)
                || ch == '.' && Character.isDigit(nextChar())) {
            
            return doNumber();
        }
        else if (isNameBeginChar(ch)) {
            
            return doWord();
        }
        else {
            
            if (ch == '(') {
                
                ++parenCount;
            }
            else if (ch == ')') {
                
                --parenCount;
            }
            
            StringBuilder sb = new StringBuilder(1);
            sb.append(ch);
            ++idx;
            return sb.toString();
        }
    }
    
    /**
     * Allows the user to un-read a previously read token.
     * 
     * @param token The token to unget.
     */
    public void unget(String token) {
        
        /*
         * By "ungetting" an open paren, then means we have to undo
         * our nesting depth.
         */
        if (token.equals("(")) {
            
            if (parenCount > 0) {
                
                --parenCount;
            }
        }
        else if (token.equals(")")) {
            
            /*
             * Same for the closing paren.
             */
            ++parenCount;
        }
        
        tokens.push(token);
    }
    
    /**
     * Returns how far nested inside of parentheses the parser is.
     * 
     * @return The parentheses nesting level.
     */
    public int getParenCount() {
        
        return parenCount;
    }
    
    /**
     * Called to process a @variable name.
     */
    private String doVariable() {
        
        StringBuilder sb = new StringBuilder();
        sb.append(sql.charAt(idx));
        
        ++idx;
        while (idx < len && isNameChar(sql.charAt(idx))) {
            
            sb.append(sql.charAt(idx));
            ++idx;
        }
        
        return sb.toString();
    }
    
    /**
     * Used to process a "word".
     * 
     * @return The word.
     */
    private String doWord() {
        
        StringBuilder sb = new StringBuilder();
        sb.append(sql.charAt(idx));
        
        ++idx;
        while (idx < len && isNameChar(sql.charAt(idx))) {
            
            sb.append(sql.charAt(idx));
            ++idx;
        }
        
        return sb.toString();
    }
    
    /**
     * Used to process a number. This logic is very weak and will
     * pick up 1.2.3.4.5.6 as a single number.
     * 
     * @return The number
     */
    private String doNumber() {
        
        StringBuilder sb = new StringBuilder();
        sb.append(sql.charAt(idx));
        
        ++idx;
        if (idx < len) {
            
            char ch = sql.charAt(idx);
            while (idx < len && (Character.isDigit(ch) || ch == '.')) {
                
                sb.append(ch);
                ++idx;
                if (idx < len) {
                    
                    ch = sql.charAt(idx);
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Called to process a "quoted" 'string'. The quotes are discarded.
     */
    private String doQuotedString() {
        
        StringBuilder sb = new StringBuilder();
        
        boolean done = false;
        char quote = sql.charAt(idx);
        ++idx;
        
        while (!done && idx < len) {
            
            char ch = sql.charAt(idx);
            if (ch == quote) {
                
                /*
                 * If we hit a doubled quote (e.g. '' or "") then it
                 * is escaped and we aren't done yet.
                 */
                if (nextChar() == quote) {
                    
                    sb.append(quote);
                    idx += 2;
                }
                else {
                    
                    ++idx;
                    done = true;
                }
            }
            else {
                
                sb.append(ch);
                ++idx;
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Skips sequences of characters that we just don't care about. This
     * would be white-space and comments.
     */
    private void doSkip() {
        
        boolean done = false;
        while (!done && idx < len) {
            
            char ch = sql.charAt(idx);
            if (Character.isWhitespace(ch)) {
                
                ++idx;
            }
            else if (ch == '-' && nextChar() == '-') {
                    
                doSkipDashComment();
            }
            else if (ch == '/' && nextChar() == '*') {
                    
                doSkipComment();
            }
            else {
                
                done = true;
            }
        }
    }
    
    /**
     * Called by doSkip() when a double dash (--) is encountered, and
     * the current position is moved forward until the comment is completed.
     */
    private void doSkipDashComment() {
        
        idx += 2;
        while (idx < len && sql.charAt(idx) != '\n') {
                
            ++idx;
        }
    }
    
    /**
     * Called by doSkip() when a slash (/*) is encountered and skips ahead until
     * the comment is completed.
     */
    private void doSkipComment () {

        idx += 2;
        boolean done = false;
        while (!done && idx < len) {

            if (sql.charAt(idx) == '*' && (idx + 1) < len
                && sql.charAt(idx + 1) == '/') {

                ++idx;
                done = true;
            }

            ++idx;
        }
    }
    
    
    /**
     * Called to process object names that are [in brackets].
     */
    private String doBracketedString() {
        
        StringBuilder sb = new StringBuilder();
        
        ++idx;
        while (idx < len && sql.charAt(idx) != ']') {
            
            sb.append(sql.charAt(idx));
            ++idx;
        }
        
        if (idx < len) {
            
            ++idx;
        }
        
        return sb.toString();
    }
    
    
    /**
     * Used to test a character to determine if the character could be
     * the start of an object name. An object name may start with
     * [a-zA-Z_].
     * 
     * @return True if the character could be starting a name.
     */
    public boolean isNameBeginChar(char ch) {
        
        return (Character.isLetter(ch) || ch == '_');
    }
    
    /**
     * Used to test a character to determine if the character is part 
     * of the body of an object name. An object name may start with
     * [a-zA-Z0-9$_]. The dollar sign ($) is for Oracle V$TABLES.
     * 
     * @return True if the character could be part of a name.
     */
    public boolean isNameChar(char ch) {
        
        return (Character.isLetter(ch) 
                || Character.isDigit(ch)
                || ch == '$'
                || ch == '_');
    }
    
    /**
     * Helper method to safely peek forward at the next character 
     * in the string.
     * 
     * @return The next character in the string or character(0) if
     *   there are no more characters available.
     */
    private char nextChar() {
        
        if (idx >= (len-1)) {
            
            return (char) 0;
        }
        
        return sql.charAt(idx + 1);
    }
}
