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

import org.sqsh.Variable;

/**
 * Used to represent a basic string variable.
 */
public class StringVariable
    extends Variable {
    
    private String value;
    
    /**
     * Required for digester.
     */
    public StringVariable() {
        
    }
    
    /**
     * Creates a string variable.
     * 
     * @param name The name of the variable.
     * @param value The value that it is to be set to.
     */
    public StringVariable (String name, String value) {
        
        super(name);
        this.value = value; 
    }
    
    /**
     * Creates a exportable string variable.
     * 
     * @param name The name of the variable.
     * @param value The value that it is to be set to.
     * @param isExported True if the variable is to be exported.
     */
    public StringVariable (String name, String value, boolean isExported) {
        
        super(name, isExported);
        this.value = value; 
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String setValue(String value) {
        
        String oldValue = value;
        this.value = value;
        
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString () {

        return value;
    }
}
