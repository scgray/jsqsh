/*
 * Copyright 2007-2015 Scott C. Gray
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

import java.io.PrintStream;

/**
 * JSqsh's internal help is written in a subset of markdown. Although
 * markdown is intended to be relatively readable as formatted, it isn't
 * readable enough for my taste, so this class is used to nicely format
 * the output taking into account things like the current screen width.
 * 
 * <p>This formatter handles the following markdown elements:
 * <ul>
 * </ul>
 */
public class MarkdownFormatter {
    
    private static final String SPACES = 
        "                                                               " +
        "                                                               " +
        "                                                               " +
        "                                                               " +
        "                                                               ";

    protected static enum LineType {
        
        TEXT,
        ULIST,
        OLIST,
        HEADER,
    }

    protected static enum Decoration {
        
        OFF("\033[0;m"),
        ITALIC("\033[4;m"),        // We use underscore here
        BOLD("\033[1;m"),          // "Bright"
        BOLD_ITALIC("\033[1;4;m"); // Bright, underscore

        private String code;
        
        private Decoration (String code) {
            
            this.code = code;
        }
        
        public String getCode() {
            
            return code;
        }
        
        @Override
        public String toString() {
            
            return code;
        }
    }
    
    /**
     * Raw (code formatting) is a special case of decoration.  It is 
     * presented as bold since our output font is already fixed space,
     * but with raw you can also be mixed into other decorations. For
     * example, you can do bold+raw (**`text`**), so the raw decoration
     * state is maintained separately from the other styles.
     */
    protected static final Decoration RAW_DECORATION = Decoration.BOLD;
    
    protected static enum State {
        
        BODY,
        LIST,
    }
    
    private int width;
    private PrintStream out;
    private StringBuilder sb = new StringBuilder();
    private Line line = new Line();
    private int[] wordWidth = new int[1];
    private Decoration decoration = Decoration.OFF;

    public MarkdownFormatter (int width, PrintStream out) {
        
        this.width = width;
        this.out = out;
    }
    
    public void format (String str) {
        
        int bulletLevel = 0;
        line.reset(str);
        sb.setLength(0);
        decoration = Decoration.OFF;
        
        while (line.next()) {
            
            switch (line.type) {
            
            case TEXT:
            case ULIST:
            case OLIST:
            case HEADER:
                out.println(sb);
                sb.setLength(0);

                if (line.level < 3) {
                    
                    String upper = line.getContent().toUpperCase();
                    // print(width, 0, upper, 0, upper.length(), sb);
                    out.println(sb.toString());
                    sb.setLength(0);
                }
                else {
                    
                    // print(width, 0, str, line.contentEnd, line.contentEnd, sb);
                    out.println(sb.toString());
                    sb.setLength(0);
                }

                bulletLevel = 0;
                break;
                
            default:
                throw new RuntimeException("Unexpected line type!: " 
                    + line.type);
            }
        }
    }

/*    
    protected static void print(StringBuilder block, StringBuilder word, 
        int indent, int screenWidth, PrintStream out) {

        int width      = indent;  // Current line in printable characters
        int wordWidth  = 0;
        int idx        = 0;       // Current location in the block
        int len        = block.length();
        Decoration decoration = Decoration.OFF;
        boolean inBacktick = false;
        boolean wordDone = false;
        
        word.setLength(0);
        
        while (idx < len) {
            
            // Had we completed collecting a word?
            if (wordDone) {
                
                // Will the word fit on the current line?
                if ((width + wordWidth+1) < screenWidth) {
                    
                    if (width > indent) {
                        
                        out.print(' ');
                    }
                }
                
                word.setLength(0);
                wordWidth = 0;
            }
            
            if (width == screenWidth) {
                
                needIndent = true;
                out.println(line);
                line.setLength(0);
            }
            else if (width > screenWidth) {
                
                // If wordEnd == indent, then we didn't have a break-able point
                // in the line, so we just blindly wrap
                if (wordEnd == indent) {
                    
                    out.append(line, 0, screenWidth-indent);
                    line.delete(0, screenWidth - indent);
                    out.println();
                }
                else {
                    
                    needIndent = true;
                    out.append(line, 0, wordEnd);

                    // There is always a space at the word end, so we will
                    // delete that space since that is where we wrapped
                    if (wordEnd + 1 >= line.length()) {
                        
                        line.setLength(0);
                    }
                    else {
                        
                        line.delete(0, wordEnd+1);
                    }
                    out.println();
                }
            }
            
            if (needIndent) {
                
                out.append(SPACES, 0, indent);
            }
            
            char ch = block.charAt(idx++);
            if (inBacktick) {

                if (ch == '`') {
                    
                    inBacktick = false;
                }
                else if (ch == '\\' && idx < len 
                            && isValidEscapeChar(block.charAt(idx))) {
                            
                    line.append(ch);
                    ++width;
                    ++idx;
                }
                else {
                            
                    line.append(ch);
                    ++width;
                }
            }
            else if (Character.isWhitespace(ch)) {
                
                while (idx < len && Character.isWhitespace(block.charAt(idx))) {
                    
                    ++idx;
                }
                
                if (idx < len) {
                    
                    wordEnd = line.length();
                    line.append(' ');
                    ++width;
                }
            }
            else {

                switch (ch) {
                
                // Backtick is an in-line code block
                case '`': 
                    inBacktick = true;
                    break;

                // Some kind of emphasis
                case '*':
                    int count = 1;
                    while (idx < len && block.charAt(idx) == '*') {
                        
                        ++count;
                        ++idx;
                    }
                    
                    switch (count) {
                    
                    case 1:
                        switch (decoration) {
                        case OFF:
                            decoration = Decoration.ITALIC;
                            line.append(decoration.getCode());
                            break;
                        case ITALIC:
                            decoration = Decoration.OFF;
                            line.append(decoration.getCode());
                            break;
                        default:
                            line.append('*');
                            width += 1;
                        }
                        break;
                    case 2:
                        switch (decoration) {
                        case OFF:
                            decoration = Decoration.BOLD;
                            line.append(decoration.getCode());
                            break;
                        case ITALIC:
                            decoration = Decoration.OFF;
                            line.append(decoration.getCode());
                            line.append('*');
                            width += 1;
                            break;
                        case BOLD:
                            decoration = Decoration.OFF;
                            line.append(decoration.getCode());
                            break;
                        default:
                            line.append("***");
                            width += 3;
                        }
                        break;
                    default:
                        switch (decoration) {
                        case OFF:
                            decoration = Decoration.BOLD_ITALIC;
                            line.append(decoration.getCode());
                            break;
                        case ITALIC:
                            decoration = Decoration.OFF;
                            line.append(decoration.getCode());
                            line.append("**");
                            width += 2;
                            break;
                        case BOLD:
                            decoration = Decoration.OFF;
                            line.append(decoration.getCode());
                            line.append('*');
                            width += 1;
                            break;
                        default: // Only BOLD_ITALIC left
                            decoration = Decoration.OFF;
                            line.append(decoration.getCode());
                        }
                        
                        for (count -= 3; count > 0; --count) {
                            
                            line.append('*');
                            ++width;
                        }
                    }
                    break;
                    
                case '\\':
                    if (idx < len && isValidEscapeChar(block.charAt(idx))) {
                        
                        line.append(block.charAt(idx));
                        ++idx;
                    }
                    ++width;
                    break;
                    
                case '[':
                    // XXX
                    break;

                default: 
                    line.append(ch);
                     ++width;
                }
            }
        }
    }
*/
    
    protected static int skipWhitespace(String str, int len, int idx) {
        
        while (idx < len && Character.isWhitespace(str.charAt(idx))) {
            
            ++idx;
        }
        
        return idx;
    }

    
    private static boolean isValidEscapeChar (char ch) {

        return (ch == '\\' || ch == '`' || ch == '*' || ch == '_' 
                        || ch == '{' || ch == '}' || ch == '[' || ch == ']'
                        || ch == '(' || ch == ')' || ch == '#' || ch == '+'
                        || ch == '-' || ch == '.' || ch == '!');
    }

    /**
     * Simple class used to iterate over lines in a markdown file. As the
     * iteration occurs, each line is classified as to the type of line it is
     * (TEXT, HEADER, ULIST, OLIST, etc). and the contents of the line are
     * identified.
     */
    protected static class Line {
        
        public LineType type;
        
        /**
         * The physical start of the current line
         */
        public int      start;

        /**
         * The beginning of the "content" for the current line.  The content
         * is the meaningful stuff that should be displayed...for headers, it 
         * is the header text, for bullets the bullet body, etc.
         */
        public int      contentStart; // Where the content of the line starts

        /**
         * The end of the content.
         */
        public int      contentEnd;

        /**
         * How much leading white space the current line has.
         */
        public int      indent;
        
        /**
         * For headers this is the level
         */
        public int      level;

        public String   str;
        public int      len;
        public int      idx;

        public Line () {
            
        }
        
        public Line (String str) {
            
            this.str = str;
            this.len = str.length();
        }
        
        public void reset (String str) {
            
            this.str = str;
            this.len = str.length();
            this.idx = 0;
        }
        
        public boolean next () {
            
            start = contentStart = idx;
            level = indent = 0;
            
            if (idx >= len) {
                
                return false;
            }
            
            char ch = str.charAt(idx++);

            // Do we have a header?
            if (ch == '#') {

                while (idx < len && str.charAt(idx) == '#') {
                    
                    ++idx;
                }

                type = LineType.HEADER;
                level = (idx - start);
                contentStart = skipWhitespace(str, len, idx);
            }
            else if (ch == '\n') {
                
                type  = LineType.TEXT;
                indent = 0;
                // "unconsume" the newline, the logic to find the end of
                // our line will re-consume it for us.
                --idx;
                contentStart = idx;
            }
            else {
                
                idx = skipWhitespace(str, len, idx);
                if (idx >= len) {
                    
                    // We had nothing but whitespace, so it is an empty line
                    this.type = LineType.TEXT;
                    this.indent = len - contentStart;
                    this.contentStart = len;
                }
                else {

                    contentStart = idx;
                    indent = (idx - start);
                    
                    ch = str.charAt(idx++);
                    if (ch >= '0' && ch <= '9') {
                    
                        // We possibly have a numbered list. 
                        while (idx < len && Character.isDigit(str.charAt(idx))) {
                        
                            ++idx;
                        }
                    
                        if (idx+1 < len && str.charAt(idx) == '.'
                            && Character.isWhitespace(str.charAt(idx+1))) {
                        
                            type = LineType.OLIST;
                            contentStart = skipWhitespace(str, len, idx+2);
                        }
                        else {

                            type  = LineType.TEXT;
                        }
                    }
                    else {
                    
                        switch (ch) {
                        
                        case '*': 
                            if (idx < len && Character.isWhitespace(str.charAt(idx))) {
                                
                                type = LineType.ULIST;
                                contentStart = skipWhitespace(str, len, idx+1);
                            }
                            else {
                                type = LineType.TEXT;
                                contentStart = idx-1;
                            }
                            break;

                        default:
                            type = LineType.TEXT;
                            contentStart = idx-1;
                        }
                    }
                }
            }
            
            // Ok, we found the start of the content, now seek forward to
            // the end of the line and we are done.
            idx = contentStart;
            while (idx < len) {
                
                ch = str.charAt(idx);
                if (ch == '\r' && (idx+1) < len && str.charAt(idx+1) == '\n') {
                    
                    contentEnd = idx;
                    idx += 2;
                    return true;
                }
                else if (ch == '\n') {
                    
                    contentEnd = idx;
                    ++idx;
                    return true;
                }
                
               ++idx;
            }

            contentEnd = idx;
            return true;
        }
        
        public boolean isEmpty() {
            
            return contentStart == contentEnd;
        }
        
        public String getContent() {
            
            return str.substring(contentStart, contentEnd);
        }
    }
    
    /**
     * A "stream" class that can output text along with decorations, such 
     * as bold, italics, etc. and, while displaying the output, can perform
     * world wrapping to ensure that the output stays within a desired output
     * width.
     */
    protected static class WrappingStream {
        
        private PrintStream out;
        private int indent;
        private int screenWidth;
        private Decoration decoration = Decoration.OFF;
        private StringBuilder word = new StringBuilder();
        private int wordWidth = 0;
        private int lineWidth;
        private boolean isRaw = false;
        private boolean needIndent = false;
        
        /**
         * Creates a new stream
         * 
         * @param out Where the final output should be sent
         * @param indent When lines wrap, this is the number of spaces that
         *   should be indented when displaying the wrapped lines
         * @param screenWidth The maximum display width before wrapping
         */
        public WrappingStream (PrintStream out, int indent, int screenWidth) {
            
            this.out = out;
            this.indent = indent;
            this.lineWidth = indent;
            this.screenWidth = screenWidth;
        }
        
        /**
         * Enables or disables "raw" mode.  With raw mode, every character
         * sent to the output is directly sent without interpretation. The main
         * main side effect is that all white space is preserved exactly as send
         * (repeating white spaces are not removed).  While in raw mode, it is
         * illegal to attempt to send a decoration to the output
         * 
         * @param isRaw If true, then raw mode is enabled
         */
        public void setRaw (boolean isRaw) {
            
            if (isRaw == this.isRaw) {
                
                throw new RuntimeException("Raw mode is already set to " 
                                + isRaw);
            }
            
            // Is raw mode being turned on?
            if (! this.isRaw && isRaw) {
                
                dumpWord(false);
            }
            
            this.isRaw = isRaw;
            
            // If we are entering raw mode, then print the raw decoration, 
            // otherwise restore the previous decoration that was in use.
            if (isRaw) {
                
                // Raw is BOLD, so if the previous decoration was ITALIC, then
                // we enter BOLD_ITALIC mode instead.
                if (decoration == Decoration.ITALIC) {

                    out.print(Decoration.BOLD_ITALIC);
                }
                else {

                    out.print(RAW_DECORATION);
                }
            }
            else {
                
                out.print(decoration);
            }
        }
        
        /**
         * Sends a decoration (bold, underline, etc.) to the output.  This is
         * only legal when raw mode is disabled. It is also illegal to enter
         * one decoration mode without leaving the existin one...that is, you
         * may not go directly from "bold" to "underline" without first 
         * disabling bold
         * 
         * @param decoration The decoration to send
         */
        public void print (Decoration decoration) {

            if (isRaw) {
                
                throw new RuntimeException("Cannot add decoration in raw mode!");
            }
            
            // Ignore requests to change decorations to the existing decoration
            if (this.decoration == decoration) {
                
                return;
            }
            
            // Cannot go from one decoration to another without being in "OFF"
            // mode first
            if (decoration != Decoration.OFF && this.decoration != Decoration.OFF) {
                
                throw new RuntimeException("Must leave one decoration mode " +
                   "before entering another");
            }
            
            this.decoration = decoration;
            word.append(decoration.getCode());
        }
        
        /**
         * Sends a character to the output, performing word wrap as necessary
         * 
         * @param ch The character to print
         */
        public void print (char ch) {
            
            checkIndent(false);

            if (ch == '\n') {
                
                dumpWord(true);
            }
            else if (ch == '\r') {
                
                return;
            }
            else if (isRaw) {
                
                if (lineWidth >= screenWidth) {
                    
                    wrap();
                    checkIndent(true);
                }

                out.print(ch);
                ++lineWidth;
            }
            else {
                
                // A space indicates the end of the previous word and the
                // start of a new one
                if (Character.isWhitespace(ch)) {
                    
                    // However, our "word" accumulated so far may consists only
                    // of non-printable control characters (bold, underline, etc),
                    // so only dump if there are printable characters.
                    if (wordWidth > 0) {

                        dumpWord(false);
                    }
                }
                else {
                    
                    // Add the letter to the word
                    word.append(ch);
                    ++wordWidth;
                    
                    // Does the word fit on the current line?
                    if (lineWidth + wordWidth >= screenWidth) {
                        
                        // Special case, if the line is the full length of
                        // the line, then just force it to dump
                        if (lineWidth == indent) {
                            
                            dumpWord(true);
                        }
                        else {
                            
                            wrap();

                            // Force a line wrap and indent in preparation
                            // for our new long word
                            checkIndent(true);
                        }
                    }
                }
            }
        }
        
        /**
         * Flushes the output stream and resets all state of the output
         * (turning off decorations, disabling raw mode, etc).
         */
        public void flush() {
            
            dumpWord(false);
            if (decoration != Decoration.OFF) {
                
                out.print(Decoration.OFF.getCode());
            }
            out.flush();
            
            
            decoration = Decoration.OFF;
            lineWidth = indent;
            isRaw = false;
            needIndent = false;
        }
        
        private void checkIndent(boolean force) {

            if (needIndent || force) {

                out.append(SPACES, 0, indent);
                if (isRaw) {
                    
                    // Raw decoration is BOLD, so if we were in ITALIC already
                    // then enter BOLD_ITALIC, otherwise we just go bold.
                    if (decoration == Decoration.ITALIC) {
                        
                        out.print(Decoration.BOLD_ITALIC);
                    }
                    else {
                        
                        out.print(RAW_DECORATION);
                    }
                }
                else {

                    if (decoration != Decoration.OFF) {
                    
                        out.print(decoration);
                    }
                }
            
                needIndent = false;
                lineWidth = indent;
            }
        }

        private void dumpWord(boolean forceWrap) {
            
            if (word.length() > 0) {

                // If the word isn't at the start of the line, then we need 
                // a space to separate it from the previous one
                boolean needsSpace = (lineWidth > indent);

                // Dump the current word, wrapping if necessary
                int charsToBeAdded = wordWidth + (needsSpace ? 1 : 0);

                if ((lineWidth + charsToBeAdded) <= screenWidth) {
                    
                    if (needsSpace) {
                        
                        out.print(' ');
                    }

                    out.print(word);
                    lineWidth += charsToBeAdded;
                    wordWidth = 0;
                    word.setLength(0);
                }
                else {
                    
                    // The word won't fit on the current line, force a word wrap
                    // and indent
                    checkIndent(true);
                    
                    out.print(word);
                    lineWidth += wordWidth;
                    word.setLength(0);
                    wordWidth = 0;
                }
            }
            
            if (forceWrap) {
                
                wrap();
            }
        }
        
        private void wrap() {

            if (isRaw || decoration != Decoration.OFF) {
            
                out.print(Decoration.OFF);
            }
            
            out.println();
            needIndent = true;
        }
    }
}
