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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The SQLContext maintains everything important about the current database
 * connection maintained within a session.
 */
public class SQLContext {
    
    /**
     * The actual SQL connection.
     */
    private Connection connection;
    
    /**
     * The JDBC url that was used to create the connection.
     */
    private String url;
    
    /**
     * The properties that were used to establish the connection.
     */
    private ConnectionDescriptor connDesc;
    
    /**
     *  The SQL analyzer that is going to be used to analyze the SQL
     *  executed through this connection.
     */
    private SQLAnalyzer analyzer;
    
    /**
     * Creates a SQLContext
     * 
     * @param connDesc The properties that were used when establishing
     *    the connection.
     * @param conn The connection 
     * @param url The JDBC URL that was used to create the connection
     * @param analyzer An analyzer for analyzing the SQL (null if no
     * analyzer is available).
     */
    public SQLContext (
            ConnectionDescriptor connDesc,
            Connection conn, String url, SQLAnalyzer analyzer) {
        
        this.connDesc = connDesc;
        this.connection = conn;
        this.url = url;
        this.analyzer = analyzer;
    }
    
    /**
     * @return the analyzer
     */
    public SQLAnalyzer getAnalyzer () {
    
        return analyzer;
    }
    
    /**
     * @param analyzer the analyzer to set
     */
    public void setAnalyzer (SQLAnalyzer analyzer) {
    
        this.analyzer = analyzer;
    }
    
    /**
     * @return the connection
     */
    public Connection getConnection () {
    
        return connection;
    }
    
    /**
     * @param connection the connection to set
     */
    public void setConnection (Connection connection) {
    
        this.connection = connection;
    }
    
    /**
     * Returns the set of properties that were used when establishing
     * the connection.
     * 
     * @return The set of properties used when establishing the 
     *   connection.
     */
    public ConnectionDescriptor getConnectionDescriptor() {
        
        return connDesc;
    }
    
    /**
     * Returns the logical name used to establish this connection.
     * 
     * @return The logical name used to establish this connection.
     */
    public String getLogicalName() {
        
        return connDesc.getName();
    }
    
    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getCatalog()
     */
    public String getCatalog () {

        return connDesc.getCatalog();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getDomain()
     */
    public String getDomain () {

        return connDesc.getDomain();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getDriver()
     */
    public String getDriver () {

        return connDesc.getDriver();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getJdbcClass()
     */
    public String getJdbcClass () {

        return connDesc.getJdbcClass();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getName()
     */
    public String getName () {

        return connDesc.getName();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getPassword()
     */
    public String getPassword () {

        return connDesc.getPassword();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getPort()
     */
    public int getPort () {

        return connDesc.getPort();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getServer()
     */
    public String getServer () {

        return connDesc.getServer();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getSid()
     */
    public String getSid () {

        return connDesc.getSid();
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getUsername()
     */
    public String getUsername () {

        return connDesc.getUsername();
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
    
    public void close() {
        
        try {
            
            connection.close();
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    

}
