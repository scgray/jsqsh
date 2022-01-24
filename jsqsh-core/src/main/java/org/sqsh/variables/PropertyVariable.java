/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh.variables;

import java.lang.reflect.InvocationTargetException;

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
     * @param isSettable True if the property can be set.
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
        
        Throwable failure = null;
        
        if (!settable) {
            
            throw new CannotSetValueError("The value of \"" 
                + getName() + "\" is read only");
        }
        
        if (failure == null) {
            
            try {
                
                Object o = getManager().getBean(bean);
                BeanUtils.setProperty(o, property, value);
            }
            catch (InvocationTargetException e) {
                
                failure = e.getTargetException();
            }
            catch (Throwable e) {
                
                failure = e;
            }
        }
        
        if (failure != null) {
            
            throw new CannotSetValueError("Cannot set variable \"" 
               + getName() + "\" (bean property " 
               + bean + "." + property + ") to \""
               + value + "\": " + failure.getMessage(), failure);
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
                
                return "null";
            }
            
            return val.toString();
        }
        catch (Throwable e) {
            
            if (quiet) {
                
                return "null";
            }
            
            return "Cannot read variable '" + getName() + "': " 
                + e.getMessage() + " (" + e.getClass().getName() + ")";
        }
    }
}
