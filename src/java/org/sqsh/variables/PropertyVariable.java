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

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.sqsh.CannotSetValueError;
import org.sqsh.Variable;

/**
 * Represents a variable that is a wrapper around a property of the sqsh
 * session object.
 */
public class PropertyVariable
    extends Variable {
    
    private String property = null;
    private String bean = null;
    private boolean settable = true;
    private boolean quiet = false;
    
    /**
     * Manditory constructor.
     */
    public PropertyVariable() {
    }
    
    /**
     * @return the bean who's property we are setting.
     */
    public String getBean () {
    
        return bean;
    }
    
    /**
     * @param bean the bean to set
     */
    public void setBean (String bean) {
    
        this.bean = bean;
    }

    /**
     * The name of the property that is to be manipulated by this variable.
     * @param property name of the property that is to be manipulated by this variable.
     */
    public void setProperty(String property) {
        
        this.property = property;
    }
    
    /**
     * Returns whether or not this property can be set.
     * @return Returns whether or not this property can be set.
     */
    public boolean isSettable () {
    
        return settable;
    }
    
    /**
     * Set to true if the property can be set.
     * @param allowSet True if the property can be set.
     */
    public void setSettable (boolean isSettable) {
    
        this.settable = isSettable;
    }
    
    /**
     * Returns true if failures to get or set should be silently ignored.
     * @return true if failures to get or set should be silently ignored.
     */
    public boolean isQuiet () {
    
        return quiet;
    }

    /**
     * @param quiet true if failures to get or set should be silently ignored.
     */
    public void setQuiet (boolean quiet) {
    
        this.quiet = quiet;
    }

    @Override
    public String setValue (String value)
        throws CannotSetValueError {
        
        String failure = null;
        
        if (settable == false) {
            
            failure = "The value of '" + getName() 
                + "' is immutable and cannot be changed";
        }
        
        if (failure == null) {
            
            try {
                
                Object o = getManager().getBean(bean);
                BeanUtils.setProperty(o, property, value);
            }
            catch (Throwable e) {
                
                if (quiet == false) {
                    
                    failure = e.getMessage();
                }
            }
        }
        
        if (failure != null) {
            
            throw new CannotSetValueError(failure);
        }
        
        return null;
    }

    @Override
    public String toString () {

        try {
            
            Object o = getManager().getBean(bean);
            Object val = 
                PropertyUtils.getNestedProperty(o, property);
            
            if (val == null) {
                
                return null;
            }
            
            return val.toString();
        }
        catch (Throwable e) {
            
            if (quiet == true) {
                
                return null;
            }
            
            return e.getMessage() + "(" + e.getClass().getName() + ")";
        }
    }
}
