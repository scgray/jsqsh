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
package org.sqsh;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter
    extends Formatter {
    
    private String lineSep = System.getProperty("line.separator");

    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format (LogRecord record) {

        StringBuffer sb = new StringBuffer();
        
        // sb.append(record.getLevel().getLocalizedName());
        sb.append("[");
        if (record.getSourceClassName() != null) {
            
            sb.append(record.getSourceClassName());
        }
        else {
            
            sb.append(record.getLoggerName());
        }
        sb.append(']');
        
        if (record.getSourceMethodName() != null) {
            
            sb.append('[');
            sb.append(record.getSourceMethodName());
            sb.append(']');
        }
        
        sb.append(": ");
        sb.append(formatMessage(record));
        if (record.getThrown() != null) {
            
            sb.append(lineSep);
            try {
                
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            }
            catch (Exception ex) {
                
            }
        }
        sb.append(lineSep);
        return sb.toString();
    }
}
