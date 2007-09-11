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

import java.util.Iterator;

/**
 * A LineIterator is responsible for iterating over a block of text, 
 * line by line. The iterator is expected to perform a couple of 
 * basic functions including:
 * 
 * <ul>
 *   <li> Removing un-displayable strings.
 *   <li> Expanding tabs to spaces.
 * </ul>
 */
public abstract class LineIterator
    implements Iterator<String> {
    
    protected int tabExpansion = 8;
    protected String str;
    protected int strLength;
    
    public LineIterator (String str) {
        
        this.str = str;
        this.strLength = str.length();;
        
        while (strLength > 0 
                && Character.isWhitespace(str.charAt(strLength - 1))) {
            
            --strLength;
        }
    }
    
    /**
     * Resets the line iterator to iterate over a new string.
     * 
     * @param str The new string to iterate over.
     */
    public void reset(String str) {
        
        this.str = str;
        this.strLength = str.length();;
        while (strLength > 0 
                && Character.isWhitespace(str.charAt(strLength - 1))) {
            
            --strLength;
        }
    }

    /**
     * @return The number of spaces that will be used when rendering a tab.
     */
    public int getTabExpansion () {
    
        return tabExpansion;
    }
    
    /**
     * @param Specifies the number of spaces that will be utilized when
     *   displaying tabs.
     */
    public void setTabExpansion (int tabExpansion) {
    
        this.tabExpansion = tabExpansion;
    }
    
    /**
     * This is a helper method to be used by implementing iterators
     * to determine how many characters will be visually taken up
     * by a specified character.
     * 
     * @param ch The character to test.
     * @return The number of characters required to visually display
     *    the specified character.
     */
    protected final int displayLength(char ch) {
        
        if (ch == '\t')  {
            
            return tabExpansion;
        }
        if (Character.isISOControl(ch)) {
            
            return 0;
        }
        
        return 1;
    }
}
