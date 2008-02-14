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
