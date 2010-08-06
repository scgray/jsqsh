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
package org.sqsh.variables;

import org.sqsh.CannotSetValueError;
import org.sqsh.Variable;

/**
 * Represents a font specification.
 */
public class FontVariable
    extends Variable {
    
    private String fontName = "Monospaced";
    private int size = 10;

    @Override
    public String setValue (String value)
        throws CannotSetValueError {
        
        String []parts = value.split("-");
        boolean ok = true;
        
        if (parts.length != 2) {
            
            ok = false;
        }
        else {
            
            try {
                
                int tmpSize = Integer.parseInt(parts[1]);
                
                size = tmpSize;
                fontName = parts[0];
            }
            catch (Exception e) {
                
                ok = false;
            }
        }
        
        if (!ok) {
            
            throw new CannotSetValueError("Invalid font specification '"
                + value + "'. Fonts must be specified as Name-Size, such "
                + " as 'Monospace-10'");
        }
        
        return null;
    }

    @Override
    public String toString () {

        return fontName + "-" + size;
    }
    
    public String getFontName() {
        
        return fontName;
    }
    
    public int getFontSize() {
        
        return size;
    }
}
