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
 * General purpose utilities for working with time.
 */
public class TimeUtils {

    private TimeUtils() {
    }

    /**
     * Given a number of milliseconds, product a string of the format
     * <pre>
     * 7d6h6m.21s
     * </pre>
     *
     * @param millis Milliseconds in duration
     * @return A duration string.
     */
    public static String millisToDurationString(long millis) {
        StringBuilder sb = new StringBuilder();

        if (millis < 0) {
            sb.append('-');
            millis = -(millis);
        }

        long val = millis / 86400000L;
        if (val > 0L) {
            sb.append(val).append('d');
            millis %= 86400000L;
        }

        val = millis / 3600000L;
        if (sb.length() > 0 || val != 0L) {
            sb.append(val).append('h');
            millis %= 3600000L;
        }

        val = millis / 60000L;
        if (sb.length() > 0 || val != 0L) {
            sb.append(val).append('m');
            millis %= 60000L;
        }

        val = millis / 1000L;
        sb.append(val);

        millis %= 1000L;
        sb.append('.');
        append3(sb, millis);
        sb.append('s');

        return sb.toString();
    }

    /**
     * Given a number of milliseconds, product a string of the format dd:hh:mm:ss
     *
     * @param millis Milliseconds in duration
     * @return A timer string.
     */
    public static String millisToTimerString(long millis) {
        StringBuilder sb = new StringBuilder();
        if (millis < 0) {
            sb.append('-');
            millis = -(millis);
        }

        long val = millis / 86400000L;
        if (val > 0L) {
            append2(sb, val).append(':');
            millis %= 86400000L;
        }

        val = millis / 3600000L;
        if (sb.length() > 0 || val != 0L) {
            append2(sb, val).append(':');
            millis %= 3600000L;
        }

        val = millis / 60000L;
        append2(sb, val).append(':');
        if (val != 0L) {
            millis %= 60000L;
        }

        val = millis / 1000L;
        append2(sb, val);
        return sb.toString();
    }

    private static StringBuilder append2(StringBuilder sb, long val) {
        if (val < 10) {
            sb.append('0');
        }
        sb.append(val);
        return sb;
    }

    private static StringBuilder append3(StringBuilder sb, long val) {
        if (val < 10) {
            sb.append("00");
        } else if (val < 100) {
            sb.append('0');
        }
        sb.append(val);
        return sb;
    }

    public static void main(String[] argv) {
        System.out.println(millisToDurationString(Long.parseLong(argv[0])));
    }
}
