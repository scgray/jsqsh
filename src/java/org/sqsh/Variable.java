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
 * The representation of a variable in sqsh. 
 */
public abstract class Variable 
    extends HelpTopic
    implements Cloneable {
    
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
        
        super.setTopic(name);
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
