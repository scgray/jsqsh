package org.sqsh.util;

/**
 * General purpose utilities for working with time.
 */
public class TimeUtils {
    
    /**
     * Given a number of milliseconds, product a string of the format
     * <pre>
     * 7d6h6m.21s
     * </pre>
     * 
     * @param millis Milliseconds in duration
     * @return A duration string.
     */
    public static String millisToDurationString (long millis) {
        
        StringBuilder sb = new StringBuilder ();
        
        if (millis < 0) {
            
            sb.append('-');
            millis = -(millis);
        }
        
        long val = millis / 86400000L;
        if (val > 0L) {
            
            sb.append (val).append ('d');
            millis %= 86400000L;
        }

        val = millis / 3600000L;
        if (sb.length () > 0 || val != 0L) {
            
            sb.append (val).append ('h');
            millis %= 3600000L;
        }

        val = millis / 60000L;
        if (sb.length () > 0 || val != 0L) {
            
            sb.append (val).append ('m');
            millis %= 60000L;
        }

        val = millis / 1000L;
        sb.append (val);

        val %= 1000L;
        sb.append ('.').append (val / 10L).append ('s');

        return sb.toString ();
    }
}
