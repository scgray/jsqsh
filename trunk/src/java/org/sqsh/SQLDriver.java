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

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sqsh.analyzers.ANSIAnalyzer;
import org.sqsh.analyzers.NullAnalyzer;

public class SQLDriver 
    implements Comparable {
    
    private static final Logger LOG = 
        Logger.getLogger(SQLDriver.class.getName());
    
    public static String USER_PROPERTY = "user";
    public static String PASSWORD_PROPERTY = "password";
    public static String SERVER_PROPERTY = "server";
    public static String PORT_PROPERTY = "port";
    public static String DATABASE_PROPERTY = "db";
    public static String SID_PROPERTY = "SID";
    public static String DOMAIN_PROPERTY = "domain";
    
    private SQLDriverManager driverMan = null;
    private String name = null;
    private String target = null;
    private String url = null;
    private String clazz = null;
    private boolean isInternal = false;
    private boolean isAvailable = false;
    private Map<String, String> variables = new HashMap<String, String>();
    private Map<String, String> properties = new HashMap<String, String>();
    private SQLAnalyzer analyzer = new NullAnalyzer();
    
    public SQLDriver() {
        
    }
    
    public SQLDriver(String name, String clazz, String url) {
        
        this.name = name;
        this.url = url;
        
        setDriverClass(clazz);
    }
    
    /**
     * Retrieves the variable map used by the driver.
     * 
     * @return The variable map used by the driver.
     */
    public Map<String, String> getVariables() {
        
        return variables;
    }
    
    /**
     * Sets the name of the class that will be utilized for analyzing the
     * SQL statements to be executed.
     * 
     * @param clazz The name of the class.
     */
    public void setAnalyzer(String sqlAnalyzer) {
        
        try {
            
            Class clazz = Class.forName(sqlAnalyzer);
            Constructor<SQLAnalyzer> constructor =  clazz.getConstructor();
            
            analyzer = constructor.newInstance();
        }
        catch (Exception e) {
            
            throw new CannotSetValueError("Unable to instantiate "
                + sqlAnalyzer + ": " + e.getMessage());
        }
    }
    
    /**
     * Returns the SQL analyzer for this driver.
     * @return The SQL analyzer for this driver or null if none is defined.
     */
    public SQLAnalyzer getAnalyzer() {
        
        if (analyzer == null) {
            
            analyzer = new ANSIAnalyzer();
        }
        
        return analyzer;
    }
    
    /**
     * Sets the manager for this driver.
     * @param driverMan The manager for this driver.
     */
    protected void setDriverManager(SQLDriverManager driverMan) {
        
        this.driverMan = driverMan;
    }
    
    /**
     * Returns true if the driver is available for use.
     * 
     * @return true if the driver is available for use.
     */
    public boolean isAvailable() {
        
        return isAvailable;
    }
    
    /**
     * Sets whether or not the driver is available.
     * 
     * @param isAvailable
     */
    public void setAvailable (boolean isAvailable) {
        
        this.isAvailable = isAvailable;
    }
    
    /**
     * The target is the target database platform.
     * 
     * @param target Target database platorm.
     */
    public void setTarget(String target) {
        
        this.target = target;
    }
    
    /**
     * Returns the target database platform.
     * @return The target database paltform.
     */
    public String getTarget() {
        
        return target;
    }
    
    /**
     * Sets the class name that the driver requires.
     * 
     * @param clazz The name of the class that implements the driver.
     */
    public void setDriverClass(String clazz) {
        
        this.clazz = clazz;
        
        if (clazz != null) {
            
            try {
                
                Class.forName(clazz);
                isAvailable = true;
            }
            catch (Exception e) {
                
                isAvailable = false;
            }
        }
    }
    
    /**
     * Returns the name of the class implementing this driver.
     * 
     * @return the name of the class implementing this driver.
     */
    public String getDriverClass() {
        
        return clazz;
    }
    
    /**
     * Sets the value of a variable.
     * @param name The name of the variable
     * @param value The value of the variable.
     */
    public void setVariable(String name, String value) {
        
        variables.put(name, value);
    }
    
    /**
     * Returns the value of a variable.
     * @param name The name to look up
     * @return The value or null if the variable is not defined.
     */
    public String getVariable(String name) {
        
        return variables.get(name);
    }
    
    /**
     * Adds a property to the driver. These properties are passed
     * in during the connection process.
     * 
     * @param name The name of the property
     * @param value The value of the property.
     */
    public void setProperty(String name, String value) {
        
        properties.put(name, value);
    }
    
    /**
     * Retrieves a property from the driver definition.
     * 
     * @param name The name of the property
     * @return The value of the property or NULL if the property 
     *    is not define.
     */
    public String getProperty(String name) {
        
        return properties.get(name);
    }
    
    /**
     * Retrieves the set of property names that have been associated
     * this this driver definition.
     * 
     * @return A set of property names.
     */
    public Collection<String> getPropertyNames() {
        
        return properties.keySet();
    }
    
    /**
     * Returns the JDBC URL for this driver.
     * @return The JDBC URL for this driver.
     */
    public String getUrl () {
    
        return url;
    }

    /**
     * Sets the JDBC URL for this driver.
     * 
     * @param url The url
     */
    public void setUrl (String url) {
    
        this.url = url;
    }

    /**
     * Returns an indicator as to whether or not the driver is an internal
     * driver or a user-defined driver.
     * 
     * @return true if it is an internal driver.
     */
    public boolean isInternal () {
    
        return isInternal;
    }

    /**
     * Sets whether or not this is an internal driver.
     * @param isInternal 
     */
    public void setInternal (boolean isInternal) {
    
        this.isInternal = isInternal;
    }

    /**
     * Set the name of the command.
     * 
     * @param name The name of the command.
     */
    public void setName(String name) {
        
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
     * Compares the names of two drivers. This method is provided primarily
     * to allow for easy sorting of drivers on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(Object o) {
        
        if (o instanceof SQLDriver) {
            
            return name.compareTo(((SQLDriver) o).getName());
        }
        
        return -1;
    }
    
    /**
     * Compares two drivers for equality. Drivers are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is a Drivers that has the same name as this.
     */
    public boolean equals(Object o) {
        
        if (o instanceof SQLDriver) {
            
            return ((SQLDriver) o).getName().equals(name);
        }
        
        return false;
    }
    
    /**
     * Returns a hash value for the driver. The hash code is nothing
     * more than the hash code for the driver name itself.
     * 
     * @return The hash code of the driver's name.
     */
    public int hashCode() {
        
        return name.hashCode();
    }
}
