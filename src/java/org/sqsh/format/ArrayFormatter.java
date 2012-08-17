/*
 * Copyright (C) 2012 by Scott C. Gray
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

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.sqsh.ColumnDescription;
import org.sqsh.FormatError;
import org.sqsh.Formatter;
import org.sqsh.SQLRenderer;

/**
 * Attempts to format an ARRAY data type
 */
public class ArrayFormatter 
    implements Formatter {
    
    private SQLRenderer renderer;
    private Formatter   elementFormatter = null;
    private String      nullStr;
    
    /**
     * Creates an array formatter
     * @param renderer The renderer that is used to fetch formatting 
     *   information about how to render the elements of the array
     * @param nullValue The string to use if a value is null
     */
    public ArrayFormatter (SQLRenderer renderer, String nullValue) {
        this.renderer = renderer;
        this.nullStr  = nullValue;
    }

    @Override
    public int getMaxWidth() {

        return Integer.MAX_VALUE;
    }

    @Override
    public String format(Object value) {
        
        Array         array = (Array)value;
        StringBuilder sb    = new StringBuilder();
        int           idx   = 0;
        String        str   = null;

        try  {
            
            ResultSet rs = array.getResultSet();
            
            /*
             * Grab a formatter if we haven't grabbed one yet.
             */
            if (elementFormatter == null) {
                ResultSetMetaData meta = rs.getMetaData();
                ColumnDescription desc = renderer.getDescription(meta, 2);
                
                elementFormatter = desc.getFormatter();
            }
            
            sb.append('[');
            while (rs.next()) {
                
                Object element = rs.getObject(2);
                if (!rs.wasNull()) {
                    
                    str = elementFormatter.format(element);
                }
                else {
                    
                    str = nullStr;
                }
                
                if (idx > 0) {
                    
                    sb.append(", ");
                }
                sb.append(str);
                ++idx;
            }
            
            sb.append(']');
        }
        catch (Throwable e) {
            
            throw new FormatError("Failed to format array entry #"
                + idx + ": " + e.getMessage(), e);
        }
        
        return sb.toString();
    }
}
