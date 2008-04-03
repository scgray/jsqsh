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
