/*
 * Copyright 2007-2012 Scott C. Gray
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

import java.sql.Timestamp;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An alternative to Java's {@link SimpleDateFormat} object that modifies
 * the meaning of the "S" field to indicate number of digits of the fractional
 * portion of the time instead of strictly milliseconds. 
 */
public class TimestampFormatter {
    
    private SimpleDateFormat format;
    private StringBuffer sb;
    private FieldPosition fp;
    private int nFractionalDigits;
    
    public TimestampFormatter (String format) {
        
        final int len = format.length();
        int idx = 0;
        boolean inQuotes = false;
        int startIdx = -1;
        int endIdx = -1;
        
        /*
         * Search through the format for a span of "S" characters that are not
         * contained within quotes...
         */
        while (idx < len) {
            
            char ch = format.charAt(idx);
            if (ch == '\'') {
                
                ++idx;
                if (idx < len && format.charAt(idx) == '\'') {
                    
                    ++idx;
                }
                else {
                    
                    inQuotes = !inQuotes;
                }
            }
            else if (ch == 'S') {
                
                if (startIdx == -1) {
                    
                    startIdx = idx;
                }
                ++idx;
            }
            else {
                
                if (startIdx != -1 && endIdx == -1) {
                    
                    endIdx = idx;
                }
                
                ++idx;
            }
        }
        
        if (startIdx != -1 && endIdx == -1) {
            
            endIdx = len;
        }
        
        if ((startIdx == -1  && endIdx == -1) || (endIdx - startIdx) == 3) {
            
            this.format = new SimpleDateFormat(format);
        }
        else {
            
            sb = new StringBuffer();
            sb.append(format, 0, startIdx);
            for (int i = startIdx; i < endIdx; i++) {
                
                sb.append('\u0000');
            }
            sb.append(format, endIdx, len);
            this.fp = new FieldPosition(0);
            this.nFractionalDigits = endIdx - startIdx;
            this.format = new SimpleDateFormat(sb.toString());
        }
    }
    
    public String format (Date date) {
        
        if (nFractionalDigits == 0) {
            
            return format.format(date);
        }
        
        sb.setLength(0);
        format.format(date, sb, fp);
        
        /*
         * Are we doing our own fractional digits?
         */
        if (nFractionalDigits > 0) {
            
            int idx = 0;
            while (sb.charAt(idx) != '\u0000') {
                
                ++idx;
            }
            
            int divisor = 100000000;
            int nanos = (date instanceof Timestamp) 
                    ? ((Timestamp) date).getNanos()
                    : (int) ((date.getTime() % 1000) * 1000000);
            
            for (int i = 0; i < nFractionalDigits; i++) {
                
                /*
                 * Do we have our marker byte for where to put our fractional digits?
                 */
                 int val = (nanos / divisor) % 10;
                 sb.setCharAt(idx++, (char) ('0' + val));
                 divisor /= 10;
            }
        }
        
        return sb.toString();
    }
}
