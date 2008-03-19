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

import org.sqsh.Formatter;

public class BitFormatter
    implements Formatter {

    public String format (Object value) {

        if (value instanceof Number) {
            
            if (((Number) value).intValue() == 0) {
                
                return "0";
            }
        }
        else if (value instanceof Boolean) {
            
            if (((Boolean) value).booleanValue()) {
                
                return "1";
            }
            
            return "0";
        }
        
        return "1";
    }

    public int getMaxWidth () {
        
        return 1;
    }
}
