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

import java.util.Iterator;

/**
 * A LineIterator is responsible for iterating over a block of text, line by line. The iterator is expected to perform a
 * couple of basic functions including:
 *
 * <ul>
 *   <li> Removing un-displayable strings.
 *   <li> Expanding tabs to spaces.
 * </ul>
 */
public abstract class LineIterator implements Iterator<String> {

    protected int tabExpansion = 8;
    protected String str;
    protected int strLength;

    public LineIterator(String str) {
        this.str = str;
        this.strLength = str.length();

        while (strLength > 0 && Character.isWhitespace(str.charAt(strLength - 1))) {
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
        this.strLength = str.length();
        while (strLength > 0 && Character.isWhitespace(str.charAt(strLength - 1))) {
            --strLength;
        }
    }

    /**
     * @return The number of spaces that will be used when rendering a tab.
     */
    public int getTabExpansion() {
        return tabExpansion;
    }

    /**
     * @param tabExpansion Specifies the number of spaces that will be utilized when displaying tabs.
     */
    public void setTabExpansion(int tabExpansion) {
        this.tabExpansion = tabExpansion;
    }

    /**
     * This is a helper method to be used by implementing iterators to determine how many characters will be visually
     * taken up by a specified character.
     *
     * @param ch The character to test.
     * @return The number of characters required to visually display the specified character.
     */
    protected final int displayLength(char ch) {
        if (ch == '\t') {
            return tabExpansion;
        }

        if (Character.isISOControl(ch)) {
            return 0;
        }
        return 1;
    }
}
