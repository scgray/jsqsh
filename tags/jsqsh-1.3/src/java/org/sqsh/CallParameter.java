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

import java.sql.Types;

/**
 * Object that is used to pass parameters to a callable or 
 * prepared statement.
 */
public class CallParameter {
    
    public static final int INPUT  = 1;
    public static final int OUTPUT = 2;
    public static final int INOUT  = 3;
    
    private int    idx;
    private int    dataType;
    private int    direction;
    private String value;
    
    public CallParameter (int idx, int dataType,
            int direction, String value) {
        
        this.idx = idx;
        this.dataType = dataType;
        this.direction = direction;
        this.value = value;
    }
    
    public CallParameter (int idx, String value) {
        
        this.idx = idx;
        this.value = value;
        this.direction = INPUT;
        this.dataType = Types.VARCHAR;
    }
    
    public int getIdx() {
        
        return idx;
    }
    
    public void setIdx(int idx) {
        
        this.idx = idx;
    }
    
    public int getType() {
        
        return dataType;
    }
    
    public void setType(int type) {
        
        this.dataType = type;
    }
    
    public int getDirection() {
        
        return direction;
    }
    
    public void setDirection(int direction) {
        
        this.direction = direction;
    }
    
    public String getValue() {
        
        return value;
    }
    
    public void setValue (String value) {
        
        this.value = value;
    }
}
