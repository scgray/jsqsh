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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.digester.Digester;


/**
 *  The SQLDriverManager provides an interface for registering SQL drivers
 *  and grabbing connections from said drivers.
 */
public class SQLDriverManager {
    
    private static final Logger LOG = 
        Logger.getLogger(SQLDriverManager.class.getName());
    
    /**
     * This is the map of logical driver names to driver defitions.
     */
    private Map<String, SQLDriver> drivers =
        new HashMap<String, SQLDriver>();
    
    /**
     * If not null, this controls the default database that connections 
     * will be placed in.
     */
    private String defaultDatabase = null;
    
    /**
     * This is the default autocommit setting for all connections 
     * created by the drivers.
     */
    private boolean defaultAutoCommit = true;
    
    /**
     * This is the class loader that is used to attempt to load our JDBC
     * drivers. Initially we will have no locations defined, but additional
     * locations can be added by the user by calling setClasspath();
     */
    private URLClassLoader classLoader = new URLClassLoader(
        new URL[0], getClass().getClassLoader());
    
    public SQLDriverManager() {
        
        /*
         * Load the internal drivers.
         */
        URL url = getClass().getClassLoader()
            .getResource("org/sqsh/Drivers.xml");
        
        if (url == null) {
            
            System.err.println("ERROR: Unable to load internal drivers "
                + "file!!");
        }
        else {
        
            loadDrivers(url, true);
        }
    }
    
    /**
     * Returns the default autocommit setting for new connections created
     * using the manager.
     * 
     * @return The default autocommit setting for all connections created
     */
    public boolean getDefaultAutoCommit() {
        
        return defaultAutoCommit;
    }
    
    /**
     * Changes the default autocommit setting for new connections
     * created using the manager.
     * @param autocommit The default autocommit setting
     */
    public void setDefaultAutoCommit(boolean autocommit) {
        
        this.defaultAutoCommit = autocommit;
    }
    
    /**
     * Sets the dafault database (catalog) that will be used by the 
     * connection when established.
     * 
     * @param db The database name or null.
     */
    public void setDefaultDatabase(String db) {
        
        if (db == null || "".equals(db) || "null".equals(db)) {
            
            this.defaultDatabase = null;
        }
        else {
            
            this.defaultDatabase = db;
        }
    }
    
    /**
     * Returns the default database.
     * 
     * @return The default database.
     */
    public String getDefaultDatabase() {
        
        return this.defaultDatabase;
    }
    
    /**
     * Sets a new classpath that will be used to search for JDBC drivers.
     * 
     * @param classpath A delimited list of jars or directories containing
     *   jars. The delimiter that is used should be your system's path
     *   delimiter.
     * 
     * @throws IOException if there is a problem
     */
    public void setClasspath(String classpath)
        throws IOException {
        
        List<URL> urls = new ArrayList<URL>();
        
        /*
         * Split up our classpath based upon the system's path separator.
         */
        String []locations = classpath.split(System.getProperty(
            "path.separator"));
        
        for (String location : locations) {
            
            File file = new File(location);
            
            /*
             * If the provided location was a directory, then look inside
             * of it can suck up every .jar file.
             */
            if (file.isDirectory()) {
                
                File []dirContents = file.listFiles();
                for (File subFile : dirContents) {
                    
                    if (subFile.isFile()
                            && subFile.getName().endsWith(".jar")) {
                        
                        urls.add(subFile.toURI().toURL());
                    }
                }
            }
            else if (file.exists()) {
                
                urls.add(file.toURI().toURL());
            }
        }
        
        classLoader = new URLClassLoader(urls.toArray(new URL[0]),
            this.getClass().getClassLoader());
        
        checkDriverAvailability();
    }
    
    /**
     * Used to check whether or not all of the currently registered
     * JDBC drivers is available in the current classloader.
     */
    private void checkDriverAvailability() {
        
        for (SQLDriver driver : drivers.values()) {
            
            try {
                
                Class.forName(driver.getDriverClass(), true, classLoader);
                driver.setAvailable(true);
            }
            catch (Exception e) {
                
                driver.setAvailable(false);
            }
        }
    }
    
    /**
     * Returns the current classpath. The classpath will have been expanded
     * of all jars contained in the directories specified by the original call
     * to setClasspath().
     * 
     * @return the current classpath.
     */
    public String getClasspath() {
        
        URL []urls = classLoader.getURLs();
        StringBuilder sb = new StringBuilder();
        String sep = System.getProperty("path.separator");
        
        for (int i = 0; i < urls.length; i++) {
            
            if (i > 0) {
                
                sb.append(sep);
            }
            
            sb.append(urls[i].toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Establishes a connection with an explicitly defined URL and class.
     * 
     * @param clazz The JDBC class.
     * @param url The JDBC URL to connect to.
     * @param session The session for which the connection is being 
     *    established. This is used primarily as a source of variables.
     * @param properties Connection properties to be utilized.
     * @return A newly created connection.
     * @throws SQLException Thrown if the connection could not be 
     *    established.
     */
    public SQLContext connect (String clazz, String url, Session session,
            Map<String, String>properties)
        throws SQLException {
        
        SQLDriver d = new SQLDriver("__temp__", clazz, url);
        drivers.put(d.getName(), d);
        
        try {
            
            return connect(d.getName(), session, properties);
            
        }
        finally {
            
            drivers.remove(d);
        }
    }
    
    /**
     * Attempts to connect to the database utilizing a driver.
     * 
     * @param driver The name of the driver to utilize. The special
     *    driver name "generic" can be provided if the manager does not
     *    have a registered driver that can be used. 
     * @param session The session for which the connection is being 
     *    established. This is used primarily as a source of variables.
     * @param properties Connection properties to be utilized.
     * @return A newly created connection.
     * @throws SQLException Thrown if the connection could not be 
     *    established.
     */
    public SQLContext connect (String driver, Session session,
            Map<String, String>properties)
        throws SQLException {
        
        SQLDriver sqlDriver = null;
        
        sqlDriver = getDriver(driver);
        if (sqlDriver == null) {
                
            throw new SQLException("Sqsh has no driver registered under "
                + "the name '" + driver + "'. To see a list of available "
                + "drivers, use the \\drivers command");
        }
        
        /*
         * Expand the url of its variables.
         */
        String url = getUrl(session, properties, sqlDriver.getVariables(),
            sqlDriver.getUrl());
        
        Connection conn = null;
        
        try {
            
            Driver jdbcDriver = getDriverFromUrl(url);
            
            /*
             * Similar to above, we'll iterate through the properties supported by
             * the driver and set them as necessary.
             */
            Properties props = new Properties();
            DriverPropertyInfo []supportedProperties =
                jdbcDriver.getPropertyInfo(url, null);
            
            for (int i = 0; i < supportedProperties.length; i++) {
                
                String name = supportedProperties[i].name;
                String value = getProperty(session, properties,
                    sqlDriver.getVariables(), name);
                
                if (value != null) {
                    
                    props.put(name, value);
                }
            }
            
            String s = getProperty(session, properties,
                sqlDriver.getVariables(), SQLDriver.USER_PROPERTY);
            if (s == null) {
                
                s = promptInput("Username", false);
                
            }
            props.put(SQLDriver.USER_PROPERTY, s);
            
            s = getProperty(session, properties, sqlDriver.getVariables(),
                SQLDriver.PASSWORD_PROPERTY);
            if (s == null) {
                
                s = promptInput("Password", true);
            }
            props.put(SQLDriver.PASSWORD_PROPERTY, s);
            
            conn = jdbcDriver.connect(url, props);
        }
        catch (SQLException e) {
            
            throw new SQLException(
                "Unable to connect via JDBC url '"
                + url + "': " + e.getMessage(), e.getSQLState(), 
                e.getErrorCode());
        }
        
        String database = getProperty(session, properties,
            sqlDriver.getVariables(),  SQLDriver.DATABASE_PROPERTY);
        if (database == null && defaultDatabase != null) {
            
            database = defaultDatabase;
        }
        
        if (database != null) {
            
            try {
                
                conn.setCatalog(database);
            }
            catch (SQLException e) {
                
                session.err.println("WARNING: Could not switch database context"
                    + " to '" + database + "': " + e.getMessage());
            }
        }    
        
        try {
                
            conn.setAutoCommit(defaultAutoCommit);
        }
        catch (SQLException e) {
            
            session.err.println("WARNING: Unable to set auto-commit mode to "
                + defaultAutoCommit);
        }
        
        return new SQLContext(conn, url, sqlDriver.getAnalyzer());
    }
    
    /**
     * Similar to DriverManager.getDriver() except that it searches
     * through our shiny new classloader.
     * 
     * @param url
     * @return
     * @throws SQLException
     */
    private Driver getDriverFromUrl(String url)
        throws SQLException {
        
        for (SQLDriver driver : drivers.values()) {
            
            try {
                
                Driver d = (Driver) Class.forName(driver.getDriverClass(), 
                    true, classLoader).newInstance();
                
                if (d.acceptsURL(url)) {
                    
                    return d;
                }
            }
            catch (Exception e) {
                
                /* IGNORED */
            }
        }
        
        return DriverManager.getDriver(url);
    }
    
    /**
     * Used to prompt input from a user.
     * 
     * @param prompt The prompt to use.
     * @param isMasked true if the input is to be masked.
     * @return The input
     */
    private static String promptInput(String prompt, boolean isMasked) {
        
        try {
        
            if (isMasked == false) {
                
                System.out.print(prompt);
                System.out.print(": ");
                
                BufferedReader input = new
                    BufferedReader(new InputStreamReader(System.in));
                
                String results = input.readLine();
                return results;
            }
            else {
                
                return new String(PasswordInput.getPassword(
                    System.in, prompt + ": "));
            }
        }
        catch (IOException e) {
            
            return null;
        }
    }
    
    /**
     * Used to expand a URL of any variables that may be defined in the 
     *  user supplied properties, session, or SQLDriver variables (in that
     *  order).
     *  
     * @param session The session handle.
     * @param properties The user supplied properties.
     * @param vars SQLDriver variables (can be null).
     * @param url The URL to expand.
     * @return The URL expanded of all variables.
     */
    private String getUrl(Session session, Map<String, String>properties,
            Map<String, String>vars, String url) {
        
        Map<String, String> connProperties = new HashMap<String, String>();
        
        if (vars != null) {
            
            for (String name : vars.keySet()) {
                
                String value = getProperty(session, properties, vars, name);
                connProperties.put(name, value);
            }
        }
        
        connProperties.put(SQLDriver.USER_PROPERTY,
            getProperty(session, properties, vars, SQLDriver.USER_PROPERTY));
        connProperties.put(SQLDriver.PASSWORD_PROPERTY,
            getProperty(session, properties, vars, SQLDriver.PASSWORD_PROPERTY));
        
        return session.expand(connProperties, url);
    }
    
    /**
     * Used internally to look up a property value following an inheritence
     * scheme of (1) properties passed by user, (2) session properties,
     * (3) the variables associated with the driver.
     * 
     * @param Session owning session
     * @param vars Variables defined by a driver (can be null).
     * @param properties The properties passed by the user.
     * @param name Variable to look up.
     * @return The value of the property.
     */
    private String getProperty(Session session, Map<String, String>properties,
            Map<String, String>vars, String name) {
        
        String value = properties.get(name);
        if (value == null) {
                
            value = session.getVariable(name);
            if (value == null && vars != null) {
                    
                value = vars.get(name);
            }
        }
        
        return value;
    }
    
    
    /**
     * Adds a driver to the manager.
     * @param driver The new driver.
     */
    public void addDriver(SQLDriver driver) {
        
        drivers.put(driver.getName(), driver);
        driver.setDriverManager(this);
    }
    
    /**
     * Fetches a driver by name
     * @param name
     * @return
     */
    public SQLDriver getDriver(String name) {
        
        return drivers.get(name);
    }
    
    /**
     * Returns the set of drivers currently registered.
     * @return the set of drivers currently registered.
     */
    public SQLDriver[] getDrivers() {
        
        return drivers.values().toArray(new SQLDriver[0]);
    }
    
    /**
     * Loads a driver configuration file.
     * @param file The file to load.
     */
    public void load(File file) {
        
        try {
            
            loadDrivers(file.toURI().toURL(), false);
        }
        catch (MalformedURLException e){
            
            /* SHOULDN'T HAPPEN */
        }
    }
    
    private void loadDrivers(URL url, boolean isInternal) {
        
        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        
        path = "Drivers/Driver";
        digester.addObjectCreate(path,  "org.sqsh.SQLDriver");
        digester.addSetNext(path, "addDriver", "org.sqsh.SQLDriver");
        digester.addCallMethod(path, 
            "setName", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "name");
        digester.addCallMethod(path, 
            "setUrl", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "url");
        digester.addCallMethod(path, 
            "setDriverClass", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "class");
        digester.addCallMethod(path, 
            "setTarget", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "target");
        digester.addCallMethod(path, 
            "setAnalyzer", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "analyzer");
            
        path = "Drivers/Driver/Variable";
        digester.addCallMethod(path, 
            "setVariable", 2, new Class[] { java.lang.String.class,
                java.lang.String.class });
            digester.addCallParam(path, 0, "name");
            digester.addCallParam(path, 1);
            
        digester.push(this); 
        InputStream in = null;
        try {
            
            in = url.openStream();
            digester.parse(in);
        }
        catch (Exception e) {
            
            System.err.println("Failed to parse driver file '"
                + url.toString() + "': " + e.getMessage());
            e.printStackTrace(System.err);
        }
        finally {
            
            try {
                
                in.close();
            }
            catch (IOException e) {
                
                /* IGNORED */
            }
        }
    }
    
}
