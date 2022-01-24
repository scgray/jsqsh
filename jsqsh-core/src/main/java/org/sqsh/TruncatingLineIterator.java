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

import java.util.NoSuchElementException;

/**
 * This iterator is used to iterate over column text that is about to be
 * displayed to the user. It will choose to truncate long lines (greater
 * than a provided width) rather than word wrapping. If you want to word
 * wrap then use the WordWrapLineIterator.
 */
public class TruncatingLineIterator
    extends LineIterator {
    
    private int idx;
    private int width;
    
    private String segment = null;
    
    /**
     * Creates a line iterator to iterate over the lines contained in 
     * the provided string, truncating each line to width characters.
     * 
     * @param str The string to be iterated.
     * @param width The maximum number of characters to allow in the
     *    line (excess will be truncated), if a 0 or a negative value
     *    is supplied then truncation will not take place.
     */
    public TruncatingLineIterator (String str, int width) {
        
        super(str);
        this.idx = 0;
        this.width = width;
    }
    
    public void reset(String str) {
        
        super.reset(str);
        this.idx = 0;
        this.segment = null;
    }
    
    public boolean hasNext () {
        
        if (segment == null) {
            
            segment = getSegment();
        }
        
        return segment != null;
    }

    public String next () {
        
        String str = segment;
        if (str != null) {
            
            segment = null;
            return str;
        }
        
        str = getSegment();
        if (str == null) {
            
            throw new NoSuchElementException();
        }
        
        return str;
    }

    public void remove () {

    }
    
    private String getSegment() {
        
        if (idx == strLength) {
            
            return null;
        }
        
        /*
         * If we were sitting on a newline from the last line, then 
         * skip it.
         */
        if (idx < strLength
                && str.charAt(idx) == '\n') {
            
            ++idx;
        }
        
        StringBuilder sb = new StringBuilder();
        char ch = str.charAt(idx);
        boolean done = false;
        while (!done
                && idx < strLength
                && (width <= 0 || sb.length() <  width)
                && ch != '\n') {
            
            if (ch == '\t') {
                
                if (width < 0 || (sb.length() + tabExpansion) <= width) {
                    
                    for (int i = 0; i < tabExpansion; i++) {
                        
                        sb.append(' ');
                    }
                    
                    ++idx;
                }
                else {
                    
                    done = true;
                }
            }
            else if (displayLength(ch) == 0) {
                
                ++idx;
            }
            else {
                
                sb.append(ch);
                ++idx;
            }
            
            if (idx < strLength) {
                
                ch = str.charAt(idx);
            }
        }
        
        /*
         * If we reached our maximum width, then we need to truncate up to
         * the next new-line.
         */
        while (width > 0 && ch != '\n' && idx < strLength) {
            
            ch = str.charAt(idx);
            ++idx;
        }
        
        return sb.toString();
    }
}
