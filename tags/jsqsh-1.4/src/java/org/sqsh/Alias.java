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
 * Representation of a sqsh alias. An alias is similar to a variable 
 * expansion except that it does not require any special syntax...anywhere
 * the alias is found in a line, it is replaced.
 */
public class Alias
    implements Comparable {
    
    /**
     * The actual alias.
     */
    private String name;
    
    /**
     * The replacement text for the alias.
     */
    private String text;
    
    /**
     * A global alias can be replaced anywhere within a line of text, if
     * an alias is not global, then it will only be replaced if it is the
     * first word on the line.
     */
    private boolean isGlobal = false;

    /**
     * Creates a new alias.
     * 
     * @param name The name of the alias.
     * @param text The replacement text for the alias.
     * @param isGlobal A global alias can be replaced anywhere within 
     * a line of text, if an alias is not global, then it will only
     * be replaced if it is the first word on the line.
     */
    public Alias (String name, String text, boolean isGlobal) {
        
        this.name = name;
        this.text = text;
        this.isGlobal = isGlobal;
    }
    
    /**
     * Returns the name of the alias.
     * 
     * @return The name of the alias.
     */
    public String getName() {
        
        return name;
    }
    
    /**
     * @return Whether or not the alias is global.
     */
    public boolean isGlobal () {
    
        return isGlobal;
    }
    
    /**
     * @param Sets whether or not the alias is global
     */
    public void setGlobal (boolean isGlobal) {
    
        this.isGlobal = isGlobal;
    }
    
    /**
     * @return The text that will replace the alias.
     */
    public String getText () {
    
        return text;
    }

    
    /**
     * @param text The text that will replace the alias.
     */
    public void setText (String text) {
    
        this.text = text;
    }

    /**
     * Compares the names of two aliases. This method is provided primarily
     * to allow for easy sorting of aliases on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(Object o) {
        
        if (o instanceof Alias) {
            
            return name.compareTo(((Alias) o).getName());
        }
        
        return -1;
    }
    
    /**
     * Compares two aliases for equality. Aliases are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is an Alias that has the same name as this.
     */
    public boolean equals(Object o) {
        
        if (o instanceof Alias) {
            
            return ((Alias) o).getName().equals(name);
        }
        
        return false;
    }
    
    /**
     * Returns a hash value for the alias. The hash code is nothing
     * more than the hash code for the alias name itself.
     * 
     * @return The hash code of the alias's name.
     */
    public int hashCode() {
        
        return name.hashCode();
    }
}
