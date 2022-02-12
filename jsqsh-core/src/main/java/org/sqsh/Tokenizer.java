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

import org.sqsh.shell.ShellException;
import org.sqsh.shell.ShellManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * This class is responsible for parsing a command line, performing variable expansion and backtick processing
 * as needed along the way, as well as interpreting any special shell actions, such as pipes and redirections,
 * which are then exposed as special tokens in the stream. Care is taken to try to keep the parsing in line
 * with Bash behavior, including when variables get expanded and how they are treated as tokens in the stream.
 * As one example:
 * <pre>
 *    echo hello `echo "how are you"`
 * </pre>
 * Even though the backtick execution of the {@code echo} command quotes the "how are you", according to how bash
 * (and presumably any posix shell?) processes it, the tokens produced will be:
 * <pre>
 *   hello
 *   how
 *   are
 *   you
 * </pre>
 * and NOT
 * <pre>
 *   hello
 *   how are you
 * </pre>
 *
 * <h2>Places that need work</h2>
 * During backtick processing, care is taken to ensure that quotes are retained as specified by the user,
 * yet jsqsh variables may be expanded so, for example:
 * <pre>
 *    \set x="hello world!"
 *    \echo `echo "$x"`
 * </pre>
 * The back-ticked command executed will be {@code echo "hello world!"}. Similarly:
 * <pre>
 *    \echo `echo '$x'`
 * </pre>
 * would be {@code echo '$x'}. The problem that remains is dual-expansion. For example:
 * <pre>
 *    \set a='$HOME'
 *    \echo `echo $a`
 * </pre>
 * will result in displaying your home path, whereas in normal shell expansion rules, it should result
 * in the string {@code $HOME}. This is fixable with strategic insertion of single quotes, but it is a
 * fight I don't feel like fighting at the moment. :)
 */
public class Tokenizer {
    private static final Logger LOG = Logger.getLogger(Tokenizer.class.getName());

    public static final String NEWLINE = "\n";
    public static final String CARRIAGE_RETURN = "\r";
    public static final String TAB = "\r";
    public static final String WHITESPACE = " \t\n\r";

    /**
     * String expander to use to expand variables during tokenization. This may be null.
     */
    private final StringExpander expander;

    /**
     * If backtick expansion is supported (e.g. {@code `cat file`}).
     */
    private final boolean expandBackTicks;

    /**
     * If backtick expansion is enabled, how to split the results.
     */
    private final String fieldSeparater;

    /**
     * If true, then literals contained within double quotes will retain the surrounding double quotes.
     */
    private boolean retainDoubleQuotes;

    /**
     * If the first token parsed has a leading escape (e.g. {@code \echo}) keep the escape character.
     */
    private final boolean retainInitialEscape;

    /**
     * Line terminator
     */
    private final int terminator;

    /**
     * When expanding back-ticks, the ShellManager is used to execute the command;
     */
    private final ShellManager shellManager;

    /**
     * Backtick expansion happens by running the command, breaking it into tokens, then pushing them into
     * this queue. The next() will feed off of it until exhausted.
     */
    private final Queue<Token> pendingTokens = new LinkedList<>();

    private final LineContext context;

    private int tokenCount = 0;

    private Tokenizer(String line, StringExpander expander, boolean expandBackTicks, String fieldSeparater,
                      ShellManager shellManager, boolean retainDoubleQuotes, boolean retainInitialEscape,
                      int terminator) {
        this.expander = expander;
        this.expandBackTicks = expandBackTicks;
        this.fieldSeparater = fieldSeparater;
        this.shellManager = shellManager;
        this.retainDoubleQuotes = retainDoubleQuotes;
        this.retainInitialEscape = retainInitialEscape;
        this.terminator = terminator;
        this.context = new LineContext(line);
    }

    public static Builder newBuilder(String line) {
        return new Builder(line);
    }

    /**
     * Given a string full of characters, replaces certain escaped characters with their literal text representations.
     * Specifically, this recognized the following special characters:
     * <ul>
     *     <li> \n - Newline</li>
     *     <li> \r - Carriage return</li>
     *     <li> \t - Tab</li>
     *     <li> \s - Treated as " \n\r\t"
     * </ul>
     * @param sep a set of characters, possibly containing escape sequences to be expanded
     * @return The expanded separator characters
     */
    public static String toFieldSeparator(String sep) {
        final int len = sep.length();
        final StringBuilder buf = new StringBuilder();
        int idx = 0;
        while (idx < len) {
            char ch = sep.charAt(idx);
            if (ch == '\\') {
                ++idx;
                if (idx < sep.length()) {
                    ch = sep.charAt(idx);
                    ++idx;
                    switch (ch) {
                        case 'n':
                            buf.append(NEWLINE);
                            break;
                        case 'r':
                            buf.append(CARRIAGE_RETURN);
                            break;
                        case 't':
                            buf.append(TAB);
                            break;
                        case 's':
                            buf.append(WHITESPACE);
                            break;
                        default:
                            buf.append(ch);
                    }
                } else {
                    buf.append("\\");
                    ++idx;
                }
            } else {
                buf.append(ch);
                ++idx;
            }
        }

        return buf.toString();
    }

    public static String fromFieldSeparator(String fieldSeparator) {
        final StringBuilder sb = new StringBuilder();
        final int len = fieldSeparator.length();

        int newlineCount = 0;
        int carriageReturnCount = 0;
        int tabCount = 0;
        int spaceCount = 0;
        int nonWhitespaceCount = 0;
        for (int i = 0; i < fieldSeparator.length(); i++) {
            char ch = fieldSeparator.charAt(i);
            switch (ch) {
                case '\n':
                    sb.append("\\n");
                    ++newlineCount;
                    break;
                case '\r':
                    sb.append("\\r");
                    ++carriageReturnCount;
                    break;
                case '\t':
                    sb.append("\\t");
                    ++tabCount;
                    break;
                case ' ':
                    sb.append(ch);
                    ++spaceCount;
                    break;
                default:
                    sb.append(ch);
                    ++nonWhitespaceCount;
                    break;
            }
        }

        if (nonWhitespaceCount == 0
            && newlineCount > 0
            && carriageReturnCount > 0
            && tabCount > 0
            && spaceCount > 0) {
            return "\\s";
        }

        return sb.toString();
    }

    /**
     * Returns the next token on the command line.
     *
     * @return The parsed token, or null if the end-of-line has been reached.
     */
    public Token next() throws CommandLineSyntaxException {
        if (!pendingTokens.isEmpty()) {
            ++tokenCount;
            return pendingTokens.remove();
        }

        context.skipWhiteSpace();

        // If we are already at the end of the line, then just return null.
        if (context.isEnd()) {
            return null;
        }

        char ch = context.peek();
        Token token;

        // First we are going to check to see if we have hit a file descriptor output redirection operation. This can
        // be of the form:
        //    X>&Y
        //    >
        //    >>
        //    X>
        //    X>>
        //    >+X
        //    >>+X
        if (ch == '>' || (context.hasAtLeast(2) && Character.isDigit(ch) && context.peek(1) == '>')) {
            token = parseOutputRedirection();
        } else if (ch == '|') {
            token = parsePipe();
        } else if (isTerminator(ch)) {
            token = parseTerminator();
        } else if (expandBackTicks && ch == '`') {
            token = doBackTick();
            // Backtick can return nothing with: `echo foo >/dev/null`, in this case the shell treats it like
            // it never even happened, so we go back to the beginning.
            if (token == null) {
                return next();
            }
        } else {
            // Failing the above, we have to assume that we are just parsing a plain old string.
            token = parseString(retainDoubleQuotes);
        }

        ++tokenCount;
        return token;
    }

    /**
     * @return A token representing the terminator.
     */
    private TerminatorToken parseTerminator() {
        return new TerminatorToken(context.line, context.idx, context.next());
    }

    /**
     * Parsing logic for a pipe (|).
     *
     * @return The pipe command line parsed.
     * @throws CommandLineSyntaxException If there's a problem.
     */
    private Token parsePipe() throws CommandLineSyntaxException {
        final int startIdx = context.idx();

        context.next(); // consume the pipe
        context.skipWhiteSpace();

        if (context.isEnd()) {
            throw new CommandLineSyntaxException("Expected a command following '|'", context.idx, context.line);
        }

        return new PipeToken(context.line, startIdx, context.remainder());
    }

    /**
     * Parses a command line output reidrection token.
     *
     * @return The parsed token.
     * @throws CommandLineSyntaxException
     */
    private Token parseOutputRedirection() throws CommandLineSyntaxException {
        final int startIdx = context.idx();

        int leftFd = 1;
        boolean isAppend = false;

        // Check for the beginning of a X> or X>&Y. Our caller has ensured that we will have a '>' character
        // following this number.
        if (Character.isDigit(context.peek())) {
            leftFd = context.next() - '0';
        }

        // Skip the current '>' character.
        context.next();

        // If we hit another '>', then we have a '>>'. Alternatively if we hit a '&', then we are performing a
        // file descriptor duplication.
        if (!context.isEnd() && context.peek() == '>') {
            context.next();
            isAppend = true;
        } else if (!context.isEnd() && context.peek() == '&') {
            context.next();
            return new FileDescriptorDupToken(context.line, startIdx, leftFd, parseDescriptorNumber());
        }

        // At this point we have parsed one of the following:
        //   X>
        //   X>>
        //   >
        //   >>
        // Now, if there is a + then the redirection is for a session target.
        if (!context.isEnd() && context.peek() == '+') {
            context.next();
            int sessionId = -1;
            if (!context.isEnd() && Character.isDigit(context.peek())) {
                sessionId = parseDescriptorNumber();
            }
            return new SessionRedirectToken(context.line, startIdx, sessionId, isAppend);
        }

        // Inelegant solution to double quotes in the filename. Since we want normal variable expansion
        // and stuff to be processed, use the normal token processing logic to grab the next token. However
        // we do NOT want double quotes retained, otherwise you end up with filenames with double quotes
        // in them.  So, temporarily turn the feature off, then get the token, and re-enable it.
        Token filenameToken;
        boolean wasRetainDoubleQuotes = retainDoubleQuotes;
        try {
            retainDoubleQuotes = false;
            filenameToken = next();
        } finally {
            retainDoubleQuotes = wasRetainDoubleQuotes;
        }

        // Error if we hit EOF (null) or if we didn't see a string.
        if (!(filenameToken instanceof StringToken)) {
            throw new CommandLineSyntaxException("Expected a target filename following redirection",
                    context.idx, context.line);
        }

        // At this point we have consumed one of the following
        //    [n]>
        //    [n]>>
        // so we are finished!
        return new RedirectOutToken(context.line, startIdx, leftFd, filenameToken.toString(), isAppend);
    }

    /**
     * Test if a character is the terminator
     *
     * @param ch The character
     * @return true if it is the terminator
     */
    private boolean isTerminator(char ch) {
        return terminator > 0 && ch == terminator;
    }

    /**
     * Helper function used to consume a generic "string" from the command line.
     *
     * @return The parsed string, or null if EOF is reached.
     */
    private StringToken parseString(boolean retainDoubleQuotes) throws CommandLineSyntaxException {
        final StringBuilder str = new StringBuilder();

        context.skipWhiteSpace();
        final int startIdx = context.idx();

        // This loop is written such that we recognize input like hello' how are you'", doing?" as a
        // single string, however each portion that is contained in quotes will do variable expansion
        // (or not) as appropriate.
        while (!context.isEnd()) {
            final char ch = context.peek();

            // Special case: jsqsh commands start with a backslash, so under normal escape processing
            // rules '\echo' would be treated like 'echo', however this throws off the jsqsh command
            // lookup logic, so if retainInitialEscape is enabled, and this is the first token that
            // has been encountered, then keep the leading slash.
            if (ch == '\\'
                    && retainInitialEscape
                    && tokenCount == 0  // Only on the first token
                    && context.idx() == startIdx) {  // Only on the first character
                str.append(context.next());
            } else if (ch == '\'') {
                doSingleQuotedString(str);
            } else if (ch == '"') {
                doDoubleQuotedString(str, retainDoubleQuotes);
            } else if (isStringCharacter(ch)) {
                doUnquotedString(str);
            } else {
                break;
            }
        }

        return new StringToken(context.line, startIdx, str.toString());
    }

    private void doUnquotedString(StringBuilder sb) throws CommandLineSyntaxException {
        doWhileAndExpand(sb, this::isStringCharacter, true);
    }

    private boolean isStringCharacter(char ch) {
        return !(Character.isWhitespace(ch)
                || isTerminator(ch)
                || ch == '\''
                || ch == '"'
                || ch == '|'
                || ch == '<'
                || ch == '>'
                || (terminator > 0 && ch == ((char) terminator))
                || (expandBackTicks && ch == '`')
                || ch == '&');
    }

    private void doSingleQuotedString(StringBuilder sb) throws CommandLineSyntaxException {
        final int startIdx = context.idx();

        context.next(); // Consume leading quote

        doWhile(sb, ch -> ch != '\'', false);
        if (context.isEnd()) {
            throw new CommandLineSyntaxException("Closing single quote not found", startIdx, context.line);
        }

        context.next(); // Consume trailing quote
    }

    private void doDoubleQuotedString(StringBuilder sb, boolean retainDoubleQuotes) throws CommandLineSyntaxException {
        final int startIdx = context.idx();

        context.next(); // Consume leading quote

        if (retainDoubleQuotes) {
            sb.append('"');
        }

        doWhileAndExpand(sb, ch -> ch != '"', true);

        if (context.isEnd()) {
            throw new CommandLineSyntaxException("Closing double quote not found", startIdx, context.line);
        }

        if (retainDoubleQuotes) {
            sb.append('"');
        }

        context.next(); // Consume trailing quote
    }

    private void doWhile(StringBuilder sb, Predicate<Character> isConsumable, boolean doEscape)
            throws CommandLineSyntaxException{
        while (!context.isEnd()) {
            char ch = context.peek();
            if (ch == '\\' && doEscape) {
                doEscape(sb);
            } else if (isConsumable.test(ch)) {
                sb.append(ch);
                context.next();
            } else {
                break;
            }
        }
    }

    private void doEscape(StringBuilder sb) throws CommandLineSyntaxException {
        context.next(); // Consume the escape
        if (context.isEnd()) {
            throw new CommandLineSyntaxException("Expected character following '\\'", context.idx, context.line);
        }

        sb.append(context.next());
    }

    private Token doBackTick() throws CommandLineSyntaxException {
        final int startIdx = context.idx();
        context.next(); // Skip leading backtick
        context.skipWhiteSpace(); // As well as leading whitespace

        final StringBuilder command = new StringBuilder();
        boolean done = false;
        while (!done && !context.isEnd()) {
            char ch = context.peek();
            switch (context.peek()) {
                case '\'':
                    command.append('\'');
                    doSingleQuotedString(command);
                    command.append('\'');
                    break;
                case '\\':
                    doEscape(command);
                    break;
                case '"':
                    // Expand variables, but keep the quotes intact.
                    doDoubleQuotedString(command, true);
                    break;
                case '`':
                    context.next(); // Consume closing quote
                    done = true;
                    break;
                default:
                    if (Character.isWhitespace(ch) || !isStringCharacter(ch)) {
                        command.append(context.next());
                    } else {
                        doUnquotedString(command);
                    }
                    break;
            }
        }

        if (!done) {
            throw new CommandLineSyntaxException("Missing closing back-tick (`)", startIdx, context.line);
        }

        try {
            return getShellOutput(command.toString());
        } catch (ShellException e) {
            throw new CommandLineSyntaxException("Failed to execute: " + command.toString() + ": " + e.getMessage(),
                    startIdx, context.line);
        }
    }

    private Token getShellOutput(String command) throws ShellException {
        final Process shell = shellManager.readShell(command, false);

        final StringBuilder field = new StringBuilder();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(shell.getInputStream()))) {
            int ch = in.read();
            while (ch != -1) {
                // Skip leading IFS characters.
                while (ch != -1 && fieldSeparater.indexOf((char) ch) >= 0) {
                    ch = in.read();
                }

                // Suck in non-IFS characters.
                field.setLength(0);
                while (ch != -1 && fieldSeparater.indexOf(ch) < 0) {
                    field.append((char) ch);
                    ch = in.read();
                }

                if (field.length() > 0) {
                    // Special case: if this is the last token in the stream, and it had a newline, then
                    // strip it off
                    if (ch == -1) {
                        int end = field.length() -1;
                        while (end >= 0 && field.charAt(end) == '\n' || field.charAt(end) == '\r') {
                            --end;
                        }
                        field.setLength(end + 1);
                    }

                    if (field.length() > 0) {
                        pendingTokens.add(new StringToken("", 0, field.toString()));
                    }
                }

                ch = in.read();
            }
        } catch (IOException e) {
            // IGNORED - Probably a dead pipe
        }

        return pendingTokens.isEmpty() ? null : pendingTokens.remove();
    }

    private void doWhileAndExpand(StringBuilder sb, Predicate<Character> isConsumable, boolean doEscape)
            throws CommandLineSyntaxException {
        if (expander == null) {
            doWhile(sb, isConsumable,doEscape);
        } else {
            StringBuilder tmp = new StringBuilder();
            doWhile(tmp, isConsumable, doEscape);
            sb.append(expander.expand(tmp.toString()));
        }
    }

    /**
     * Helper function used to consume a number from the command line at the current parsing point. An exception will be
     * thrown if there is not a number to be found.
     *
     * @return The parsed number
     * @throw CommandLineSyntaxException if there is not a number to be parsed at the current location.
     */
    private int parseDescriptorNumber() throws CommandLineSyntaxException {
        StringBuilder number = new StringBuilder();

        final int startIdx = context.idx();

        // Allow, but skip, leading white space in the number.
        context.skipWhiteSpace();

        doWhile(number, Character::isDigit, false);
        if (number.length() == 0) {
            throw new CommandLineSyntaxException("Expected a number following file descriptor duplication token '>&'",
                    startIdx, context.line);
        }
        return Integer.parseInt(number.toString());
    }

    @Override
    public String toString() {
        return context.toString();
    }

    private class LineContext {
        private final String line;
        private final int len;
        private int idx;

        public LineContext(String line) {
            this.line = line;
            this.len = line.length();
            this.idx = 0;
        }

        public boolean hasAtLeast(int chars) {
            return (len - (idx + 1)) >= chars;
        }

        public int idx() {
            return idx;
        }

        public char next() {
            return line.charAt(idx++);
        }

        public char peek() {
            return line.charAt(idx);
        }

        public char peek(int ahead) {
            return line.charAt(idx + ahead);
        }

        public boolean isEnd() {
            return idx >= line.length();
        }

        public void skipWhiteSpace() {
            while (idx < len && Character.isWhitespace(line.charAt(idx))) {
                ++idx;
            }
        }

        public String remainder() {
            String remainder = line.substring(idx);
            idx = len;
            return remainder;
        }

        @Override
        public String toString() {
            if (idx >= len) {
                return line + "^";
            }
            if (idx == 0) {
                return "^" + line;
            }
            return line.substring(0, idx) + "^" + line.substring(idx);
        }
    }

    /**
     * Builder to construct a tokenizer.
     */
    public static class Builder {
        private StringExpander expander = StringExpander.ENVIRONMENT_EXPANDER;
        private ShellManager shellManager = ShellManager.getInstance();
        private boolean expandBackTicks = true;
        private boolean retainDoubleQuotes = false;
        private boolean retainInitialEscape = true;
        private int terminator = -1;
        private String fieldSeparator = WHITESPACE;
        private final String line;

        public Builder(String line) {
            this.line = line;
        }

        /**
         * Sets an expander used to expand variables present in the string. If not specified the
         * {@link StringExpander#ENVIRONMENT_EXPANDER} will be used.
         *
         * @param expander the string expander to use
         * @return this builder
         */
        public Builder setExpander(StringExpander expander) {
            this.expander = expander;
            return this;
        }

        /**
         * Controls backtick expansion, which is enabled, by default. If disabled, backticks will be
         * treated as normal characters so, for example, {@code `echo} would be treated a single string token.
         *
         * @param expandBackTicks true to enable backtick expansion
         * @return this builder
         */
        public Builder setExpandBackTicks(boolean expandBackTicks) {
            this.expandBackTicks = expandBackTicks;
            return this;
        }

        /**
         * The {@code ShellManager} to use to execute commands contained in backticks. The default {@code ShellManager}
         * uses default shell commands ({@code /bin/sh -c} on UNIX and {@code cmd.exe /c} on Windows). It is
         * recommended to use the {@code ShellManager} that is controlled by the {@link Session}, as that one allows
         * the user to provide alternative values via the {@code $shell} variable.
         *
         * @param shellManager the {@code ShellManager} to use
         * @return this builder
         */
        public Builder setShellManager(ShellManager shellManager) {
            this.shellManager = shellManager;
            return this;
        }

        /**
         * Set of characters that separate words during the processing of backtick commands (similar to {@code $IFS}
         * in normal shell processing). This may contain zero or more characters, each of which will be considered
         * as a split point in the output of a backtick command. For example:
         * <pre>
         *   \echo `echo hello:how:are:you`
         * </pre>
         * will be returned as a stream of tokens:
         * <pre>
         *   \echo
         *   hello:how:are:you
         * </pre>
         * whereas:
         * <pre>
         *   \set ifs=:
         *   \echo `echo hello:how:are:you`
         * </pre>
         * will be returned as:
         * <pre>
         *   \echo
         *   hello
         *   how
         *   are
         *   you
         * </pre>
         *
         * The default value is " \t\n\r".
         *
         * @param fieldSeparator set of characters to use as field separators
         * @return this builder
         */
        public Builder setFieldSeparator(String fieldSeparator) {
            this.fieldSeparator = fieldSeparator;
            return this;
        }

        /**
         * Controls whether double-quoted strings retain the surrounding double quotes. For example, when disabled,
         * {@code "hello there"} is returned as {@code hello there}, whereas with it enabled {@code "hello there"} is
         * returned. This is primarily used by commands in which the presence of the quotes indicates a SQL
         * quoted identifier. For example, {@code \show "The Table"}, the double quotes are retained and passed
         * along to the JDBC driver so that it knows that the user provided a properly quoted identifier.
         * The default value is {@code false}.
         *
         * @param retainDoubleQuotes true to enable retaining double quotes
         * @return this builder
         */
        public Builder setRetainDoubleQuotes(boolean retainDoubleQuotes) {
            this.retainDoubleQuotes = retainDoubleQuotes;
            return this;
        }

        /**
         * Controls if a leading escape character ("{@code \}") in the first string token is retrained. This property
         * is used to ensure that {@code \the\ command} is parsed as "{@code \the command}". That is, the leading
         * escape character is retained (so that jsqsh can recognize it is a command), yet the second escape is
         * handled under normal processing rules. The default is {@code true}.
         *
         * @param retainInitialEscape true to enable retaining the initial escape
         * @return this builder
         */
        public Builder setRetainInitialEscape(boolean retainInitialEscape) {
            this.retainInitialEscape = retainInitialEscape;
            return this;
        }

        /**
         * Sets the terminator character, the value {@code -1} (the default), disables recognition of a terminator
         * character which will be returned as a discrete {@link TerminatorToken}.
         *
         * @param terminator the character to recognize as terminator
         * @return this builder
         */
        public Builder setTerminator(int terminator) {
            this.terminator = terminator;
            return this;
        }

        public Tokenizer build() {
            return new Tokenizer(line, expander, expandBackTicks, fieldSeparator, shellManager,
                    retainDoubleQuotes, retainInitialEscape, terminator);
        }
    }
}
