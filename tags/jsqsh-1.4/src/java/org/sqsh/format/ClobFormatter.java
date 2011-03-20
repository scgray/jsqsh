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

import java.io.Reader;
import java.sql.Clob;

import org.sqsh.Formatter;

public class ClobFormatter
    implements Formatter {
    
    public String format (Object value) {
        
        Clob clob = (Clob) value;
        StringBuilder sb = new StringBuilder();
        char []chars = new char[512];
        
        try {
            
            Reader in = clob.getCharacterStream();
            while (in.read(chars) >= 0) {
                
                sb.append(chars);
            }
            
            in.close();
        }
        catch (Exception e) {
            
            /* IGNORED */
        }
        
        return sb.toString();
    }

    public int getMaxWidth () {

        return Integer.MAX_VALUE;
    }
}
