/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
package org.sqsh.format;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.sqsh.Formatter;

public class DateFormatter
    implements Formatter {
    
    private SimpleDateFormat format;
    private int maxWidth;
    
    public DateFormatter (String format, int maxWidth) {
        
        this.format = new SimpleDateFormat(format);
        this.maxWidth = maxWidth;
    }

    public String format (Object value) {
        
        return format.format(value);
    }

    public int getMaxWidth () {

        return maxWidth;
    }
}
