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
 * Iterator to iterate over the lines in a string, performing word wrapping
 * where necessary.  Tabs are expanded to 8 spaces, by default, but this
 * behavior may be overidden if desired.
 */
public class WordWrapLineIterator
    extends LineIterator {
    
    Position pos;
    private int width;
    
    private boolean isBeginningOfLine = true;
    
    String segment = null;
    
    /**
     * Creates a word wrapper.
     * @param str The string to be wrapped.
     * @param width The width at which it should be wrapped.
     */
    public WordWrapLineIterator (String str, int width) {
        
        super(str);
        
        pos = new Position();
        this.width = width;
    }
    
    public void reset(String str) {
        
        super.reset(str);
        pos = new Position();
        segment = null;
    }
    
    public boolean hasNext () {

        if (segment == null) {
            
            segment = nextSegment();
        }
        
        return (segment != null);
    }

    public String next() {
        
        String str = segment;
        if (str != null) {
            
            segment = null;
            return str;
        }
        
        str = nextSegment();
        if (str == null) {
            
            throw new NoSuchElementException();
        }
        
        return str;
    }

    public void remove () {

    }
    
    private String nextSegment() {
        
        String str = null;
        int ch = pos.next();
        
        if (pos.isEOL()) {
            
            return null;
        }
        
        /*
         * If we are not at the begining of the line then we can simply
         * discard all leading white-space.
         */
        if (isBeginningOfLine == false && ch != '\n') {
            
            while (ch != '\n' && ch != -1 && Character.isWhitespace(ch)) {
                
                ch = pos.next();
            }
        }
        
        /*
         * Move forward until we hit maximum width or hit a new-line
         * or end-of-line.
         */
        while (ch != '\n' && ch != -1 && pos.getLen() < width) {
            
            ch = pos.next();
        }
        
        isBeginningOfLine = (ch == '\n');
        
        /*
         * Determine if we need to back up to the previous word.
         */
        if (pos.getLen() >= width) {
            
            /*
             * Peek forward at the next character.
             */
            int nextCh = pos.peek();
            
            /*
             * If we ended exactly at our wordwrap length and the next
             * character is a new-line, then consume the new-line and don't
             * bother backing up.
             */
            if (pos.getLen() == width                 
                    && (nextCh == '\n' || nextCh == -1)) {
                
                isBeginningOfLine = (nextCh == '\n');
                str = pos.getSegment();
                pos.next();
            }
            else if (pos.getLen() == width
                    && Character.isWhitespace((nextCh))) {
                
                /*
                 * Similarly if the next character is a white-space, then
                 * consume it and don't bother backing up.
                 */
                str = pos.getSegment();
                pos.next();
            }
            else if (! Character.isWhitespace(ch)) {
                
                /*
                 * The last character we used was not a white-space, and the
                 * next character was not a white space...we are in the middle
                 * of a word!  Time to back up and leave this word for the next
                 * line then.
                 */
                Position end = (Position) pos.clone();
                ch = end.prev();
                while (ch != -1 && !Character.isWhitespace(ch)) {
                    
                    ch = end.prev();
                }
                
                /*
                 * If we backed all the way to the beginning of the string, then
                 * we have to just do a hard truncation.
                 */
                if (ch != -1) {
                    
                    /*
                     * We need to "put back" last character we just tested.
                     */
                    end.next();
                    pos = end;
                }
                else {
                    
                    while (pos.getLen() > width) {
                        
                        pos.prev();
                    }
                }
                
                str = pos.getSegment();
            }
            else {
                
                /*
                 * This block means that the last character we read was a 
                 * white space and the next character up is not a white space.
                 * That is good, nothing to be done.
                 */
                str = pos.getSegment();
            }
        }
        else {
            
            str = pos.getSegment();
        }
        
        pos.skip();
        return str;
    }
    
    private class Position
        implements Cloneable {
        
        private int startIdx;
        private int endIdx;
        private int displayLength;
        
        public Position () {
            
            this.startIdx = 0;
            this.endIdx   = 0;
            this.displayLength = 0;
        }
        
        /**
         * Skips the segment that we have been processing so far.
         */
        public void skip() {
            
            if (endIdx > strLength) {
                
                endIdx = strLength;
            }
            
            this.startIdx = endIdx;
            this.displayLength = 0;
        }
        
        /**
         * Retrieves the string that is contained within the current
         * segment.
         * 
         * @return the string contained within the current segment.
         */
        public String getSegment() {
            
            StringBuilder sb = new StringBuilder();
            
            /*
             * Ignore trailing white space
             */
            int actualEndIdx = endIdx;
            for (; actualEndIdx > startIdx && Character.isWhitespace(str.charAt(actualEndIdx-1)); 
                    --actualEndIdx); 
            
            for (int i = startIdx; i < actualEndIdx; i++) {
                
                char ch = str.charAt(i);
                if (ch == '\t') {
                    
                    for (int j = 0; j < tabExpansion; j++) {
                        
                        sb.append(' ');
                    }
                }
                else if (displayLength(ch) > 0) {
                    
                    sb.append(ch);
                }
            }
            
            return sb.toString();
        }
        
        public Object clone() {
            
            try {
                
                return super.clone();
            }
            catch (CloneNotSupportedException e) {
                
                /* CANNOT HAPPEN */
            }
            
            return null;
        }
        
        /**
         * Returns true if the position is at the end of the line.
         * @return true if the position is at the end of the line.
         */
        public boolean isEOL() {
            
            return (startIdx == strLength);
        }
        
        /**
         * Returns the display length of the line based upon the current
         * position in the string.
         * 
         * @return the length of the line.
         */
        public int getLen() {
            
            return displayLength;
        }
        
        /**
         * Peeks forward at the next character without shifting the position.
         * 
         * @return A glance at the next character.
         */
        public int peek() {
            
            if (endIdx >= strLength) {
                
                return -1;
            }
            
            int idx = endIdx;
            char ch = str.charAt(idx);
            ++idx;
            
            /*
             * Throw away undisplayable characteres.
             */
            if (ch != '\n' && displayLength(ch) == 0) {
                
                while (ch != '\n' && 
                        displayLength(ch) == 0 && idx < strLength) {
                    
                    ch = str.charAt(idx);
                    ++idx;
                }
                
                if (idx == strLength
                        && ch != '\n' && displayLength(ch) == 0) {
                    
                    return -1;
                }
            }
                
            return ch;
        }
        
        /**
         * Returns the next character in the string.
         * 
         * @return The next character in the string or -1 if the end of 
         * string has been reached.
         */
        public int next() {
            
            if (endIdx >= strLength) {
                
                return -1;
            }
            
            char ch = str.charAt(endIdx);
            ++endIdx;
            
            /*
             * Throw away undisplayable characteres.
             */
            if (ch != '\n' && displayLength(ch) == 0) {
                
                while (ch != '\n' && displayLength(ch) == 0
                        && endIdx < strLength) {
                    
                    ch = str.charAt(endIdx);
                    ++endIdx;
                }
                
                if (endIdx == strLength
                        && ch != '\n' && displayLength(ch) == 0) {
                    
                    return -1;
                }
            }
                
            displayLength += displayLength(ch);
            return ch;
        }
        
        /**
         * Returns the character that was most recently read, backing up
         * the position in the string.
         * @return The previous character or -1 if the start of string
         *   has been reached.
         */
        public int prev() {
            
            if (startIdx == endIdx) {
                
                return -1;
            }
            
            --endIdx;
            char ch = str.charAt(endIdx);
            
            if (displayLength(ch) == 0) {
                
                while (displayLength(ch) == 0 && endIdx > startIdx) {
                    
                    --endIdx;
                    ch = str.charAt(endIdx);
                }
                
                if (displayLength(ch) == 0 && endIdx == startIdx) {
                    
                    return -1;
                }
            }
            
            displayLength -= displayLength(ch);
            return ch;
        }
    }
}
