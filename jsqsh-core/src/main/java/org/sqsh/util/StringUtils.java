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
package org.sqsh.util;

/**
 * Helper methods for diddling with strings
 */
public class StringUtils {

    /**
     * Tips on how to handle a bad escape sequence in {@link #expandEscapes(String, BadEscapeHandling)}.
     */
    public enum BadEscapeHandling {

        /**
         * In an un-recognized escape sequence, such as <code>\q</code> drop the escape character (leaving just the
         * <code>q</code>)
         */
        DROP_ESCAPE_CHARACTER,

        /**
         * In an un-recognized escape sequence leave the escape sequence untouched
         */
        LEAVE_ESCAPE_CHARACTER
    }

    public static String expandEscapes(final String str) {
        return expandEscapes(str, BadEscapeHandling.LEAVE_ESCAPE_CHARACTER);
    }

    /**
     * Given a string, process any escapes within it according to java based rules. This handles the following cases:
     * <ul>
     *     <li> <code>\\t</code> - Expaned to tab</li>
     *     <li> <code>\\b</code> - Expaned to backspace</li>
     *     <li> <code>\\n</code> - Expaned to newline</li>
     *     <li> <code>\\r</code> - Expaned to carriage return</li>
     *     <li> <code>\\f</code> - Expaned to formfeed</li>
     *     <li> <code>\\uHHHH</code> - Expand four digit hex sequnce representing a unicode codepoint</li>
     *     <li> <code>\\xHH</code> - Expand two digit hex sequnce representing a character</li>
     *     <li> <code>\\\\</code> - Expaned to backslash</li>
     * </ul>
     *
     * @param str The string to expand
     * @param badEscape How unrecognized escape sequences should be handled
     * @return The expanded version of the string
     */
    public static String expandEscapes(final String str, BadEscapeHandling badEscape) {
        int idx = str.indexOf('\\');
        if (idx < 0) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(str, 0, idx);

        final int len = str.length();
        while (idx < len) {
            char ch = str.charAt(idx++);
            if (ch == '\\' && idx < len) {
                ch = str.charAt(idx++);
                switch (ch) {
                    case 't':
                        sb.append('\t');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'u': // Unicode escape (\\uHHHH)
                        sb.append(Character.toChars(getCharEscapeValue("\\u", str, len, idx, 4)));
                        idx += 4;
                        break;
                    case 'x': // Hex byte (\\xHH)
                        sb.append((char) getCharEscapeValue("\\x", str, len, idx, 2));
                        idx += 2;
                        break;
                    default:
                        // Octal \\XX
                        if (ch >= '0' && ch <= '7') {
                            sb.append((char) getOctalValue(str, len, idx - 1));
                            idx += 2;
                        } else {
                            if (badEscape == BadEscapeHandling.LEAVE_ESCAPE_CHARACTER) {
                                sb.append('\\');
                            }
                            sb.append(ch);
                        }
                        break;
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }


    private static int getCharEscapeValue(final String escType, final String str, final int strLen, int idx,
            final int nDigits) {
        if ((strLen - idx) < nDigits) {
            throw new IllegalArgumentException("Invalid value in character escape " + escType);
        }
        int value = 0;
        for (int i = idx; i < (idx + nDigits); i++) {
            value <<= 4;
            char ch = str.charAt(i);
            if (ch >= '0' && ch <= '9') {
                value += ch - '0';
            } else if (ch >= 'a' && ch <= 'f') {
                value += (10 + (ch - 'a'));
            } else if (ch >= 'A' && ch <= 'F') {
                value += (10 + (ch - 'A'));
            } else {
                throw new IllegalArgumentException(
                        "Invalid value in character escape " + escType + str.substring(idx, idx + nDigits));
            }
        }
        return value;
    }

    private static int getOctalValue(final String str, final int strLen, final int idx) {
        if ((strLen - idx) < 3) {
            throw new IllegalArgumentException("Invalid value in octal character escape");
        }
        int value = 0;
        for (int i = idx; i < (idx + 3); i++) {
            value <<= 3;
            char ch = str.charAt(i);
            if (ch >= '0' && ch <= '7') {
                value += ch - '0';
            } else {
                throw new IllegalArgumentException(
                        "Invalid value in character escape \\" + str.substring(idx, idx + 3));
            }
        }
        return value;
    }
}
