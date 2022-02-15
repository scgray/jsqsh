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
package org.sqsh.format;

import org.sqsh.ColumnDescription;
import org.sqsh.FormatError;
import org.sqsh.Formatter;
import org.sqsh.SQLRenderer;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * Attempts to format an ARRAY data type
 */
public class ArrayFormatter implements Formatter {
    private final SQLRenderer renderer;
    private final String nullStr;

    private Formatter elementFormatter;

    /**
     * Creates an array formatter
     *
     * @param renderer The renderer that is used to fetch formatting information about how to render the
     *         elements of the array
     * @param nullValue The string to use if a value is null
     */
    public ArrayFormatter(SQLRenderer renderer, String nullValue) {
        this.renderer = renderer;
        this.nullStr = nullValue;
    }

    @Override
    public int getMaxWidth() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String format(Object value) {
        Array array = (Array) value;
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        String str;
        try {
            ResultSet rs = array.getResultSet();

            // Grab a formatter if we haven't grabbed one yet.
            if (elementFormatter == null) {
                ResultSetMetaData meta = rs.getMetaData();
                ColumnDescription desc = renderer.getDescription(meta, 2);
                elementFormatter = desc.getFormatter();
            }
            sb.append('[');
            while (rs.next()) {
                Object element = rs.getObject(2);
                if (!rs.wasNull()) {
                    str = elementFormatter.format(element);
                } else {
                    str = nullStr;
                }
                if (idx > 0) {
                    sb.append(", ");
                }
                sb.append(str);
                ++idx;
            }
            sb.append(']');
        } catch (Throwable e) {
            throw new FormatError("Failed to format array entry #" + idx + ": " + e.getMessage(), e);
        }
        return sb.toString();
    }
}
