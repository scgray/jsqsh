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

import java.util.Stack;

/**
 * The SimpleKeywordTokenizer attempts to iterate through potential SQL keywords, skipping over quoted strings,
 * punctuation (except for semicolon), comments, and variable names so that hopefully only keywords, or objects names
 * that couldn't be confused with keywords are returned.
 */
public class SimpleKeywordTokenizer {

    /**
     * Token value returned when a string literal is identified.
     */
    public static final String STRING_LITERAL_TOKEN = "___STRING_LITERAL";

    /**
     * Token value returned when a quoted identifier is identified.
     */
    public static final String QUOTED_IDENTIFIER_TOKEN = "___QUOTED_IDENTIFIER";

    /**
     * Value returned with a variable (e.g. {@code $foo} or {@code @hello}) is identified.
     */
    public static final String VARIABLE_TOKEN = "___VARIABLE";

    protected final CharSequence sql;
    protected final char terminator;
    protected final boolean toUpperCase;
    protected final int len;

    protected int idx;

    private final Stack<String> tokens = new Stack<>();

    public SimpleKeywordTokenizer(CharSequence sql, char terminator, boolean toUpperCase) {
        this.sql = sql;
        this.len = sql.length();
        this.idx = 0;
        this.terminator = terminator;
        this.toUpperCase = toUpperCase;
    }

    public SimpleKeywordTokenizer(CharSequence sql, char terminator) {
        this(sql, terminator, true);
    }

    /**
     * Returns the next token.
     *
     * @return The next available token or null if there is nothing else to process. All tokens are returned in upper
     *         case.
     */
    public String next() {
        if (!tokens.isEmpty()) {
            return tokens.pop();
        }

        return nextToken();
    }

    /**
     * Allows the user to un-read a previously read token.
     *
     * @param token The token to unget.
     */
    public void unget(String token) {
        if (token != null) {
            tokens.push(token);
        }
    }

    /**
     * Peek ahead one token
     *
     * @return The next token or null if there is no next token
     */
    public String peek() {
        String t = next();
        if (t != null) {
            tokens.push(t);
        }
        return t;
    }

    /**
     * Skip a sequence of keywords if they are present, otherwise do not advance at all.
     *
     * @param keywords sequence of keywords to skip
     * @return {@code true} if the keywords were present
     */
    public boolean skip(String...keywords) {
        int idx = 0;
        while (idx < keywords.length) {
            String token = next();
            if (!keywords[idx].equals(token)) {
                unget(token);
                for (--idx; idx >= 0; --idx) {
                    unget(keywords[idx]);
                }
                return false;
            }
            ++idx;
        }

        return true;
    }

    /**
     * Return {@code true} if the next set of characters matches the string {@code toMatch}, without
     * advancing the current position.
     */
    protected boolean matchesAhead(String toMatch) {
        final int toMatchLen = toMatch.length();
        if (idx + toMatchLen > len) {
            return false;
        }
        for (int i = 0; i < toMatchLen; i++) {
            if (sql.charAt(idx + i) != toMatch.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Skip any whitespace. Whitespace are considered normal whitespace characters well as comments.
     *
     * @return false if we are at EOF
     */
    protected boolean skipWhitespaceAndComments() {
        while (idx < len) {
            char ch = sql.charAt(idx);
            if (Character.isWhitespace(ch)) {
                ++idx;
            } else if (matchesAhead("--")) {
                idx = SQLParseUtil.skipDashComment(sql, len, idx);
            } else if (matchesAhead("/*")) {
                idx = SQLParseUtil.skipComment(sql, len, idx);
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Skips through sequences of characters which don't contain keywords or in which the terminator
     * character could not be a valid jsqsh terminator (for example, inside of comments or quoted strings).
     */
    protected String nextToken() {
        if (!skipWhitespaceAndComments()) {
            return null;
        }

        char ch = sql.charAt(idx);

        // We have a terminator character!! Yay!
        if (ch == terminator) {
            ++idx;
            return String.valueOf(ch);
        }

        // Look for things that cannot be keywords or identifiers.
        String nonKeywordOrIdentifier = nextNonKeywordOrIdentifier();
        if (nonKeywordOrIdentifier != null) {
            return nonKeywordOrIdentifier;
        }

        // If the current character cannot be part of a literal or identifier, then just return it all by itself.
        if (!isIdentifierLeading(ch)) {
            ++idx;
            return String.valueOf(ch);
        }

        // We are starting a keyword or identifier...
        StringBuilder sb = new StringBuilder();
        sb.append(toUpperCase ? Character.toUpperCase(ch) : ch);
        ++idx;

        while (idx < len) {
            ch = sql.charAt(idx);
            if (!isIdentiferTrailing(ch)) {
                return sb.toString();
            }
            sb.append(toUpperCase ? Character.toUpperCase(ch) : ch);
            ++idx;
        }

        return sb.toString();
    }

    /**
     * Processes anything that isn't a keyword or identifier. This includes string literals, quoted identifiers,
     * variables, etc. Specifically, it attempts to skip things that are capable of containing the terminator but
     * in which the character cannot be treated as a terminator. If {@code null} is returned, then no non-keyword
     * or identifier are present, otherwise a place-holder for the type of token is returned (e.g.
     * {@link #STRING_LITERAL_TOKEN}.
     *
     * @return a string indicating the type of quoted string found or {@code null} if no quoted string is present
     */
    protected String nextNonKeywordOrIdentifier() {
        if (idx >= len) {
            return null;
        }

        // For things like string literals, we know the terminator cannot appear inside of them,
        // so the contents are not important, but we still return a token to recognize that there
        // is something there (in case the state of the parsing depends on knowing this fact).
        switch (sql.charAt(idx)) {
            case '\'':
                idx = SQLParseUtil.skipQuotedString(sql, len, idx);
                return STRING_LITERAL_TOKEN;
            case '"':
                idx = SQLParseUtil.skipQuotedString(sql, len, idx);
                return QUOTED_IDENTIFIER_TOKEN;
            case '@':
                idx = SQLParseUtil.skipVariable(sql, len, idx);
                return VARIABLE_TOKEN;
            case '[':
                idx = SQLParseUtil.skipBrackets(sql, len, idx);
                return QUOTED_IDENTIFIER_TOKEN;
            default:
                break;
        }

        return null;
    }

    protected boolean isIdentifierLeading(char ch) {
        return Character.isLetter(ch);
    }

    protected boolean isIdentiferTrailing(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }
}
