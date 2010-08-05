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

/**
 * The Unformatter doesn't perform any manipulation of the object to 
 * be formatter.
 */
public class Unformatter
    implements Formatter {
    
    private int maxWidth;
    
    public Unformatter (int maxWidth) {
        
        this.maxWidth = maxWidth;
    }

    public String format (Object value) {

        return value.toString();
    }

    public int getMaxWidth () {

        return maxWidth;
    }
}
