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

import java.util.Iterator;

/**
 * This class is used by all renderers to describe the columns that are 
 * to be rendered.
 */
public class ColumnDescription {
    
    /**
     * Represents how the contents of the column will be aligned.
     */
    public enum Alignment { LEFT, RIGHT };
    
    /**
     * Used to indicate how the column should handle values that are
     * wider than the display width.
     */
    public enum OverflowBehavior { TRUNCATE, WRAP };
    
    /**
     * Represents a basic datatype for the column.
     */
    public enum Type { NUMBER, STRING };
    
    /**
     * Name of the column
     */
    private String name;
    
    /**
     * Display alignment of the column
     */
    private Alignment alignment = Alignment.LEFT;
    
    /**
     * The basic type of the column.
     */
    private Type type = Type.STRING;
    
    /**
     * The display width of the column.
     */
    private int width;
    
    /**
     * Indicates how the column should handle values that are wider than
     * its display width.
     */
    private OverflowBehavior overflowBehavior = OverflowBehavior.WRAP;
    
    /**
     * This may or may not actually be used, but is here for a convenience
     * to carry around the object that is used to turn a raw value (e.g.
     * a Double) to a formatted value.
     */
    private Formatter formatter = null;
    
    /**
     * Creates a column descriptor that will be left-aligned and will
     * wrap its output as necessary.
     * 
     * @param name The name of the column.
     * @param width The display width of the column.
     */
    public ColumnDescription (String name, int width) {
        
        this.name = name;
        this.width = width;
    }
    
    /**
     * Creates a column.
     * 
     * @param name The name of the column
     * @param width The display width of the column 
     * @param alignment The alignment of the column
     * @param overflowBehavior How the column should deal with 
     *   data that overflows the width.
     */
    public ColumnDescription (String name, int width, 
            Alignment alignment, OverflowBehavior overflowBehavior) {
        
        this.name = name;
        this.width = width;
        this.alignment = alignment;
        this.overflowBehavior = overflowBehavior;
    }
    
    /**
     * @return the formatter
     */
    public Formatter getFormatter () {
    
        return formatter;
    }
    
    /**
     * @param formatter the formatter to set
     */
    public void setFormatter (Formatter formatter) {
    
        this.formatter = formatter;
    }

    /**
     * @return the alignment
     */
    public Alignment getAlignment () {
    
        return alignment;
    }

    /**
     * @param alignment the alignment to set
     */
    public void setAlignment (Alignment alignment) {
    
        this.alignment = alignment;
    }
    
    /**
     * @return the name
     */
    public String getName () {
    
        return name;
    }
    
    /**
     * @param name the name to set
     */
    public void setName (String name) {
    
        this.name = name;
    }
    
    /**
     * @return the overflowBehavior
     */
    public OverflowBehavior getOverflowBehavior () {
    
        return overflowBehavior;
    }
    
    /**
     * @param overflowBehavior the overflowBehavior to set
     */
    public void setOverflowBehavior (OverflowBehavior overflowBehavior) {
    
        this.overflowBehavior = overflowBehavior;
    }
    
    /**
     * Sets the basic type of the column.
     * 
     * @param type The type.
     */
    public void setType (Type type) {
        
        this.type = type;
    }
    
    /**
     * Returns the basic type of the column.
     */
    public Type getType() {
        
        return type;
    }
    
    /**
     * @return the width
     */
    public int getWidth () {
    
        return width;
    }
    
    /**
     * @param width the width to set
     */
    public void setWidth (int width) {
    
        this.width = width;
    }
}
