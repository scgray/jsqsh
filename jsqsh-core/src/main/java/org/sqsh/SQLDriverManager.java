/*
 * Copyright 2007-2017 Scott C. Gray
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.digester.Digester;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;


/**
 *  The SQLDriverManager provides an interface for registering SQL drivers
 *  and grabbing connections from said drivers.
 */
public class SQLDriverManager {
    
    private static final Logger LOG = 
        Logger.getLogger(SQLDriverManager.class.getName());
    
    /**
     * This is the map of logical driver names to driver definitions.
     */
    private Map<String, SQLDriver> drivers =
        new HashMap<String, SQLDriver>();
    
    /**
     * The default username to connect with ($dflt_username)
     */
    private String defaultUsername = null;
    
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
     * The default JDBC driver name to use.
     */
    private String defaultDriver = "generic";
    
    /**
     * This is the class loader that is used to attempt to load our JDBC
     * drivers. Initially we will have no locations defined, but additional
     * locations can be added by the user by calling setClasspath();
     */
    private URLClassLoader classLoader = new URLClassLoader(
        new URL[0], getClass().getClassLoader());
    
    /**
     * Turned on while loading the configuration files to make sure that 
     * we don't go checking driver availablility every time a classpath
     * entry is added.
     */
    private boolean disabledDriverVerification = false;
    
    /**
     * Set to true while internal drivers are being loaded
     */
    private boolean isLoadingInternal = false;
    
    /**
     * Classes that have registered to listen for when drivers become
     * available
     */
    private List<SQLDriverListener> listeners = new ArrayList<>();
    
    /**
     * Wrapper class around drivers that are to be loaded with my custom
     * class loader. This idea was taken from:
     * 
     *    http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
     */
    public static class DriverShim 
        implements Driver {
        
        private Driver driver;
        
        public DriverShim(Driver driver) {
            
            this.driver = driver;
        }

        @Override
        public boolean acceptsURL(String url)
            throws SQLException {

            boolean ret = driver.acceptsURL(url);
            return ret;
        }

        @Override
        public Connection connect(String url, Properties arg1)
                        throws SQLException {

            try {
                
                return driver.connect(url, arg1);
            }
            catch (SQLException e) {
                
                /*
                 * Some drivers (HIVE!!) have connect() methods that are broken
                 * and throw an exception rather than returning null when they
                 * receive the wrong URL. This checks for that and gives them 
                 * the correct behavior.
                 */
                if (!driver.acceptsURL(url)) {
                    
                    return null;
                }
                
                throw e;
            }
        }

        @Override
        public int getMajorVersion() {

            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {

            return driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1)
                        throws SQLException {

            return driver.getPropertyInfo(arg0, arg1);
        }

        @Override
        public boolean jdbcCompliant() {

            return driver.jdbcCompliant();
        }

        public Logger getParentLogger() {
            return LOG;
        }
    }
    
    
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
     * @return The default username
     */
    public String getDefaultUsername() {
    
        return defaultUsername;
    }
    
    /**
     * Sets the default username to connect with
     * @param user The username to connect with
     */
    public void setDefaultUsername(String user) {
    
        if (user == null || "".equals(user) || "null".equals(user)) {
            
            this.defaultUsername = null;
        }
        else {
            
            this.defaultUsername = user;
        }
    }
    
    /**
     * @return The default JDBC driver name that will be used for new connections
     */
    public String getDefaultDriver() {
    
        return defaultDriver;
    }
    
    /**
     * Sets the default JDBC driver name to be used for new connections
     * @param driver The default driver name
     */
    public void setDefaultDriver(String driver) {
        
        if (driver == null || "".equals(driver) || "null".equals(driver)) {
            
            this.defaultDriver = null;
        }
        else {
            
            this.defaultDriver = driver;
        }
    
        this.defaultDriver = driver;
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
        
        String []locations = classpath.split(System.getProperty("path.separator"));
        
        List<URL> urls = new ArrayList<URL>();
        for (String loc : locations) {
            
            addClasspath(urls, loc);
        }
        
        classLoader = new URLClassLoader(urls.toArray(new URL[0]), this.getClass().getClassLoader());
        checkDriverAvailability();
    }
    
    /**
     * Adds a class that registers to be notified when a driver becomes
     * available.  The listener will automatically be notified of any driver
     * that is available at the time of registering.
     * 
     * @param listener The listener to add
     */
    public void addListener (SQLDriverListener listener) {
        
        for (SQLDriver driver : this.drivers.values()) {
            
            Class<? extends Driver> clazz = driver.getDriver();
            if (clazz != null) {
                
                listener.driverAvailable(this, driver);
            }
        }
        
        listeners.add(listener);
    }
    
    /**
     * @return The classloader used by the manager
     */
    public ClassLoader getClassLoader() {
        
        return classLoader;
    }
    
    /**
     * Given a classpath, parse it and return the individual entries
     * @param classpath The classpath
     * @return The parsed classpath
     * @throws IOException 
     */
    public void addClasspath (List<URL> classpath, String element) 
        throws IOException {
        
        File file = new File(element);
            
        /*
         * If the provided location was a directory, then look inside
         * of it can suck up every .jar file.
         */
        if (file.isDirectory()) {
                
            File []dirContents = file.listFiles();
            for (File subFile : dirContents) {
                
                if (subFile.isFile()
                        && subFile.getName().endsWith(".jar")) {
                    
                    classpath.add(subFile.toURI().toURL());
                }
            }
        }
        else if (file.exists()) {
                
            classpath.add(file.toURI().toURL());
        }
    }
    
    /**
     * Trys to load a driver class using the driver manager's
     * classpath.
     * 
     * @param name The name of the class to load.
     * @throws Exception Thrown if the class cannot be loaded.
     */
    public void loadClass(String name)
        throws Exception {
        
        Class.forName(name, true, classLoader);
    }
    
    /**
     * Used to check whether or not all of the currently registered
     * JDBC drivers is available in the current classloader.
     */
    private void checkDriverAvailability() {
        
        checkDriverAvailability(null);
    }
    
    /**
     * Used by a SQLDriver to notify any listeners when it becomes available.
     * @param driver The driver that is available
     */
    protected void notifyDriverAvailable (SQLDriver driver) {
        
        for (SQLDriverListener listener : listeners) {
            
            listener.driverAvailable(this, driver);
        }
    }
    
    /**
     * Check the availability of a specific driver
     * @param name The name of the driver or null if all drivers should be 
     *   checked.
     */
    protected void checkDriverAvailability(String name) {
        
        if (disabledDriverVerification) {
            
            return;
        }
        
        for (SQLDriver driver : drivers.values()) {
            
            if (name == null || driver.getName().equals(name)) {
                
                ClassLoader driverLoader = driver.getClassLoader(classLoader);
            
                try {
                    
                    Class<? extends Driver> driverClass = Class.forName(
                        driver.getDriverClass(), true, driverLoader).asSubclass(Driver.class);
                    Driver d = driverClass.newInstance();
                    DriverManager.registerDriver(new DriverShim(d));
                    
                    driver.setAvailable(driverClass);
                }
                catch (Throwable e) {
                    
                    LOG.fine("Unable to load " + driver.getDriverClass() + ": "
                        + e.getMessage());
                    driver.setAvailable(null);
                }
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
        
        return getClasspath(classLoader.getURLs());
    }
    
    /**
     * Helper function to turn a list of URL's into a classpath
     * @param urls The urls
     * @return A classpath
     */
    protected String getClasspath(URL []urls) {
        
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
     * Attempts to connect to the database utilizing a driver.
     * 
     * @param session The session for which the connection is being
     *    established. The session is primarly used as a place to
     *    send error messages.
     * @param connDesc Descriptor for the connection that is to be used to establish
     *    the connection
     * @return A newly created connection.
     * @throws SQLException Thrown if the connection could not be 
     *    established.
     */
    public SQLConnectionContext connect (Session session, ConnectionDescriptor connDesc)
        throws SQLException {
        
        SQLDriver sqlDriver = null;
        
        /*
         * If no driver was supplied, then set it to the default.
         */
        if (connDesc.getDriver() == null) {
            
            connDesc.setDriver(defaultDriver);
        }
        
        /*
         * Make sure we are going to have enough to connect with here!
         */
        if (connDesc.getDriver() == null
                && connDesc.getUrl() == null) {
            
            throw new SQLException(
                "Either an explicit JSqsh driver name must be supplied "
                + "(via --driver) or a JDBC fully qualified JDBC url "
                + "must be provided (via --jdbc-url). In most cases the "
                + "JDBC url must also be accompanied with the "
                + "name of the JDBC driver class (via --jdbc-class");
        }
        
        /*
         * We need to have a SQLDriver object because it makes our life
         * easier expanding variables. To allow for no driver being
         * supplied by the user, we will use one called "generic".
         */
        if (connDesc.getDriver() == null) {
            
            connDesc.setDriver("generic");
        }
        
        sqlDriver = getDriver(connDesc.getDriver());
        if (sqlDriver == null) {
                
            throw new SQLException("JSqsh has no driver registered under "
                + "the name '" + connDesc.getDriver()
                + "'. To see a list of available drivers, use the "
                + "\\drivers command");
        }
        
        /*
         * If the user asked for a JDBC driver class, then make sure
         * that we can load it.
         */
        if (connDesc.getJdbcClass() != null) {
            
            try {
                
                Class<? extends Driver> driverClass = Class.forName(
                    connDesc.getJdbcClass(), true, classLoader).asSubclass(Driver.class);
                Driver d = driverClass.newInstance();
                DriverManager.registerDriver(new DriverShim(d));
            }
            catch (Exception e) {
                
                throw new SQLException("Cannot load JDBC driver class '"
                    + connDesc.getJdbcClass() + "': " + e.getMessage());
            }
        }
        
        /*
         * The JDBC URL will either be one the one supplied by the user
         * or the one that is defined by the JDBC driver.
         */
        String url = connDesc.getUrl();
        if (url == null) {
            
            url = sqlDriver.getUrl();
        }
        
        /*
         * Turn our connection descriptor into properties that can be
         * referenced while expanding the URL.
         */
        Map<String, String> properties = toProperties(session, connDesc);
        Map<String, String> variables = toVariables(connDesc, sqlDriver);
        
        /*
         * Expand the url of its variables.
         */
        url = getUrl(session, properties, variables, url);
        
        Connection conn = null;
        try {
            
            Driver jdbcDriver = DriverManager.getDriver(url);
            
            /*
             * Similar to above, we'll iterate through the properties supported by
             * the driver and set them as necessary.
             */
            Properties props = new Properties();

            /*
             * If the driver explicitly declares a property 
             * we just blindly pass it in.
             */
            for (String name : sqlDriver.getPropertyNames()) {
                    
                props.put(name, session.expand(sqlDriver.getProperty(name)));
            }
            
            /*
             * If the connection descriptor specified a set of properties to use
             * then use them too (wow, we have a lot of ways to get properties
             * to the driver!)
             */
            Map<String, String> descProps = connDesc.getPropertiesMap();
            if (descProps.size() > 0) {
                
                for (Entry<String,String> e : descProps.entrySet()) {
                    
                    props.put(e.getKey(), e.getValue());
                }
            }
            
            String s = getProperty(properties,
                sqlDriver.getVariables(), SQLDriver.USER_PROPERTY);
            if (s == null) {
                
                if (defaultUsername == null) {
                    
                    s = System.getProperty("user.name");
                }
                
                if (s == null) {
                    
                    s = promptInput(session, "Username", false);
                }
                connDesc.setUsername(s);
            }
            if (s != null) {
                
                props.put(SQLDriver.USER_PROPERTY, s);
            }
            
            s = getProperty(properties, sqlDriver.getVariables(),
                SQLDriver.PASSWORD_PROPERTY);
            if (s == null) {
                
                s = promptInput(session, "Password", true);
            }
            if (s != null) {
                
                props.put(SQLDriver.PASSWORD_PROPERTY, s);
            }

            conn = DriverManager.getConnection(url, props);
            SQLTools.printWarnings(session, conn);
        }
        catch (SQLException e) {
            
            throw new SQLException(
                "Unable to connect via JDBC url '"
                + url + "': " + e.getMessage(), e.getSQLState(), 
                e.getErrorCode(), e);
        }
        
        String database = getProperty(properties,
            sqlDriver.getVariables(),  SQLDriver.DATABASE_PROPERTY);
        if (database == null && defaultDatabase != null) {
            
            database = defaultDatabase;
        }
        
        if (database != null) {
/*            
            try {
                
                conn.setCatalog(database);
                SQLTools.printWarnings(session, conn);
            }
            catch (SQLException e) {
                
                session.err.println("WARNING: Could not switch database context"
                    + " to '" + database + "': " + e.getMessage());
            }
*/
        }    
        
        try {
                
            conn.setAutoCommit(defaultAutoCommit);
            SQLTools.printWarnings(session, conn);
        }
        catch (SQLException e) {
            
            session.err.println("WARNING: Unable to set auto-commit mode to "
                + defaultAutoCommit);
        }

        /*
         * AWFUL AWFUL HACK!!!
         * In a second we will transfer variables defined by the 
         * driver via the SessionVariable setting. However, often
         * these variables will be setting information in the ConnectionContext
         * that belongs to the session -- which is likely the one we are
         * about to return, but haven't yet.  This hack temporarily
         * stuffs it into the session so it can get set, then pulls it
         * back out.
         */
        ConnectionContext oldContext = session.getConnectionContext();
        SQLConnectionContext newContext = 
            new SQLConnectionContext(session, connDesc, conn, url, 
                sqlDriver.getAnalyzer(),
                sqlDriver.getNormalizer(),
                sqlDriver.getCurrentSchemaQuery());
        session.setConnectionContext(newContext, false);

        try {

            /*
             * Now that we have our connection established, set session
             * variables that have been requested by the driver.
             */
            Iterator<String> varIter = 
                sqlDriver.getSessionVariables().keySet().iterator();
            while (varIter.hasNext()) {

                String name = varIter.next();
                session.setVariable(name, sqlDriver.getSessionVariable(name));
            }
        }
        finally {

            session.setConnectionContext(oldContext, false);
        }

        return newContext;
    }
    
    /**
     * Given the connection settings that the user provided, creates a map of
     * properties that are required by {@link SQLDriverManager#connect(Session, ConnectionDescriptor)}
     * in order to establish a connection. For a given property, such
     * as {@link SQLDriver#SERVER_PROPERTY}, the value is established by
     * the first of the following that is available:
     * 
     * <ol>
     *   <li> Using the option provided in the {@link ConnectionDescriptor}
     *   <li> Looking the in the session for variable of the name
     *        "dflt_&lt;property&gt;" (e.g. "dflt_server"). 
     * </ol>
     * 
     * If none of those is available, then the property is not passed in 
     * and it is up to the {@link SQLDriver} to provide a default.
     * 
     * @param session The session used to look up the properties.
     * @param connDesc The connection descriptor
     * @return A map of properties.
     */
    private Map<String, String> toProperties(
            Session session, ConnectionDescriptor connDesc) {
        
        Map<String, String> properties = new HashMap<String, String>();
        
        setProperty(properties, session,
            SQLDriver.SERVER_PROPERTY, connDesc.getServer());
        setProperty(properties, session,
            SQLDriver.PORT_PROPERTY, 
                (connDesc.getPort() == -1
                        ? null : Integer.toString(connDesc.getPort())));
        setProperty(properties, session,
            SQLDriver.USER_PROPERTY, connDesc.getUsername());
        setProperty(properties, session,
            SQLDriver.PASSWORD_PROPERTY, connDesc.getPassword());
        setProperty(properties, session,
            SQLDriver.DATABASE_PROPERTY, connDesc.getCatalog());
        setProperty(properties, session,
            SQLDriver.DOMAIN_PROPERTY, connDesc.getDomain());
        
        return properties;
    }

    /**
     * Merges the JDBC driver URL variables that are provided by the driver as well as by the connection
     * descriptor, with the connection descriptor taking precidence.
     * @param connDesc Connection descriptor
     * @param driver Driver
     * @return The merged variables
     */
    private Map<String, String> toVariables(ConnectionDescriptor connDesc, SQLDriver driver) {

        Map<String, String> vars = new HashMap<String, String>();
        vars.putAll(driver.getVariables());
        vars.putAll(connDesc.getUrlVariablesMap());
        return vars;
    }
    
    /**
     * Helper for toProperties() to utilize dflt_ variables to provide
     * a property name.
     * 
     * @param map The map to populate.
     * @param session The session from which dflt_ variables are fetched.
     * @param name The name of the property.
     * @param value The value of the property to set.
     */
    private void setProperty(Map<String, String>map, Session session,
            String name, String value) {
        
        if (value == null) {
            
            value = session.getVariable("dflt_" + name);
        }
        
        if (value != null) {
            
            map.put(name, value);
        }
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
     * @param session The session
     * @param prompt The prompt to use.
     * @param isMasked true if the input is to be masked.
     * @return The input
     */
    private static String promptInput(Session session, String prompt, boolean isMasked) {

        try {

            return session.getContext().getConsole().readLine(prompt, isMasked ? '*' : null);
        }
        catch (UserInterruptException e) {

            return null;
        }
        catch (EndOfFileException e) {

            return null;
        }
    }

    /**
     * Used to expand a URL of any variables that may be defined in the 
     *  user supplied properties, session, or SQLDriver variables (in that
     *  order).
     *  
     * @param properties The user supplied properties.
     * @param vars SQLDriver variables (can be null).
     * @param url The URL to expand.
     * @return The URL expanded of all variables.
     */
    private String getUrl(Session session, Map<String, String>properties,
            Map<String, String>vars, String url) {
        
        Map<String, String> connProperties = new HashMap<String, String>();
        connProperties.putAll(properties);
        
        if (vars != null) {
            
            for (String name : vars.keySet()) {
                
                String value = getProperty(properties, vars, name);
                connProperties.put(name, value);
            }
        }
        
        connProperties.put(SQLDriver.USER_PROPERTY,
            getProperty(properties, vars, SQLDriver.USER_PROPERTY));
        connProperties.put(SQLDriver.PASSWORD_PROPERTY,
            getProperty(properties, vars, SQLDriver.PASSWORD_PROPERTY));
        
        return session.expand(connProperties, url);
    }
    
    /**
     * Used internally to look up a property value following an inheritence
     * scheme of (1) properties passed by user, (2) the variables 
     * associated with the driver.
     * 
     * @param vars Variables defined by a driver (can be null).
     * @param properties The properties passed by the user.
     * @param name Variable to look up.
     * @return The value of the property.
     */
    private String getProperty(Map<String, String>properties,
            Map<String, String>vars, String name) {
        
        String value = properties.get(name);
        if (value == null) {
                
            value = vars.get(name);
        }
        
        return value;
    }
    
    /**
     * Adds a driver to the manager.
     * @param driver The new driver.
     */
    public void addDriver(SQLDriver driver) {
        
        SQLDriver orig = drivers.put(driver.getName(), driver);
        
        /*
         * This is a complete hack.  Because it is easy to get a copy of the 
         * driver.xml file in your home directory, I want to be able to "install"
         * new configuration parameters like the SQL normalizer into the copy of the
         * driver definition that the user already has defined.  So, if I see I'm
         * replacing an existing entry for the driver with a new one, then I check
         * if the new one doesn't have a normalizer defined (it is using the default),
         * so I tell the new one to use the one defined internally.
         */
        if (orig != null && driver.getNormalizer() == SQLDriver.DEFAULT_NORMALIZER) {
            
            driver.setNormalizer(orig.getNormalizer());
        }
        
        if (orig != null && driver.getCurrentSchemaQuery() == null) {
            
            driver.setCurrentSchemaQuery(orig.getCurrentSchemaQuery());
        }
        
        driver.setInternal(isLoadingInternal);
        driver.setDriverManager(this);
        checkDriverAvailability(driver.getName());
    }
    
    /**
     * Removes a driver definition from the manager
     * @param name The name of the definition to remove
     */
    public void removeDriver(String name) {
        
        SQLDriver driver = drivers.remove(name);
        if (driver != null) {
            
            driver.setDriverManager(null);
        }
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
        
        isLoadingInternal = isInternal;
        
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
        digester.addCallMethod(path, 
            "setNormalizer", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "normalizer");
            
        path = "Drivers/Driver/Classpath";
        digester.addCallMethod(path, 
            "addClasspath", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        path = "Drivers/Driver/Variable";
        digester.addCallMethod(path, 
            "setVariable", 2, new Class[] { java.lang.String.class,
                java.lang.String.class });
            digester.addCallParam(path, 0, "name");
            digester.addCallParam(path, 1);

        path = "Drivers/Driver/SessionVariable";
        digester.addCallMethod(path, 
            "setSessionVariable", 2, new Class[] { java.lang.String.class,
                java.lang.String.class });
            digester.addCallParam(path, 0, "name");
            digester.addCallParam(path, 1);
            
        path = "Drivers/Driver/Property";
        digester.addCallMethod(path, 
            "setProperty", 2, new Class[] { java.lang.String.class,
                java.lang.String.class });
            digester.addCallParam(path, 0, "name");
            digester.addCallParam(path, 1);
            
        path = "Drivers/Driver/CurrentSchemaQuery";
        digester.addCallMethod(path, 
            "setCurrentSchemaQuery", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        digester.push(this); 
        InputStream in = null;
        disabledDriverVerification = true;
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
            
            isLoadingInternal = false;
            disabledDriverVerification = false;
            checkDriverAvailability();
            
            try {
                
                in.close();
            }
            catch (IOException e) {
                
                /* IGNORED */
            }
        }
    }
    
    /**
     * Writes out information about the driver classpaths to an XML file
     * that is readable with loadDriverClasspath()
     * @param file The file to write to
     */
    public void save(File file) {
        
       PrintStream out = null;
        
        try {
            
            out = new PrintStream(new FileOutputStream(file));
            out.println("<Drivers>");
            
            for (SQLDriver driver : drivers.values()) {
                
                if (! driver.isInternal()) {
                    
                    out.println("   <Driver name=\""     + driver.getName() + "\"");
                    out.println("           url=\""      + driver.getUrl() + "\"");
                    out.println("           class=\""    + driver.getDriverClass() + "\"");
                    out.println("           target=\""   + driver.getTarget() + "\"");
                    out.println("           analyzer=\"" + driver.getAnalyzer().getClass().getName() + "\"");
                    out.println("           normalizer=\"" + driver.getNormalizer().getClass().getName() + "\">");
                    String classpath[] = driver.getClasspathArray();
                    if (classpath != null && classpath.length > 0) {
                        
                        for (int i = 0; i < classpath.length; i++) {
                            
                            out.println("      <Classpath><![CDATA[" + classpath[i] + "]]></Classpath>");
                        }
                    }
                    
                    if (driver.getCurrentSchemaQuery() != null) {
                        
                        out.print("      <CurrentSchemaQuery><![CDATA[");
                        out.print(driver.getCurrentSchemaQuery());
                        out.println("]]></CurrentSchemaQuery>");
                    }
                    
                    Map<String, String> vars = driver.getVariables();
                    for (Entry<String, String> e : vars.entrySet()) {
                        
                        out.print("      <Variable name=\"");
                        out.print(e.getKey());
                        out.print("\"><![CDATA[");
                        out.print(e.getValue());
                        out.println("]]></Variable>");
                    }
                    
                    vars = driver.getSessionVariables();
                    for (Entry<String, String> e : vars.entrySet()) {
                        
                        out.print("      <SessionVariable name=\"");
                        out.print(e.getKey());
                        out.print("\"><![CDATA[");
                        out.print(e.getValue());
                        out.println("]]></SessionVariable>");
                    }
                    
                    vars = driver.getDriverProperties();
                    for (Entry<String, String> e : vars.entrySet()) {
                        
                        out.print("      <Property name=\"");
                        out.print(e.getKey());
                        out.print("\"><![CDATA[");
                        out.print(e.getValue());
                        out.println("]]></Property>");
                    }
                    
                    out.println("   </Driver>");
                }
            }
            
            out.println("</Drivers>");
        }
        catch (IOException e) {
            
            System.err.println("WARNING: Unable to write driver classpath information to "
                + file + ": " + e.getMessage());
        }
        finally {
            
            if (out != null) {
                
                out.close();
            }
        }
    }
}
