/*
 * Copyright 2007-2012 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqsh;

/**
 * This class is used by all renderers to describe the columns that are 
 * to be rendered.
 */
public class ColumnDescription {
    
    /**
     * Represents how the contents of the column will be aligned.
     */
    public static enum Alignment { LEFT, RIGHT };
    
    /**
     * Used to indicate how the column should handle values that are
     * wider than the display width.
     */
    public static enum OverflowBehavior { TRUNCATE, WRAP };
    
    /**
     * Represents a basic datatype for the column.
     */
    public static enum Type { NUMBER, STRING };
    
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
     * This will not be available for all types of data (really only
     * from columns that come from a SQL result set), but indicates 
     * what the original datatype was.
     */
    private int nativeType = -1;
    
    /**
     * The display width of the column.
     */
    private int width;
    
    /**
     * Indicates whether or not the column "resists" being resized (used for
     * "perfect" display style). If false, then attempts will be made to keep
     * from resizing a column so small that it wraps.  There are no guarentees
     * that this will behonored, but it will try to be.
     */
    private boolean resizeable;
    
    /**
     * Indicates how the column should handle values that are wider than
     * its display width.
     */
    private OverflowBehavior overflowBehavior;
    
    /**
     * This may or may not actually be used, but is here for a convenience
     * to carry around the object that is used to turn a raw value (e.g.
     * a Double) to a formatted value.
     */
    private Formatter formatter = null;
    
    /**
     * Creates a column with no pre-defined with. The column will be left
     * aligned, will be allowed to resized.
     * 
     * @param name The name of the column
     */
    public ColumnDescription (String name) {
        
        this(name, -1, Alignment.LEFT, OverflowBehavior.WRAP, true);
    }
    
    /**
     * Creates a column with no pre-defined with. The column will be left
     * aligned and set to word wrap if it is resized.
     * 
     * @param name The name of the column
     * @param resizeable Whether or not it resists being resized
     */
    public ColumnDescription (String name, boolean resizeable) {
        
        this(name, -1, Alignment.LEFT, OverflowBehavior.WRAP, true);
    }
    
    /**
     * Creates a column descriptor that will be left-aligned and will
     * wrap its output as necessary.
     * 
     * @param name The name of the column.
     * @param width The display width of the column.
     */
    public ColumnDescription (String name, int width) {
        
        this(name, width, Alignment.LEFT, OverflowBehavior.WRAP, true);
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
        
        this(name, width, alignment, overflowBehavior, true);
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
            Alignment alignment, OverflowBehavior overflowBehavior,
            boolean resizeable) {
        
        this.name = name;
        this.width = width;
        this.alignment = alignment;
        this.overflowBehavior = overflowBehavior;
        this.resizeable = resizeable;
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
     * @return true if this column should "resist" being resized so that its
     *     contents wrap, false if it is OK to resize this column
     */
    public boolean isResizeable() {
        
        return resizeable;
    }

    /**
     * Sets whether or not this column "resists" being resized. 
     * 
     * @param resizeable true if the column should resist being resized, false
     *   if it is ok to resize the column and wrap.
     */
    public void setResizeable(boolean resizeable) {
        
        this.resizeable = resizeable;
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
        
        /*
         * Numbers will automatically resist resizing.
         */
        if (type == Type.NUMBER) {
            
            this.resizeable = false;
        }
    }
    
    /**
     * Returns the basic type of the column.
     */
    public Type getType() {
        
        return type;
    }
    
    /**
     * Returns the native type for the column. This will only be 
     * valid for SQL result sets.
     * 
     * @return The native type {@link java.sql.Types} or -1 if 
     *   the native type is not known or unavailable.
     */
    public int getNativeType () {
    
        return nativeType;
    }

    /**
     * Sets the native type.
     * 
     * @param nativeType The native type to set.
     */
    public void setNativeType (int nativeType) {
    
        this.nativeType = nativeType;
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
    
    /**
     * Returns the column name
     */
    public String toString() {
        
        return name;
        
    }
    
}
