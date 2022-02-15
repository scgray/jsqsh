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

import org.sqsh.Formatter;

/**
 * Formats a byte or a byte array.
 */
public class ByteFormatter implements Formatter {

    private static final String[] HEX_DIGITS =
            {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    private final byte[] b = new byte[1];
    private final int maxBytes;
    private final boolean useStringFormat;

    public ByteFormatter(int maxBytes, boolean useStringFormat) {
        this.maxBytes = maxBytes;
        this.useStringFormat = useStringFormat;
    }

    public ByteFormatter(int maxBytes) {
        this(maxBytes, false);
    }

    public String format(Object value) {
        return format(value, -1);
    }

    public String format(Object value, int len) {
        byte[] bytes;
        if (value instanceof Byte) {
            b[0] = ((Byte) value);
            bytes = b;
        } else {
            bytes = ((byte[]) value);
        }
        StringBuilder sb = new StringBuilder(2 + (bytes.length * 2));
        byte ch;
        if (useStringFormat) sb.append("X'");
        else sb.append("0x");
        if (len < 0) {
            len = bytes.length;
        }
        for (int i = 0; i < len; i++) {
            ch = (byte) (bytes[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            sb.append(HEX_DIGITS[ch]);
            ch = (byte) (bytes[i] & 0x0F);
            sb.append(HEX_DIGITS[ch]);
        }
        if (useStringFormat) sb.append("'");
        return sb.toString();
    }

    public int getMaxWidth() {
        return 2 + (maxBytes * 2);
    }
}
