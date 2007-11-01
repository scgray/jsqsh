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
package org.sqsh;

/**
 * A formatter is used to convert a data type (usually a base java type or
 * a java.lang.sql datatype) to a readable string.
 */
public interface Formatter {
    
    /**
     * Returns the maximum number of characters required to display a value
     * as a textual string.
     * 
     * @return The maximum number of characters required to display a value.
     */
    int getMaxWidth();

    /**
     * Formats a value into a string.
     * 
     * @param value The value to be formatted.
     * @return The formatted value.
     */
    String format(Object value);
}
