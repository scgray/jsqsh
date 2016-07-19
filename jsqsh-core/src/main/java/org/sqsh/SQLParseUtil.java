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


/**
 * Simple static methods for doing general SQL parsing
 */
public class SQLParseUtil {
    
    /**
     * Searches through a string to look for parameter markers ("?"). Care is
     * taken to ignore parameter marker characters embedded in string literals
     * or quoted identifiers as well as comments.
     * 
     * @param str The string to parse
     * @param len The length of the string to parse
     * @param idx The index at which searching begins
     * @return The index of the parameter marker or len, if no parameter
     *   markers have been found.
     */
    public static int skipToParameterMarker(CharSequence str, int len, int idx) {
        
        boolean done = false;
        while (idx < len && ! done) {
            
            char ch = str.charAt(idx);
            if (ch == '/') {
                
                idx = skipComment(str, len, idx);
            }
            else if (ch == '-') {
                
                idx = skipDashComment(str, len, idx);
            }
            else if (ch == '\'' || ch == '"') {
                
                idx = skipQuotedString(str, len, idx);
            }
            else if (ch == '?') {
                
                done = true;
            }
            else {
                
                ++idx;
            }
        }
        
        return idx;
    }
    
    /**
     * Skips white space, comments are considered white space as well
     * 
     * @param str The string being parsed
     * @param len The length of the string being parsed
     * @param idx The index at which to start skipping
     * 
     * @return The index of the next non-whitespace character. If idx was
     *   sitting on a non-whitespace character, then it is not incremented
     */
    public static int skipWhitespace(CharSequence str, int len, int idx) {
        
        boolean done = false;
        while (idx < len && ! done) {
            
            char ch = str.charAt(idx);
            if (Character.isWhitespace(ch)) {
                
                ++idx;
            }
            else if (ch == '/') {
                
                int nextIdx = skipComment(str, len, idx);
                if (nextIdx == idx) {
                    
                    done = true;
                }
                idx = nextIdx;
            }
            else if (ch == '-') {
                
                int nextIdx = skipDashComment(str, len, idx);
                if (nextIdx == idx) {
                    
                    done = true;
                }
                idx = nextIdx;
            }
            else {
                
                done = true;
            }
        }
        
        return idx;
    }

    /**
     * Skips over a double dash comment.
     * 
     * @param str The string being parsed
     * @param len The length of the string being parsed
     * @param idx The index at which the first dash is located in the string
     * 
     * @return The index immediately after the newline terminating the comment
     */
    public static int skipDashComment(CharSequence str, int len, int idx) {
        
        ++idx;
        if (idx < len && str.charAt(idx) == '-') {
            
            ++idx;
            while (idx < len && str.charAt(idx) != '\n') {
                
                ++idx;
            }
        }
        
        return idx;
    }
    
    /**
     * Skips over a C style comment.
     * 
     * @param str The string being parsed
     * @param len The length of the string being parsed
     * @param idx The index at which a '/' occurs which might be a C style comment
     * 
     * @return If the leading '/' was not followed by a '*', then the slash is
     *   skipped and the next index returned, otherwise, the string is parsed 
     *   until a trailing '*' followed by '/' is encountered and the index
     *   immediately following it is returned.
     */
    public static int skipComment(CharSequence str, int len, int idx) {
        
        ++idx;
        if (idx < len && str.charAt(idx) == '*') {
            
            ++idx;
            boolean done = false;
            while (!done && idx < len) {
                
                if (str.charAt(idx) == '*'
                    && (idx+1) < len && str.charAt(idx + 1) == '/') {
                    
                    ++idx;
                    done = true;
                }
                else {
                
                    ++idx;
                }
            }
        }
        
        return idx;
    }
    
    /**
     * Skips over a quoted string.
     * 
     * @param str The string being parsed
     * @param len The length of the string being parsed
     * @param idx The index at which the starting quote is located
     * 
     * @return The index immediately following the closing quote, or the end of
     *   the string of the closing quote was not found
     */
    public static int skipQuotedString(CharSequence str, int len, int idx) {
        
        boolean done = false;
        
        char quote = str.charAt(idx++);
        while (!done && idx < len) {
            
            char ch = str.charAt(idx);
            if (ch == quote) {
                
                ++idx;
                
                /*
                 * If we hit a doubled quote (e.g. '' or "") then it
                 * is escaped and we aren't done yet.
                 */
                if (idx < len && str.charAt(idx) == quote) {
                    
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
        
        return idx;
    }
    
    /**
     * Skips contents between [ brackets ]. This does not support or allow for
     * nested brackets.
     * 
     * @param str The string being parsed
     * @param len The length of the string being parsed
     * @param idx The index at which the starting bracket ([) is located
     * 
     * @return The index immediately following the closing bracket (]), or the end of
     *   the string of the closing quote was not found
     */
    public static int skipBrackets(CharSequence str, int len, int idx) {
        
        ++idx;
        while (idx < len && str.charAt(idx) != ']') {
            
            ++idx;
        }
        
        if (idx < len) {
            
            ++idx;
        }
        
        return idx;
    }
    
    /**
     * Skips a T-SQL style @variable.
     * 
     * @param str The string being parsed
     * @param len The length of the string being parsed
     * @param idx The index at which a '@' occurs
     * 
     * @return The index immediately following the end of the variable.
     */
    public static int skipVariable(CharSequence str, int len, int idx) {
        
        boolean done = false;
        
        ++idx;
        while (!done && idx < len) {
            
            char ch = str.charAt(idx);
            if (Character.isLetter(ch)
                    || Character.isDigit(ch)
                    || ch == '_') {
                
                ++idx;
            }
            else {
                
                done = true;
            }
        }
        
        return idx;
    }
}
