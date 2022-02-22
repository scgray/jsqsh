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
package org.sqsh.renderers;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;
import org.sqsh.util.StringUtils;

/**
 * Outputs results as CSV (comma separated values). This class attempts to adhere to <a
 * href="http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm"> http://www.creativyst.com/Doc/Articles/CSV/CSV01
 * .htm</a>.
 */
public class CSVRenderer extends Renderer {
    private static String delimiter = ",";
    private static String quote = "\"";
    private static String quoteEsc = "";
    private static String nullStr = "";

    private final StringBuilder line = new StringBuilder();

    public CSVRenderer(Session session, RendererManager renderMan) {
        super(session, renderMan);
    }

    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the delimiter string for this and all future instances.
     *
     * @param delimiter The delimiter string to use.
     */
    public void setDelimiter(String delimiter) {
        CSVRenderer.delimiter = StringUtils.expandEscapes(
                delimiter, StringUtils.BadEscapeHandling.LEAVE_ESCAPE_CHARACTER);
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        CSVRenderer.quote = quote;
    }

    public String getQuoteEsc() {
        return quoteEsc;
    }

    public void setQuoteEsc(String quoteEsc) {
        CSVRenderer.quoteEsc = quoteEsc;
    }

    public String getNull() {
        return nullStr;
    }

    public void setNull(String nullStr) {
        CSVRenderer.nullStr = nullStr;
    }

    public void header(ColumnDescription[] columns) {
        super.header(columns);
        if (manager.isShowHeaders()) {
            String[] row = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                row[i] = columns[i].getName();
                if (row[i] == null) {
                    row[i] = "";
                }
            }
            row(row);
        }
    }

    @Override
    public boolean flush() {
        return true;
    }

    private boolean needsQuoting(String str) {
        final int sz = str.length();

        if (sz == 0) {
            // If our NULL representation is a missing value, then force
            // quoting to differentiate the empty string from NULL
            return nullStr == null || nullStr.length() == 0;
        }

        if (Character.isWhitespace(str.charAt(0)) || Character.isWhitespace(str.charAt(sz - 1))) {
            return true;
        }

        final char quoteChar = quote.charAt(0);
        final char delChar = delimiter.charAt(0);

        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);
            if (ch == quoteChar) {
                if (quote.length() == 1 || str.regionMatches(i, quote, 0, quote.length())) {
                    return true;
                }
            } else if (ch == delChar) {
                if (delimiter.length() == 1 || str.regionMatches(i, delimiter, 0, delimiter.length())) {
                    return true;
                }
            } else if (ch == '\n') {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean row(String[] row) {
        line.setLength(0);
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                line.append(delimiter);
            }

            String field = row[i];
            if (!isNull(field)) {
                if (!needsQuoting(field)) {
                    line.append(field);
                } else {
                    line.append(quote);
                    final char quoteChar = quote.charAt(0);
                    for (int j = 0; j < field.length(); j++) {
                        char ch = field.charAt(j);
                        if (ch == quoteChar && (quote.length() == 1 || field.regionMatches(j, quote, 0,
                                quote.length()))) {
                            if (quoteEsc == null || quoteEsc.length() == 0) {
                                line.append(quote).append(quote);
                            } else {
                                line.append(quoteEsc);
                            }
                        }
                        line.append(ch);
                    }
                    line.append(quote);
                }
            } else {
                line.append(nullStr);
            }
        }
        session.out.println(line);
        return !session.out.checkError();
    }

    @Override
    public void footer(String footer) {
        // This style will never display footer information.
    }
}
