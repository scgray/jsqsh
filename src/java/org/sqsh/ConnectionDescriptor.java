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

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

import org.sqsh.options.Option;

/**
 * Container for a block of settings that are used for connecting
 * to a specific database platform.  This object is also a self-
 * describing {@link SqshOption} and as such can be populated
 * using the {@link org.sqsh.options.OptionProcessor}.
 */
public class ConnectionDescriptor
    extends SqshOptions
    implements Cloneable {
    
    
    @Option(
        option='S', longOption="server", arg=REQUIRED, argName="server",
        description="Name of the database server to connect to")
     public String server = null;

    @Option(
        option='p', longOption="port", arg=REQUIRED, argName="port",
        description="Listen port for the server to connect to")
    public int port = -1;

    @Option(
        option='D', longOption="database", arg=REQUIRED, argName="db",
        description="Database (catalog) context to use upon connection")
    public String database = null;

    @Option(
        option='U', longOption="user", arg=REQUIRED, argName="user",
        description="Username utilized for connection")
    public String username = null;

    @Option(
        option='P', longOption="password", arg=REQUIRED, argName="pass",
        description="Password utilized for connection")
    public String password = null;
    
    @Option(
        option='w', longOption="domain", arg=REQUIRED, argName="domain",
        description="Windows domain to be used for authentication")
    public String domain = null;

    @Option(
        option='s', longOption="sid", arg=REQUIRED, argName="SID",
        description="Instance id (e.g. Oracle SID) to utilize")
    public String SID = null;

    @Option(
        option='c', longOption="jdbc-class", arg=REQUIRED, argName="driver",
        description="JDBC driver class to utilize")
    public String driverClass = null;
    
    @Option(
        option='d', longOption="driver", arg=REQUIRED, argName="driver",
        description="Name of jsqsh driver to be used for connection")
    public String driverName = null;
    
    @Option(
        option='u', longOption="jdbc-url", arg=REQUIRED, argName="url",
        description="JDBC url to use for connection")
    public String url = null;
    
    @Option(
        option='N', longOption="name", arg=REQUIRED, argName="name",
        description="Name to apply to connection")
    public String name = null;
    
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
              && nullEquals(this.username, that.username));
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
    
}
