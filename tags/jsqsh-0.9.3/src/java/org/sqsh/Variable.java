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
 * The representation of a variable in sqsh. 
 */
public abstract class Variable 
    extends HelpTopic
    implements Comparable, Cloneable {
    
    /**
     * The context that owns thie variable.
     */
    private VariableManager varMan = null;
    
    /**
     * The name of the variable.
     */
    private String name;
    
    /**
     * Indicates whether or not this variable is to be exported to the
     * environment of spawned processes.
     */
    private boolean isExported;
    
    /**
     * This should only be called the VariableManager
     */
    public Variable() {
        
    }
    
    /**
     * Creates a named variable.
     * @param name The name of the variable.
     */
    public Variable (String name) {
        
        super.setTopic(name);
        this.name = name;
    }
    
    /**
     * Creates a named variable.
     * @param name The name of the variable.
     */
    public Variable (String name, boolean isExported) {
        
        this.name = name;
        this.isExported = isExported;
    }
    
    /**
     * Used by the VariableManager to record the fact that it owns this
     * variable.
     * 
     * @param varMan The session.
     */
    public void setManager (VariableManager varMan) {
        
        this.varMan = varMan;
    }
    
    /**
     * Returns the manager that owns this variable.
     * 
     * @return The manager that owns this variable.
     */
    public VariableManager getManager() {
        
        return varMan;
    }
    
    /**
     * Sets the name of the variable.
     * 
     * @param name The name of the variable.
     */
    public void setName(String name) {
        
        super.setTopic(name);
        this.name = name;
    }
    
    /**
     * Returns the name of the variable.
     * 
     * @return The name of the variable.
     */
    public String getName() {
        
        return name;
    }
    
    /**
     * Returns whether or not this variable is to be exported to 
     * the processes spawned by sqsh.
     * 
     * @return True if the variable will be exported.
     */
    public boolean isExported () {
    
        return isExported;
    }
    
    /**
     * Sets whether or not this variable will be exported to the
     * processes spawned by sqsh.
     * 
     * @param isExported true if it is to be exported.
     */
    public void setExported (boolean isExported) {
    
        this.isExported = isExported;
    }

    /**
     * All variables must be settable via a string value, regardless of
     * their actual underlying type. This is to allow the sqsh 'set' 
     * command to attempt to set any variable value.
     * 
     * @param value The new value of the variable.
     * @throws CannotSetValueError This error may, optionally, be thrown
     *   if the variable does not allow its value to be set or the value
     *   provided is invalid. This is an error and not an exception 
     *   because most internal code will be well aware of which variables 
     *   are settable and which aren't and I don't want to have to have
     *   try/catch blocks everywhere.
     */
    public abstract String setValue(String value) 
        throws CannotSetValueError;
    
    /**
     * Every variable must provide a toString() that displayed the value
     * of the variable. This is the method that will be used during variable
     * expansion on the command line and within SQL text.
     */
    public abstract String toString();
    
    /**
     * Compares the names of two variables. This method is provided primarily
     * to allow for easy sorting of variables on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(Object o) {
        
        if (o instanceof Variable) {
            
            return name.compareTo(((Variable) o).getName());
        }
        
        return -1;
    }
    
    /**
     * Compares two variables for equality. Variables are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is a Variable that has the same name as this.
     */
    public boolean equals(Object o) {
        
        if (o instanceof Variable) {
            
            return ((Variable) o).getName().equals(name);
        }
        
        return false;
    }
    
    /**
     * Returns a hash value for the variable. The hash code is nothing
     * more than the hash code for the variable name itself.
     * 
     * @return The hash code of the variable's name.
     */
    public int hashCode() {
        
        return name.hashCode();
    }
}
