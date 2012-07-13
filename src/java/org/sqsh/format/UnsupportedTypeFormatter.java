package org.sqsh.format;

import org.sqsh.Formatter;

/**
 * Formatter for displaying data types that jsqsh doesn't have direct support
 * for. This simply asks the data type to display itself as a string.
 */
public class UnsupportedTypeFormatter
    implements Formatter {

    @Override
    public int getMaxWidth() {

        return Integer.MAX_VALUE;
    }

    @Override
    public String format(Object value) {

        return value.toString();
    }
}
