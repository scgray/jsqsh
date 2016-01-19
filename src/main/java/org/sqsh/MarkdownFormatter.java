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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

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
    
    // How much a normal text block is indented
    private static final int TEXT_INDENT = 2;
    // How many spaces a list item is indented
    private static final int LIST_INDENT = 2;

    /*
     * Spaces used as a source of characters for indenting. Who could
     * ever want to indent more than this?? ;)
     */
    private static final String SPACES = 
        "                                                               " +
        "                                                               " +
        "                                                               " +
        "                                                               " +
        "                                                               ";
    
    /**
     * Types of Markdown sections that we could currently be processing
     */
    protected static enum SectionType {
        TEXT,           // Normal text
        ULIST,          // Unordered list
        OLIST,          // Ordered list
    }
    
    /**
     * The state of the current list that is being worked on
     */
    protected static class ListState {
        
        public SectionType type;   // Type of list (ULIST or OLIST)
        public int         indent; // How much 
        public int         num;    // For OLIST, what is the current number?
        
        public ListState (SectionType type, int indent) {
            
            assert type == SectionType.OLIST || type == SectionType.ULIST;
            this.type = type;
            this.indent = indent;
            this.num  = 1;
        }
    }
    
    /**
     * Types of decoration that can be used for text.
     */
    protected static class Decoration {
        
        // Bits used to represent our decoration properties
        public static final int OFF        = 0;
        public static final int BOLD       = 1;
        public static final int DIM        = 2;
        public static final int ITALIC     = 4;
        public static final int UNDERSCORE = 8;
        public static final int RAW        = 16;
        
        // These are the actual codes that we will use to display our text
        // properties.  Note that they don't exactly line up with the terminal
        // codes  That is, for italics, I use underscore, because most 
        // terminals will not support italics.
        private static final int BOLD_CODE       = 1;
        private static final int DIM_CODE        = 2;
        private static final int ITALIC_CODE     = 4; // underscore
        private static final int UNDERSCORE_CODE = 4;
        private static final int RAW_CODE        = 1; // bold

        private static final Map<Integer, String> codes =
                        new HashMap<Integer, String>();
        
        public static final String OFF_ESC        = "\033[0m";
        public static final String BOLD_ESC       = getEscape(BOLD);
        public static final String DIM_ESC        = getEscape(DIM);
        public static final String ITALIC_ESC     = getEscape(ITALIC);
        public static final String UNDERSCORE_ESC = getEscape(UNDERSCORE);
        public static final String RAW_ESC        = getEscape(RAW);
        
        /**
         * Given a set of bits representing which decorations are currently
         * enabled, returns an ANSI escape code sequence needed to enable
         * those attributes on a terminal
         * 
         * @param bits The bits
         * @return The string containing the ANSI escape code
         */
        public static String getEscape(int bits) {
            
            if (bits == 0) {
                
                return OFF_ESC;
            }
            
            String code = codes.get(bits);
            if (code == null) {
                
                // The logic in this section is carefully designed so that 
                // if two decorations are physically the same decoration 
                // (e.g. italics are displayed as underline), then the 
                // physical property is only set once.
                int beenSet = 0;

                StringBuilder sb = new StringBuilder("\033[");
                if ((bits & BOLD) != 0) {
                    
                    beenSet |= (1 << BOLD_CODE);
                    sb.append(BOLD_CODE);
                }

                if ((bits & DIM) != 0) {
                    
                    if ((beenSet & (1 << DIM_CODE)) == 0) {
                        
                        if (sb.length() > 2)
                            sb.append(';');
                        beenSet |= (1 << DIM_CODE);
                        sb.append(DIM_CODE);
                    }
                }
                if ((bits & ITALIC) != 0) {
                    
                    if ((beenSet & (1 << ITALIC_CODE)) == 0) {
                        
                        if (sb.length() > 2)
                            sb.append(';');
                        beenSet |= (1 << ITALIC_CODE);
                        sb.append(ITALIC_CODE);
                    }
                }
                if ((bits & UNDERSCORE) != 0) {
                    
                    if ((beenSet & (1 << UNDERSCORE_CODE)) == 0) {
                        
                        if (sb.length() > 2)
                            sb.append(';');
                        beenSet |= (1 << UNDERSCORE_CODE);
                        sb.append(UNDERSCORE_CODE);
                    }
                }
                if ((bits & RAW) != 0) {

                    if ((beenSet & (1 << RAW_CODE)) == 0) {
                        
                        if (sb.length() > 2)
                            sb.append(';');
                        beenSet |= (1 << RAW_CODE);
                        sb.append(RAW_CODE);
                    }
                }
                
                sb.append('m');
                code = sb.toString();
                codes.put(bits, code);
            }
            
            return code;
        }
    }
    
    private PrintStream out;
    private WrappingStream wrappingOut;
    private StringBuilder block = new StringBuilder();
    private Line line = new Line();
    private Stack<ListState> listStates = new Stack<ListState>();

    public MarkdownFormatter (int screenWidth, PrintStream out) {
        
        this.out = out;
        this.wrappingOut = new WrappingStream(out, 0, screenWidth);
    }
    
    public void format (String str) {
        
        SectionType currentSectionType = SectionType.TEXT;
        boolean prevLineWasEmpty = true;
        boolean isFirstLine = true;
        listStates.clear();
        block.setLength(0);
        
        line.reset(str);
        wrappingOut.setIndent(0);
        
        while (line.next()) {

            int codeBlockIndentSize = 4 + (listStates.size() * 4);
            
            // A code block is a code block if:
            //   1. It is indented by four spaces deeper than the current
            //      list nesting level
            //   2. It was preceeded by an empty line
            // If we see this, then we process the whole code block here.
            //
            if (prevLineWasEmpty && ! line.isEmpty() 
                && line.indent >= codeBlockIndentSize) {
                
                // If the previous line was empty, then the previous block 
                // must have been spewed forth.
                assert block.length() == 0;
                
                boolean wasNext = false;
                
                // Dump all of the lines in the code block
                do {
                    
                    if (prevLineWasEmpty && ! isFirstLine) {
                        
                        out.println();
                    }

                    if (! line.isEmpty()) {

                        out.append(SPACES, 0, codeBlockIndentSize);
                        out.append(line.str, line.start + codeBlockIndentSize, 
                                line.contentEnd);
                        out.println();
                    }
                    
                    prevLineWasEmpty = line.isEmpty();
                    isFirstLine = false;
                }
                while ((wasNext = line.next())
                    && (line.isEmpty() || line.indent >= codeBlockIndentSize));
                
                if (! wasNext) {
                    
                    break;
                }
            }
            
            switch (line.type) {
            
            case TEXT:
                // In this context "TEXT" just means that the line didn't start
                // with something special like a header (##) or a bullet (*), 
                // so it could be normal text, or it could be the continuation
                // of, say, a bulleted list.
                
                // An empty line indicates that the current block needs to be
                // dumped
                if (line.isEmpty()) {
                    
                    printBlock();
                }
                else {
                    
                    if (prevLineWasEmpty 
                       && (currentSectionType == SectionType.TEXT 
                           || (line.indent == 0))) {
                        
                        if (! isFirstLine) {

                            out.println();
                        }

                        currentSectionType = SectionType.TEXT;
                        listStates.clear();
                        out.append(SPACES, 0, TEXT_INDENT);
                        wrappingOut.setIndent(TEXT_INDENT);
                    }
                    
                    if (block.length() > 0) {
                        
                        block.append(' ');
                    }

                    block.append(line.str, line.contentStart, line.contentEnd);
                    if (line.forcesNewline()) {
                            
                        block.append('\n');
                    }
                }
                break;

            case ULIST:
            case OLIST:
                // Dump the previous block we were working on.
                printBlock();
                
                // If we weren't in a list to begin with, then put a blank line
                // before the start of the list
                if (! isFirstLine && listStates.size() == 0) {
                    
                    out.println();
                }
                
                // It took me a lot of tinkering around on github to figure out.
                // List indenting works like this:
                //
                // 1. The first * is always the first indent level
                // 2. A * that is indented EXACTLY at the same level as
                //    the previous * is considered at the same level
                // 3. A * that is not indented exactly at the same level
                //    is considered the next level, even if it is before
                //    the previous * (!!)
                //
                // so:
                //
                //   * this is level 1
                // * and this is level 2
                //
                // is rendered like so:
                //
                //   * this is level 1
                //     * and this is level 2
              
                ListState state = null;
                int toDelete = 0;
                for (int i = 0; i < listStates.size(); i++) {
                    
                    ListState cur = listStates.get(i);
                    
                    // An exact match for indent
                    if (cur.indent == line.indent) { 

                        state = cur;
                        toDelete = listStates.size() - (i+1);
                        break;
                    }
                    else if (cur.indent < line.indent) {

                        // The indent is less than the level we are looking
                        // at, this could be:
                        //
                        //    * this is level 1
                        //  * this is level 2
                        //
                        // or it could be
                        //
                        //  * This is level 1
                        //      * This is level 2
                        //    * This is also level 2
                        //
                        if (i+1 < listStates.size()) {
                            
                            state = listStates.get(i+1);
                            toDelete = listStates.size() - (i+2);
                        }
                    }
                }
                
                // No matching indent level, so this must be a new one!
                if (state == null) {

                    currentSectionType = (line.type == Line.Type.OLIST 
                        ? SectionType.OLIST : SectionType.ULIST);
                    
                    state = new ListState(currentSectionType, line.indent);
                    listStates.push(state);
                }
                
                while (toDelete > 0) {
                    
                    listStates.pop();
                    --toDelete;
                }
                
                int newIndent = TEXT_INDENT + (LIST_INDENT * listStates.size());
                out.append(SPACES, 0, newIndent);

                if (currentSectionType == SectionType.ULIST) {
                    
                    out.print("* ");
                    wrappingOut.setIndent(newIndent + 2);
                }
                else {
                    
                    out.print(state.num);
                    out.print(". ");
                    newIndent += 2;
                    if (state.num < 10) {
                        
                        out.print(' ');
                        ++newIndent;
                    }
                    ++state.num;
                    wrappingOut.setIndent(newIndent);
                }
                
                block.append(line.str, line.contentStart, line.contentEnd);
                
                break;

                
            case HEADER:
                // A header ends the previous block, adds a newline (if necessary)
                // and resets all indent.
                printBlock();
                
                // For sex-appeal, levels 1 and 2 are always printed in
                // UPPER case
                String headerText = line.getContent();
                if (line.level < 3) {
                    
                    headerText = headerText.toUpperCase();
                }
                
                if (! isFirstLine) {
                    
                    out.println();
                }
                wrappingOut.setIndent(0);
                print(headerText, wrappingOut);

                currentSectionType = SectionType.TEXT;
                break;
                
            default:
                throw new RuntimeException("Unexpected line type!: " 
                    + line.type);
            }
            
            prevLineWasEmpty = line.isEmpty();
            isFirstLine = false;
        }
        
        printBlock();
    }
    
    protected void printBlock() {
        
        if (block.length() > 0) {
            
            print(block, wrappingOut);
            block.setLength(0);
        }
    }
    
    protected static void print(CharSequence block, WrappingStream wrappingOut) {

        int idx = 0;                      // Current location in the block
        int len = block.length();
        
        while (idx < len) {
            
            char ch = block.charAt(idx++);
            
            switch (ch) {
            
            case '`':
                
                // If we hit a backtick (code block), then seek forward until
                // we hit the end of the block.
                wrappingOut.raw(true);
                while (idx < len) {
                    
                    ch = block.charAt(idx++);
                    if (ch == '`') {
                        
                        break;
                    }
                    else {
                        
                        wrappingOut.print(ch);
                    }
                }
                wrappingOut.raw(false);
                break;

            // Some kind of emphasis.
            case '*':
                // Emphasis is only enabled if there is no white-space
                // following it and is only disabled if there is no whitespace
                // preceeding it.  That is, the following work:
                //
                // *c*
                // *a b*
                // a*c d*e
                // a**c d**e
                // 
                // but these leave the stars in-tact
                // 
                // * c*
                // *c *
                // *a b *
                
                int decorations = wrappingOut.getCurrentDecorations();

                // If italics are on then a star turns it off
                if ((decorations & Decoration.ITALIC) != 0) {
                    
                    boolean needStar = true;
                    
                    // Only let the star disable italic mode if the character
                    // preceeding it is a non-whitespace character.
                    if (idx > 1) {
                        
                        if (!Character.isWhitespace(block.charAt(idx-2))) {
                            
                            wrappingOut.italic(false);
                            needStar = false;
                        }
                    }

                    if (needStar) {
                        
                        wrappingOut.print('*');
                    }
                }
                else {
                    // Italics are off right now....
                    
                    // Do we have a second star? This could mean bold...
                    boolean isBold = false;
                    if (idx < len && block.charAt(idx) == '*') {
                        
                        // If bold was on, we can only turn it off if there 
                        // was a non-whitespace preceeding the **
                        if ((decorations & Decoration.BOLD) != 0) {
                            
                            boolean needStarStar = true;
                            
                            if (idx > 2 
                                && !Character.isWhitespace(block.charAt(idx-2))) {
                                    
                                    wrappingOut.bold(false);
                                    needStarStar = false;
                            }
                            
                            if (needStarStar) {
                                
                                wrappingOut.print('*');
                                wrappingOut.print('*');
                            }
                            
                            ++idx;
                            isBold = true;
                        }
                        else if (idx+1 < len 
                            && !Character.isWhitespace(block.charAt(idx+1))
                            && hasClosingBold(block, idx+1)) {
                            
                            // Bold was off, so we will turn it on if
                            // 1. The next character is a non-whitespace
                            // 2. There is a closing bold somewhere 
                            wrappingOut.bold(true);
                            ++idx;
                            isBold = true;
                        }
                    }
                    
                    // Italic was off and we didn't hit a bold, so we can be
                    // turning on italic if the next character is a non-whitespace
                    // and there is a closing italic
                    if (!isBold) {
                        
                        if (idx < len
                            && !Character.isWhitespace(block.charAt(idx))
                            && hasClosingItalic(block, idx+1)) {
                        
                            wrappingOut.italic(true);
                        }
                        else {
                            
                            wrappingOut.print('*');
                        }
                    }
                }
                break;
                
            case '\\':
                if (idx < len && isValidEscapeChar(block.charAt(idx))) {
                    
                    wrappingOut.print(block.charAt(idx));
                    ++idx;
                }
                break;
                
            case '[':
                // XXX -- TODO, URL's
                break;

            default: 
                wrappingOut.print(ch);
            }
        }
        
        wrappingOut.flush();
    }
    
    /**
     * Used to search to see if an opening italic (e.g. "*a...") has a 
     * matching closing italic (e.g. "...b*");
     * 
     * @param block The block being scanned
     * @param idx The point to start scanning
     * @return true if there is a closing italic
     */
    private static boolean hasClosingItalic(CharSequence block, int idx) {
        
        int len = block.length();
        while (idx < len) {
            
            char ch = block.charAt(idx);
            
            // Ignore *'s in code blocks
            if (ch == '`') { 

                idx = skipCode(block, len, idx+1);
            }
            else {
            
                if (ch == '*' && idx > 0 
                   && !Character.isWhitespace(block.charAt(idx-1))) {
                
                    return true;
                }

                ++idx;
            }
        }
        
        return false;
    }

    /**
     * Used to search to see if an opening bold (e.g. "**a...") has a 
     * matching closing bold (e.g. "...b**");
     * 
     * @param block The block being scanned
     * @param idx The point to start scanning
     * @return true if there is a closing italic
     */
    private static boolean hasClosingBold(CharSequence block, int idx) {
        
        int len = block.length();
        while (idx < len) {
            
            char ch = block.charAt(idx);
            
            // Ignore *'s in code blocks
            if (ch == '`') { 

                idx = skipCode(block, len, idx+1);
            }
            else {
            
                // Hit the first star
                if (ch == '*') {
                    
                    // It can only be closing if there is a non-whitespace 
                    // and non-star before
                    boolean canBeClosing = false;
                    if (idx > 0) {
                        
                        ch = block.charAt(idx-1);
                        canBeClosing = ch != '*' && !Character.isWhitespace(ch);
                    }

                    
                    // Eat the '*'
                    ++idx;
                    
                    // Is there another one?
                    if (idx < len && block.charAt(idx) == '*') {
                        
                        // Eat it too
                        ++idx;
                        if (canBeClosing) {
                            
                            return true;
                        }
                    }
                }

                ++idx;
            }
        }
        
        return false;
    }
    
    private static int skipCode(CharSequence block, int len, int idx) {

        for (++idx; idx < len; idx++) {
            
            if (block.charAt(idx) == '`') {
                
                return idx+1;
            }
        }
        
        return len;
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

        public static enum Type {
            
            TEXT,
            ULIST,
            OLIST,
            HEADER,
        }
        
        public Type     type;
        
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
            
            char ch = str.charAt(idx);

            // Do we have a header?
            if (ch == '#') {

                ++idx;
                while (idx < len && str.charAt(idx) == '#') {
                    
                    ++idx;
                }

                type = Type.HEADER;
                level = (idx - start);
                contentStart = skipWhitespace(str, len, idx);
            }
            else if (ch == '\n') {
                
                type  = Type.TEXT;
                indent = 0;
                contentStart = idx;
            }
            else {
                
                idx = skipWhitespace(str, len, idx);
                if (idx >= len) {
                    
                    // We had nothing but whitespace, so it is an empty line
                    this.type = Type.TEXT;
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
                        
                            type = Type.OLIST;
                            contentStart = skipWhitespace(str, len, idx+2);
                        }
                        else {

                            type  = Type.TEXT;
                        }
                    }
                    else {
                    
                        switch (ch) {
                        
                        case '*': 
                            if (idx < len && Character.isWhitespace(str.charAt(idx))) {
                                
                                type = Type.ULIST;
                                contentStart = skipWhitespace(str, len, idx+1);
                            }
                            else {
                                type = Type.TEXT;
                                contentStart = idx-1;
                            }
                            break;

                        default:
                            type = Type.TEXT;
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
        
        /**
         * @return True if the last two characters of the line are spaces,
         *   which indicates a forced newline
         */
        public boolean forcesNewline() {
            
            return (contentEnd - contentStart >= 2)
               && str.charAt(contentEnd-1) == ' '
               && str.charAt(contentEnd-2) == ' ';
        }
        
        public boolean isEmpty() {
            
            return contentStart == contentEnd;
        }
        
        public String getContent() {
            
            return str.substring(contentStart, contentEnd);
        }

        protected static int skipWhitespace(String str, int len, int idx) {
            
            while (idx < len) {
                
                char ch = str.charAt(idx);
                if (! Character.isWhitespace(ch) || ch == '\n') {
                    
                    return idx;
                }
                
                ++idx;
            }
            
            return len;
        }
        
        @Override
        public String toString() {
            
            StringBuilder sb = new StringBuilder();
            sb.append(">>");
            sb.append(str, start, start + indent);
            sb.append("^");
            sb.append(str, start + indent, contentStart);
            sb.append('[');
            sb.append(str, contentStart, contentEnd);
            sb.append(']');
            sb.append("<<");
            
            return sb.toString();
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
        private int decorations = Decoration.OFF;
        private StringBuilder word = new StringBuilder();
        private int wordWidth = 0;
        private int lineWidth;
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
         * Set the indent level of this stream.  This may only be called
         * before any text is output, or immediately after flush() is called.
         * 
         * @param indent The new indent level
         */
        public void setIndent(int indent) {
            
            this.indent = indent;
            this.lineWidth = indent;
        }
        
        /**
         * Toggles a particular decoration on or off
         * 
         * @param decoration The decoration to toggle
         */
        public void toggleDecoration(int decoration) {
            
            if ((decorations & decoration) == 0) {
                
                decorationOn(decoration);
            }
            else {
                
                decorationOff(decoration);
            }
        }
        
        /**
         * @return The current set of decoration bits that are enabled.
         */
        public int getCurrentDecorations() {
            
            return decorations;
        }
        
        /**
         * Enables or disables "raw" mode.  With raw mode, every character
         * sent to the output is directly sent without interpretation. The main
         * main side effect is that all white space is preserved exactly as send
         * (repeating white spaces are not removed).
         * 
         * @param onOff If true, then raw mode is enabled
         */
        public void raw (boolean isOn) {
            
            if (isOn) 
                decorationOn(Decoration.RAW);
            else 
                decorationOff(Decoration.RAW);
        }
        
        /**
         * Turns BOLDing on or off
         * @param isOn true if on, false if off
         */
        public void bold (boolean isOn) {
            
            if (isOn) 
                decorationOn(Decoration.BOLD);
            else 
                decorationOff(Decoration.BOLD);
        }

        /**
         * Turns underscoring on or off
         * @param isOn true if on, false if off
         */
        public void underscore (boolean isOn) {
            
            if (isOn) 
                decorationOn(Decoration.UNDERSCORE);
            else 
                decorationOff(Decoration.UNDERSCORE);
        }
        
        /**
         * Turns italic on or off
         * @param isOn true if on, false if off
         */
        public void italic (boolean isOn) {
            
            if (isOn) 
                decorationOn(Decoration.ITALIC);
            else 
                decorationOff(Decoration.ITALIC);
        }

        /**
         * Turns dim on or off
         * @param isOn true if on, false if off
         */
        public void dim (boolean isOn) {
            
            if (isOn) 
                decorationOn(Decoration.DIM);
            else 
                decorationOff(Decoration.DIM);
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
            else if ((decorations & Decoration.RAW) != 0) {
                
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
         * (turning off decorations, disabling raw mode, etc).  This will also
         * ensure that the output is terminated with a final newline.
         */
        public void flush() {
            
            dumpWord(false);
            
            // If needIndent is true, then someone already turned off decorations
            // and pushed a newline out, so we don't need to do it here.
            if (! needIndent) {

                if (decorations != Decoration.OFF) {
                    
                    out.print(Decoration.OFF_ESC);
                }

                out.println();
            }

            out.flush();
            
            decorations = Decoration.OFF;
            lineWidth = indent;
            needIndent = false;
        }

        private void decorationOn(int bit) {
            
            assert bit != Decoration.OFF; 

            // Bit is already set, it is a no-op
            if ((decorations & bit) != 0) {
                
                return;
            }

            // Entering raw mode.  We need to dump the last word that we were
            // in the middle of processing.
            if (bit == Decoration.RAW) {

                // If we have a word that has been accumulated but not displayed
                // then we have this scenario:
                //    abc`def`
                // however, if there had previously been a space, like so:
                //    abc `def`
                // then the word would have been dumped already, which means
                // we need to insert a space to give the proper output.
                boolean needSpace = (wordWidth == 0);
                dumpWord(false);
                
                // We have to insert a space...
                if (needSpace) {

                    // Won't fit on the screen? Then wrap...
                    if (lineWidth+1 >= screenWidth) {
                        
                        wrap();
                        checkIndent(true);
                    }
                    else if (lineWidth > indent) {
                        
                        // Only add the space if this isn't at the start of
                        // the line
                        out.print(' ');
                        ++lineWidth;
                    }
                }
            }

            decorations |= bit;
            
            // Ensure BOLD and DIM are not on at the same time
            if (bit == Decoration.BOLD) {
                
                decorations &= ~(Decoration.DIM);
            }
            else if (bit == Decoration.DIM) {
                
                decorations &= ~(Decoration.BOLD);
            }

            if (bit == Decoration.RAW) {

                out.print(Decoration.getEscape(decorations));
            }
            else {
                
                word.append(Decoration.getEscape(decorations));
            }
        }
        
        private void decorationOff(int bit) {

            assert bit != Decoration.OFF; 

            // Bit is already off, it is a no-op
            if ((decorations & bit) == 0) {
                
                return;
            }
            
            decorations &= ~(bit);
            if ((decorations & Decoration.RAW) != 0) {

                out.print(Decoration.getEscape(decorations));
            }
            else {
                
                word.append(Decoration.getEscape(decorations));
            }
        }
        
        private void checkIndent(boolean force) {

            if (needIndent || force) {

                out.append(SPACES, 0, indent);
                if (decorations != Decoration.OFF) {
                    
                    out.print(Decoration.getEscape(decorations));
                }

                needIndent = false;
                lineWidth = indent;
            }
        }

        private void dumpWord(boolean forceWrap) {
            
            if (word.length() > 0) {

                // If the word isn't at the start of the line, then we need 
                // a space to separate it from the previous one
                boolean needsSpace = (wordWidth > 0 && lineWidth > indent);

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

            if (decorations != Decoration.OFF) {
            
                out.print(Decoration.OFF_ESC);
            }
            
            out.println();
            needIndent = true;
        }
    }
}
