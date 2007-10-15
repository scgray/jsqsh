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
     *  The SQL analyzer that is going to be used to analyze the SQL
     *  executed through this connection.
     */
    private SQLAnalyzer analyzer;
    
    /**
     * Creates a SQLContext
     * 
     * @param conn The connection 
     * @param url The JDBC URL that was used to create the connection
     * @param analyzer An analyzer for analyzing the SQL (null if no
     * analyzer is available).
     */
    public SQLContext (Connection conn, String url, SQLAnalyzer analyzer) {
        
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
