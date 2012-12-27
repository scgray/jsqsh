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

import static org.sqsh.options.ArgumentRequired.REQUIRED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sqsh.options.OptionProperty;

/**
 * Container for a block of settings that are used for connecting
 * to a specific database platform.  This object is also a self-
 * describing {@link SqshOption} and as such can be populated
 * using the {@link org.sqsh.options.OptionProcessor}.
 */
public class ConnectionDescriptor
    extends SqshOptions
    implements Cloneable {
    
    
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
        option='s', longOption="sid", arg=REQUIRED, argName="SID",
        description="Instance id (e.g. Oracle SID) to utilize")
    public String SID = null;
    
    @OptionProperty(
        option='O', longOption="prop", arg=REQUIRED, argName="name=val",
        description="Set a driver connection property. Can be used more than once")
    public List<String> properties = new ArrayList<String>();

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
    
    /**
     * The name cannot be set with a flag.
     */
    public String name = null;
    
    private Map<String,String> propMap = null;
    
    /**
     * Creates an empty connection descriptor.
     */
    public ConnectionDescriptor() {
        
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
        
        return Base64.encodeBytes(
            Crypto.encrypt(password.getBytes()));
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
            
            this.password = 
                new String(Crypto.decrypt(Base64.decode(password)));
        }
        else {
            
            this.password = password;
        }
    }
    
    /**
     * @return the sid
     */
    public String getSid () {
    
        return SID;
    }
    
    /**
     * @param sid the sid to set
     */
    public void setSid (String sid) {
    
        this.SID = sid;
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
     * Add a property to the descriptor.
     * @param name The name
     * @param value The value
     */
    public void addProperty(String name, String value) {
        
        this.propMap = null;
        properties.add(name + "=" + value);
    }
    
    /**
     * @param properties New connection properties to use
     */
    public void addProperties(List<String> properties) {
        
        propMap = null;
        for (String prop : properties) {
            
            if (!this.properties.contains(prop)) {
                
                this.properties.add(prop);
            }
        }
    }
    
    /**
     * @return The set of properties for this connection
     */
    public List<String> getProperties() {
        
        return this.properties;
    }
    
    /**
     * @return The set of properties that should be passed along to the
     *   JDBC driver during connect.
     */
    public Map<String,String> getPropertiesMap() {
        
        if (propMap == null) {
            
            propMap = getValues(properties);
        }
        
        return propMap;
    }
    
    /**
     * Converts a list of "name=value" pairs into a hash map.
     * @param pairs The list of "name=value" pairs.
     * @return The newly created hash map
     */
    protected Map<String,String> getValues(List<String> pairs) {
        
        Map<String, String> map = new HashMap<String, String>();
        if (pairs == null || pairs.size() == 0) {
            
            return map;
        }
        
        for (int i = 0; i < pairs.size(); i++) {
            
            String v = pairs.get(i);
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
     * Returns true if this descriptor matches that descriptor.
     * 
     * @param that The descriptor to compare to.
     * @return true if they are completely identical.
     */
    public boolean isIdentical(ConnectionDescriptor that) {
        
        return (nullEquals(this.database, that.database)
              && nullEquals(this.domain, that.domain)
              && nullEquals(this.driverClass, that.driverClass)
              && nullEquals(this.driverName, that.driverName)
              && nullEquals(this.name, that.name)
              && nullEquals(this.password, that.password)
              && this.port == that.port
              && nullEquals(this.server, that.server)
              && nullEquals(this.SID, that.SID)
              && nullEquals(this.url, that.url)
              && nullEquals(this.username, that.username)
              && this.properties.equals(that.properties));
    }
    
    /**
     * Helper to compare two objects that could be null.
     * 
     * @param o1 First object to compare.
     * @param o2 Second object to compare.
     * @return True if they are equal.
     */
    private boolean nullEquals(Object o1, Object o2) {
        
        return ((o1 == null && o2 == null)
                || (o1 != null && o2 != null && o1.equals(o2)));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode () {

        return name.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals (Object obj) {

        if (obj instanceof String) {
            
            return ((String)obj).equals(name);
        }
        else if (obj instanceof ConnectionDescriptor) {
            
            return (((ConnectionDescriptor)obj).name.equals(name));
        }
        
        return false;
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
        sb.append(", SID=").append(SID);
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
        return sb.toString();
    }
}
