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
package org.sqsh;

import org.sqsh.options.OptionProperty;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

/**
 * Container for a block of settings that are used for connecting to a specific database platform. This object is
 * also a self-describing {@link SqshOptions} and as such can be populated using the {@link org.sqsh.options.OptionProcessor}.
 */
public class ConnectionDescriptor extends SqshOptions implements Cloneable, Comparable<ConnectionDescriptor> {

    @OptionProperty(
        option='S', longOption="server", arg=REQUIRED, argName="server",
        description="Name of the database server to connect to")
     public String server = null;

    @OptionProperty(
        option='p', longOption="port", arg=REQUIRED, argName="port",
        description="Listen port for the server to connect to")
    public int port = -1;

    @OptionProperty(
        option='D', longOption="database", arg=REQUIRED, argName="db",
        description="Database (catalog) context to use upon connection")
    public String database = null;

    @OptionProperty(
        option='U', longOption="user", arg=REQUIRED, argName="user",
        description="Username utilized for connection")
    public String username = null;

    @OptionProperty(
        option='P', longOption="password", arg=REQUIRED, argName="pass",
        description="Password utilized for connection")
    public String password = null;
    
    @OptionProperty(
        option='w', longOption="domain", arg=REQUIRED, argName="domain",
        description="Windows domain to be used for authentication")
    public String domain = null;

    @OptionProperty(
        option='O', longOption="prop", arg=REQUIRED, argName="name=val",
        description="Set a driver connection property. Can be used more than once")
    public List<String> properties = new ArrayList<String>();

    @OptionProperty(
            option='V', longOption="url-var", arg=REQUIRED, argName="name=val",
            description="Set a driver URL variable. Can be used more than once")
    public List<String> urlVariables = new ArrayList<String>();

    @OptionProperty(
        option='c', longOption="jdbc-class", arg=REQUIRED, argName="driver",
        description="JDBC driver class to utilize")
    public String driverClass = null;
    
    @OptionProperty(
        option='d', longOption="driver", arg=REQUIRED, argName="driver",
        description="Name of jsqsh driver to be used for connection")
    public String driverName = null;
    
    @OptionProperty(
        option='u', longOption="jdbc-url", arg=REQUIRED, argName="url",
        description="JDBC url to use for connection")
    public String url = null;
    
    @OptionProperty(
        option='A', longOption="autoconnect", arg=NONE, argName="bool",
        description="Allows jsqsh to automatically attempt to connect")
    public boolean toggleAutoconnect = false;
    
    public boolean autoconnect;
    
    /**
     * The name cannot be set with a flag.
     */
    public String name;

    /**
     * Properties as a name=value map
     */
    private Map<String,String> propMap = null;

    /**
     * URL variables as a name=value map
     */
    private Map<String,String> urlVarMap = null;

    public ConnectionDescriptor() {
        this(null);
    }

    /**
     * Creates a named connection descriptor.
     * 
     * @param name Name of the descriptor.
     */
    public ConnectionDescriptor (String name) {
        this.name = name;
    }
    
    /**
     * Creates a copy of the object.
     */
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            /* IGNORED */
        }
        
        return null;
    }
    
    /**
     * Given a {@link SQLDriver} property name, returns the value of that given property for this connection
     * descriptorr.
     *
     * @param name The name of the property (e.g. "user"))
     * @return The value of the property, if set, or null if not set
     */
    public String getValueOf(String name) {
        if (name.equals(SQLDriver.DATABASE_PROPERTY)) {
            return getCatalog();
        }
        if (name.equals(SQLDriver.DOMAIN_PROPERTY)) {
            return getDomain();
        }
        if (name.equals(SQLDriver.PASSWORD_PROPERTY)) {
            return getPassword();
        }
        if (name.equals(SQLDriver.PORT_PROPERTY)) {
            if (getPort() < 0) {
                return null;
            }
            return Integer.toString(getPort());
        }
        if (name.equals(SQLDriver.SERVER_PROPERTY)) {
            return getServer();
        }
        if (name.equals(SQLDriver.USER_PROPERTY)) {
            return getUsername();
        }

        return getUrlVariablesMap().get(name);
    }
    
    /**
     * Sets the value of an attribute of the descriptor by name
     *
     * @param name The name of the property (e.g. "user")
     * @param value THe value to set
     */
    public void setValueOf(String name, String value) {
        if (value != null && value.length() == 0) {
            value = null;
        }

        if (name.equals(SQLDriver.DATABASE_PROPERTY)) {
            setCatalog(value);
        }
        else if (name.equals(SQLDriver.DOMAIN_PROPERTY)) {
            setDomain(value);
        }
        else if (name.equals(SQLDriver.PASSWORD_PROPERTY)) {
            setPassword(value);
        }
        else if (name.equals(SQLDriver.PORT_PROPERTY)) {
            try {
                setPort(Integer.parseInt(value));
            }
            catch (NumberFormatException e) {
                /* What to do? */
            }
        }
        else if (name.equals(SQLDriver.SERVER_PROPERTY)) {
            setServer(value);
        }
        else if (name.equals(SQLDriver.USER_PROPERTY)) {
            setUsername(value);
        }
        else {
            setUrlVariable(name, value);
        }
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
     * @return the driver
     */
    public String getDriver () {
        return driverName;
    }
    
    /**
     * @param driver the driver to set
     */
    public void setDriver (String driver) {
        this.driverName = driver;
    }
    
    /**
     * @return the server
     */
    public String getServer () {
        return server;
    }
    
    /**
     * @param server the server to set
     */
    public void setServer (String server) {
        this.server = server;
    }

    /**
     * @return the port
     */
    public int getPort () {
        return port;
    }
    
    /**
     * @param port the port to set
     */
    public void setPort (int port) {
        this.port = port;
    }
    
    /**
     * @return the catalog
     */
    public String getCatalog () {
        return database;
    }
    
    /**
     * @param catalog the catalog to set
     */
    public void setCatalog (String catalog) {
        this.database = catalog;
    }
    
    /**
     * @return the username
     */
    public String getUsername () {
        return username;
    }
    
    /**
     * @param username the username to set
     */
    public void setUsername (String username) {
        this.username = username;
    }
    
    /**
     * @return the password
     */
    public String getPassword () {
        return password;
    }
    
    /**
     * @param password the password to set
     */
    public void setPassword (String password) {
        this.password = password;
    }
    
    /**
     * Returns an "encrypted" form of the password.
     * 
     * @return An ecnrypted form of the password.
     */
    public String getEncryptedPassword() {
        return Base64.getEncoder().encodeToString(Crypto.encrypt(password.getBytes()));
    }
    
    /**
     * @return True if this connection is set to be used by jsqsh to 
     *    automatically connect with upon startup.
     */
    public boolean isAutoconnect() {
        return autoconnect;
    }

    /**
     * Sets whether or not this connection will automatically be used
     * to establish a connection when jsqsh starts. Enabling autoconnect
     * for one connection, turns it off for any other connection definition
     * that had it set.
     * 
     * @param autoconnect True if autoconnect is enabled.
     */
    public void setAutoconnect(boolean autoconnect) {
        this.autoconnect = autoconnect;
    }

    /**
     * Sets the password.
     * 
     * @param password The password.
     * @param isEncrypted If true, the password that is supplied is
     *   encrypted via a call to getEncryptedPassword().
     */
    public void setPassword(String password, boolean isEncrypted) {
        if (isEncrypted) {
            this.password = new String(Crypto.decrypt(Base64.getDecoder().decode(password)));
        }
        else {
            this.password = password;
        }
    }
    
    /**
     * @return the jdbcClass
     */
    public String getJdbcClass () {
    return driverClass;
    }
    
    /**
     * @param jdbcClass the jdbcClass to set
     */
    public void setJdbcClass (String jdbcClass) {
        this.driverClass = jdbcClass;
    }
    
    /**
     * @return the domain
     */
    public String getDomain () {
        return domain;
    }
    
    /**
     * @param domain the domain to set
     */
    public void setDomain (String domain) {
        this.domain = domain;
    }
    
    /**
     * @return the url
     */
    public String getUrl () {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl (String url) {
        this.url = url;
    }
    
    /**
     * @param properties New connection properties to use
     */
    public void setProperties(List<String> properties) {
        this.propMap = null;
        this.properties = properties;
    }

    /**
     * Sets a property on the descriptor.
     *
     * @param name The name of the property
     * @param value The value of the property
     */
    public void setProperty(String name, String value) {
        this.propMap = null;
        setVariable(properties, name, value);
    }
    
    /**
     * Removes a property from the connection descriptor.
     *
     * @param name The property to remove
     */
    public void removeProperty(String name) {
        
        this.propMap = null;
        removeVariable(properties, name);
    }
    
    /**
     * @param properties New connection properties to use
     */
    public void addProperties(List<String> properties) {
        propMap = null;
        addVariables(this.properties, properties);
    }

    /**
     * @return The set of properties for this connection
     */
    public List<String> getProperties() {
        return this.properties;
    }
    
    /**
     * @return The set of properties that should be passed along to the JDBC driver during connect.
     */
    public Map<String,String> getPropertiesMap() {
        if (propMap == null) {
            propMap = getValues(properties);
        }
        
        return propMap;
    }

    /**
     * Sets a JDBC URL variable on the descriptor.
     *
     * @param name The name of the variable
     * @param value The value of the variable
     */
    public void setUrlVariable(String name, String value) {
        this.urlVarMap = null;
        setVariable(urlVariables, name, value);
    }

    /**
     * Removes a JDBC URL variable from the connection descriptor.
     *
     * @param name The variable to remove
     */
    public void removeUrlVariable(String name) {
        this.urlVarMap = null;
        removeVariable(urlVariables, name);
    }

    /**
     * @param urlVariables Additional URL variables to add
     */
    public void addUrlVariables(List<String> urlVariables) {
        urlVarMap = null;
        addVariables(this.urlVariables, urlVariables);
    }

    /**
     * @return The set of properties for this connection
     */
    public List<String> getUrlVariables() {
        return this.urlVariables;
    }

    /**
     * @return The set of variables that are to be used when expanding variables in the URL of JDBC driver during connect.
     */
    public Map<String,String> getUrlVariablesMap() {
        if (urlVarMap == null) {
            urlVarMap = getValues(urlVariables);
        }

        return urlVarMap;
    }

    /**
     * Converts a list of "name=value" pairs into a hash map.
     *
     * @param pairs The list of "name=value" pairs.
     * @return The newly created hash map
     */
    protected static Map<String,String> getValues(List<String> pairs) {
        Map<String, String> map = new HashMap<>();
        if (pairs == null || pairs.size() == 0) {
            return map;
        }

        for (String v: pairs) {
            int idx = v.indexOf('=');
            String name;
            String value = null;
            
            if (idx < 0) {
                name = v;
            }
            else {
                name  = v.substring(0, idx);
                if (idx < (v.length() - 1)) {
                    value = v.substring(idx+1);
                }
            }
            
            map.put(name, value);
        } 
        
        return map;
    }
    
    /**
     * Given a list of strings of the form "name=value" adds or replaces and existing entry.
     *
     * @param variables The list of name=value pairs
     * @param name The name to add
     * @param value the value to add
     */
    private static void setVariable(List<String> variables, String name, String value) {
        String lead = name + "=";
        for (int i = 0; i < variables.size(); i++) {
            if (variables.get(i).startsWith(lead)) {
                variables.remove(i);
                break;
            }
        }
        variables.add(name + "=" + value);
    }

    /**
     * Given a two lists of "name=value" pairs, adds the values in the existing list from the toBeAdded
     * list, replacing an existing variable by the same name along the way.
     *
     * @param existing Existing list
     * @param toBeAdded List to be added
     */
    public static void addVariables(List<String> existing, List<String> toBeAdded) {

        for (String nameValue : toBeAdded) {
            String name;
            String value = null;
            int idx = nameValue.indexOf("=");
            if (idx < 0) {
                name = nameValue;
            }
            else if (idx == 0) {
                name = "null";
                value = nameValue;
            }
            else {
                name = nameValue.substring(0, idx);
                value = nameValue.substring(idx+1);
            }

            setVariable(existing, name, value);
        }
    }

    /**
     * Removes a variable name from a list of "name=value" pairs.
     *
     * @param variables The list of "name=value" pairs
     * @param name The name to remove
     */
    private static void removeVariable(List<String> variables, String name) {
        String lead = name + "=";
        for (int i = 0; i < variables.size(); i++) {
            if (variables.get(i).startsWith(lead)) {
                variables.remove(i);
                break;
            }
        }
    }

    @Override
    public int hashCode () {
        return name.hashCode();
    }

    @Override
    public boolean equals (Object obj) {
        if (obj instanceof String) {
            return obj.equals(name);
        }
        else if (obj instanceof ConnectionDescriptor) {
            return (((ConnectionDescriptor)obj).name.equals(name));
        }
        
        return false;
    }
    
    @Override
    public int compareTo(ConnectionDescriptor o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("server=").append(server);
        sb.append(", port=").append(port);
        sb.append(", database=").append(database);
        sb.append(", username=").append(username);
        sb.append(", password=").append(password);
        sb.append(", domain=").append(domain);
        sb.append(", driverClass=").append(driverClass);
        sb.append(", driverName=").append(driverName);
        sb.append(", url=").append(url);
        sb.append(", name=").append(name);
        sb.append(", properties=[");
        for (int i = 0; i < properties.size(); i++)
        {
            if (i > 0)
                sb.append(',');
            sb.append(properties.get(i));
        }
        sb.append(']');
        sb.append(", urlVariables=[");
        for (int i = 0; i < urlVariables.size(); i++)
        {
            if (i > 0)
                sb.append(',');
            sb.append(urlVariables.get(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
