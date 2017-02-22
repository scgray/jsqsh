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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.sqsh.analyzers.ANSIAnalyzer;
import org.sqsh.analyzers.SQLAnalyzer;
import org.sqsh.analyzers.NullAnalyzer;
import org.sqsh.normalizer.SQLNormalizer;
import org.sqsh.normalizer.NullNormalizer;

public class SQLDriver 
    implements Comparable<SQLDriver> {
    
    private static final Logger LOG = 
        Logger.getLogger(SQLDriver.class.getName());
    
    public static String USER_PROPERTY = "user";
    public static String PASSWORD_PROPERTY = "password";
    public static String SERVER_PROPERTY = "server";
    public static String PORT_PROPERTY = "port";
    public static String DATABASE_PROPERTY = "db";
    public static String DOMAIN_PROPERTY = "domain";
    
    /**
     * Simple class to describe the set of variables used to configure the
     * driver.
     */
    public static class DriverVariable implements Comparable<DriverVariable>{
        
        private String name;
        private String displayName;
        private String defaultValue;
        
        public DriverVariable (String name, String displayName, String defaultValue) {
            
            this.name = name;
            this.displayName = displayName;
            this.defaultValue = defaultValue;
        }

        public String getName() {
        
            return name;
        }

        public String getDisplayName() {
        
            return displayName;
        }

        public String getDefaultValue() {
        
            return defaultValue;
        }

        @Override
        public int compareTo(DriverVariable o) {

            return this.displayName.compareTo(o.displayName);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DriverVariable) {
                return ((DriverVariable)o).name.equals(this.name);
            }
            return false;
        }
    }
    
    protected static SQLNormalizer DEFAULT_NORMALIZER = new NullNormalizer();
    
    private SQLDriverManager driverMan = null;
    private String name = null;
    private String target = null;
    private String url = null;
    private String clazz = null;
    private boolean isInternal = false;
    private Class<? extends Driver> driver = null;
    private Map<String, String> variables = new HashMap<String, String>();
    private Map<String, String> properties = new HashMap<String, String>();
    private Map<String, String> sessionVariables = new HashMap<String, String>();
    private SQLAnalyzer analyzer = new NullAnalyzer();
    private SQLNormalizer normalizer = DEFAULT_NORMALIZER;
    private String currentSchemaQuery = null;
    private List<String> classpath = null;
    
    public SQLDriver() {
        
    }
    
    public SQLDriver(String name, String clazz, String url) {
        
        this.name = name;
        this.url = url;
        
        setDriverClass(clazz);
    }
    
    /**
     * @return A copy of this driver.
     */
    public SQLDriver copy() {
        
        SQLDriver n = new SQLDriver(this.name, clazz, url);
        n.driverMan = driverMan;
        n.isInternal = false;
        n.driver = driver;
        n.target = target;
        n.variables = new HashMap<String, String>();
        n.variables.putAll(variables);
        n.properties = new HashMap<String, String>();
        n.properties.putAll(properties);
        n.sessionVariables = new HashMap<String, String>();
        n.sessionVariables.putAll(sessionVariables);
        n.analyzer = analyzer;
        n.currentSchemaQuery = currentSchemaQuery;
        n.normalizer = normalizer;
        if (classpath != null) {
            
            n.classpath = new ArrayList<String>();
            n.classpath.addAll(classpath);
        }
        
        return n;
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
     * Retrieves the set of variables that are to set in the users
     * session upon connection.
     * 
     * @return The session variable map to be placed in the user's
     *    session.
     */
    public Map<String, String> getSessionVariables() {
        
        return sessionVariables;
    }
    
    /**
     * Return the set of driver properties that will be 
     * @return
     */
    public Map<String, String> getDriverProperties() {
        
        return properties;
    }
    
    /**
     * Sets the name of the class that will be utilized for analyzing the
     * SQL statements to be executed.
     * 
     * @param sqlAnalyzer The name of the class.
     */
    public void setAnalyzer(String sqlAnalyzer) {
        
        isInternal = false;
        try {
            
            Class<? extends SQLAnalyzer> clazz 
                = Class.forName(sqlAnalyzer).asSubclass(SQLAnalyzer.class);
            Constructor<? extends SQLAnalyzer> constructor 
                = clazz.getConstructor();
            
            analyzer = constructor.newInstance();
        }
        catch (Exception e) {
            
            throw new CannotSetValueError("Unable to instantiate "
                + sqlAnalyzer + ": " + e.getMessage());
        }
    }
    
    /**
     * Sets the SQL analyzer for this driver
     * @param analyzer The SQL analyzer for this driver
     */
    public void setAnalyzer(SQLAnalyzer analyzer) {
        
        if (analyzer == null) {
            
            this.analyzer = new ANSIAnalyzer();
        }
        else {
            
            this.analyzer = analyzer;
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
     * Sets the name of the class that will be utilized for normalizing identifier names
     * 
     * @param clazz The name of the class.
     */
    public void setNormalizer(String sqlNormalizer) {
        
        isInternal = false;
        try {
            
            Class<? extends SQLNormalizer> clazz = 
                Class.forName(sqlNormalizer).asSubclass(SQLNormalizer.class);
            Constructor<? extends SQLNormalizer> constructor 
                = clazz.getConstructor();
            
            normalizer = constructor.newInstance();
        }
        catch (Exception e) {
            
            throw new CannotSetValueError("Unable to instantiate "
                + sqlNormalizer + ": " + e.getMessage());
        }
    }
    
    /**
     * Sets the SQL normalizer for this driver
     * @param normalizer The SQL normalizer for this driver
     */
    public void setNormalizer(SQLNormalizer normalizer) {
        
        if (normalizer == null) {
            
            this.normalizer = DEFAULT_NORMALIZER;
        }
        else {
            
            this.normalizer = normalizer;
        }
    }
    
    /**
     * Returns the SQL normalizer for this driver.
     * @return The SQL normalizer for this driver or null if none is defined.
     */
    public SQLNormalizer getNormalizer() {
        
        if (normalizer == null) {
            
            return DEFAULT_NORMALIZER;
        }
        
        return normalizer;
    }
    
    /**
     * Installs a query that can be run to determine the session's current
     * schema. This is only necessary if the JDBC driver does not support the
     * API call to fetch the current schema;
     * 
     * @param query The query to run to fetch the current schema. This must
     *   return a single row with a single string column
     */
    public void setCurrentSchemaQuery(String query) {
        
        this.currentSchemaQuery = query;
    }
    
    /**
     * @return A query to use to fetch the current active schema for the session.
     *   This may return null if there is no such query installed.
     */
    public String getCurrentSchemaQuery() {
        
        return this.currentSchemaQuery;
    }
    
    /**
     * Adds a file or classpath onto the end of the existing classpath
     * @param classpath The new classpath to add. This classpath may contain
     *   environment variables, which will be expanded.
     * @throws IOException
     */
    public void addClasspath(String classpath) throws IOException {
        
        isInternal = false;
        
        /*
         * Split up our classpath based upon the system's path separator.
         */
        String []locations = classpath.split(System.getProperty(
            "path.separator"));
        
        if (this.classpath == null) {
            
            this.classpath = new ArrayList<String>();
        }
        
        for (String loc : locations) {
            
            this.classpath.add(loc);
        }
        
        /*
         * The manager isn't available if this method is called by the digester
         * while loading the configuration file.
         */
        if (driverMan != null) {
            
            driverMan.checkDriverAvailability(getName());
        }
    }
    
    /**
     * Sets a new classpath that will be used when loading this JDBC driver
     * 
     * @param classpath A delimited list of jars or directories containing
     *   jars. The delimiter that is used should be your system's path
     *   delimiter.
     * 
     * @throws IOException if there is a problem
     */
    public void setClasspath(String classpath) throws IOException {
        
        this.classpath = null;
        if (classpath != null) {
            
            addClasspath(classpath);
        }
    }
    
    /**
     * @return An array of classpath elements that are used by this driver 
     *   when it is loaded, or null if the driver has no special classpath
     */
    public String[] getClasspathArray() {
        
        if (classpath == null) {
            
            return null;
        }
        
        return classpath.toArray(new String[0]);
    }
    
    /**
     * @return The classpath that will be used for this driver
     */
    public String getClasspath() {
        
        if (this.classpath == null) {
            
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String sep = System.getProperty("path.separator"); 
        for (int i = 0; i < classpath.size(); i++) {
            
            if (i > 0) {
                
                sb.append(sep);
            }
            
            sb.append(classpath.get(i));
        }
        
        return sb.toString();
    }
    
    /**
     * Returns the list of classpath elements for this driver, with all of the
     * individual jars in the classpath expanded.
     * 
     * @return The list of classpath elements;
     */
    public List<URL> getExpandedClasspath() {
        
        List<URL> urls = new ArrayList<URL>();
        
        if (classpath == null) {
            
            return urls;
        }
        
        StringExpander expander = StringExpander.getEnvironmentExpander();
        for (String path : classpath) {
            
            try {
                
                String expanded = expander.expand(path);
                
                if (expanded.indexOf(File.pathSeparatorChar) >= 0) {
                    
                    String []parts = expanded.split(File.pathSeparator);
                    for (String part : parts) {
                        
                        driverMan.addClasspath(urls, part);
                    }
                }
                else {
                
                    driverMan.addClasspath(urls, expanded);
                }
            }
            catch (IOException e) {
                
                System.err.println("WARNING: Driver " + name + " failed to load \""
                    + path + "\" in classpath: " + e.getMessage());
            }
        }
        
        return urls;
    }
    
    /**
     * Creates a classloader capable of loading this driver.
     * @param parent The parent classloader to delegate to
     * @return The new classloader
     */
    public ClassLoader getClassLoader(ClassLoader parent) {
        
        if (classpath == null) {
            
            return parent;
        }
        
        List<URL> urls = getExpandedClasspath();
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
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
        
        return driver != null;
    }
    
    /**
     * @return If the SQL driver is available, returns the actual JDBC driver
     *   class, otherwise null.
     */
    public Class<? extends Driver> getDriver() {
        
        return driver;
    }
    
    /**
     * Marks the driver as available, indicating which SQL driver will be
     * used to load it.
     * 
     * @param driver The driver class that loads it
     */
    public void setAvailable (Class<? extends Driver> driver) {
        
        boolean wasSet = this.driver != null;
        this.driver = driver;

        if (! wasSet && driverMan != null) {
            
            driverMan.notifyDriverAvailable(this);
        }
    }
    
    /**
     * The target is the target database platform.
     * 
     * @param target Target database platorm.
     */
    public void setTarget(String target) {
        
        isInternal = false;
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
        
        isInternal = false;
        this.clazz = clazz;
        
        if (clazz != null) {
            
            try {
                
                this.driver = Class.forName(clazz).asSubclass(Driver.class);
                setAvailable(this.driver);
            }
            catch (Exception e) {
                
                LOG.fine("Cannot load driver \"" + clazz + "\": " + e.getMessage());
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
        
        isInternal = false;
        variables.put(name, value);
    }
    
    /**
     * Removes a variable default value
     * @param name The name of the variable to remove.
     */
    public void removeVariable(String name) {
        
        isInternal = false;
        variables.remove(name);
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
     * The the value of a variable that is to be set in the user's session
     * upon successfully establishing a connection to a server.
     *
     * @param name The name of the variable
     * @param value The value of the variable.
     */
    public void setSessionVariable(String name, String value) {
        
        isInternal = false;
        sessionVariables.put(name, value);
    }
    
    /**
     * Returns the value of a variable.
     * @param name The name to look up
     * @return The value or null if the variable is not defined.
     */
    public String getSessionVariable(String name) {
        
        return sessionVariables.get(name);
    }
    
    /**
     * Removes the definition of a session variable
     * @param name The session variable to remove
     */
    public void removeSessionVariable(String name) {
        
        sessionVariables.remove(name);
    }
    
    /**
     * Adds a property to the driver. These properties are passed
     * in during the connection process.
     * 
     * @param name The name of the property
     * @param value The value of the property.
     */
    public void setProperty(String name, String value) {
        
        isInternal = false;
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
     * Removes a driver property
     * @param name The name of the property to remove
     */
    public void removeProperty(String name) {
        
        properties.remove(name);
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
    
        isInternal = false;
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
        
        isInternal = false;
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
     * @return A list of variables the driver actually uses and the default
     *     value (if any) for each variable
     */
    public List<DriverVariable> getVariableDescriptions() {
        
        return getVariableDescriptions(true);
    }
    
    /**
     * @return A list of variables the driver actually uses and the default
     *     value (if any) for each variable
     */
    public List<DriverVariable> getVariableDescriptions(boolean userAndPassword) {

        List<DriverVariable> vars = new ArrayList<SQLDriver.DriverVariable>();
        
        /*
         * This is gross, I need to make real metadata somewhere.
         */
        String url = getUrl();
        if (url == null) {
            
            return vars;
        }
        
        final int sz = url.length();
        
        int idx = 0;
        while (idx < sz) {
            
            char ch = url.charAt(idx++);
            if (ch == '$') {
                
                if (idx < sz && url.charAt(idx) == '{') {
                    
                    int start = ++idx;
                    while (idx < sz && url.charAt(idx) != '}') {
                        
                        ++idx;
                    }
                    
                    String name = url.substring(start, idx);
                    ++idx;
                    
                    String displayName = null;
                    if (name.equals(SERVER_PROPERTY)) {
                        
                        displayName = "Server";
                    }
                    else if (name.equals(PORT_PROPERTY)) {
                        
                        displayName = "Port";
                    }
                    else if (name.equals(DATABASE_PROPERTY)) {
                        
                        displayName = "Database/Schema";
                    }
                    else if (name.equals(DOMAIN_PROPERTY)) {
                        
                        displayName = "Domain";
                    }
                    else {
                        
                        displayName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }
                    
                    DriverVariable var = new DriverVariable(name, displayName, getVariable(name));
                    if (! vars.contains(var)) {
                        
                        vars.add(new DriverVariable(name, displayName, getVariable(name)));
                    }
                }
            }
        }
        
        Collections.sort(vars);
        
        if (userAndPassword) {
            
            vars.add(new DriverVariable(USER_PROPERTY, "Username", System.getProperty("user.name")));
            vars.add(new DriverVariable(PASSWORD_PROPERTY, "Password", null));
        }
        
        return vars;
    }
    
    /**
     * Compares the names of two drivers. This method is provided primarily
     * to allow for easy sorting of drivers on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(SQLDriver o) {
        
        return name.compareTo(((SQLDriver) o).getName());
    }
    
    /**
     * Compares two drivers for equality. Drivers are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is a Drivers that has the same name as this.
     */
    @Override
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
    @Override
    public int hashCode() {
        
        return name.hashCode();
    }
}
