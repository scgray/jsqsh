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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqsh.ConnectionDescriptor;
import org.sqsh.SQLDriver;
import org.sqsh.SQLDriverManager;
import org.sqsh.analyzers.SQLAnalyzer;
import org.sqsh.input.completion.Completer;
import org.sqsh.input.completion.DatabaseObjectCompleter;

public class SQLConnectionContext
    extends ConnectionContext {
    
    /**
     * Value to be passed to setExecutionMode() to indicate that SQL should
     * be executed directory.
     */
    public static final int EXEC_IMMEDIATE = 1;

    /**
     * Value to be passed to setExecutionMode() to indicate that SQL should
     * be executed via a prepare.
     */
    public static final int EXEC_PREPARE = 2;

    /**
     * The mode in which SQL is to be executed by default.
     */
    private int executionMode = EXEC_IMMEDIATE;
    
    /**
     * The actual SQL connection.
     */
    private Connection connection;
    
    /**
     * SQL anlayzer to use for terminator.
     */
    private SQLAnalyzer analyzer;
    
    /**
     * The JDBC url that was used to create the connection.
     */
    private String url;
    
    /**
     * I don't like the way this works, but this is normally null.  When
     * the SQLRenderer uses this connection to execute a query, it sets this
     * to the current statement handle, which allows us to cancel() it if
     * need be.
     */
    private Statement statement;
    
    /**
     * The properties that were used to establish the connection.
     */
    private ConnectionDescriptor connDesc;
    
    /**
     * Creates a ConnectionContext
     * 
     * @param connDesc The properties that were used when establishing
     *    the connection.
     * @param conn The connection 
     * @param url The JDBC URL that was used to create the connection
     * @param analyzer An analyzer for analyzing the SQL (null if no
     * analyzer is available).
     */
    public SQLConnectionContext (
            Session session,
            ConnectionDescriptor connDesc,
            Connection conn, String url, SQLAnalyzer analyzer) {
        
        super(session);
        
        this.session = session;
        this.analyzer = analyzer;
        this.connDesc = connDesc;
        this.connection = conn;
        this.url = url;
    }
    
    
    @Override
    public Style getStyle() {
        
        return new Style (session.getRendererManager().getDefaultRenderer());
    }

    @Override
    public void setStyle(Style style) {

        session.getRendererManager().setDefaultRenderer(style.getName());
    }
    
    @Override
    public void setStyle(String name) {
        
        session.getRendererManager().setDefaultRenderer(name);
    }
    
    @Override
    public boolean supportsQueryTimeout() {

        return true;
    }

    /**
     * Executes SQL on the connection.
     */
    @Override
    public void evalImpl(String batch, Session session, SQLRenderer renderer)
        throws Exception {
        
        renderer.execute(session, batch);
    }

    @Override
    public synchronized void cancel() throws Exception {
        
        if (statement != null) {
            
            statement.cancel();
        }
    }
    
    /**
     * This method is called by the SQLRenderer when it executes a query
     * on this connection, it lets us know which statement handle is in use.
     * @param statement The statement handle
     */
    public synchronized void setStatement(Statement statement) {
        
        this.statement = statement;
        
        /*
         * If a timeout was requested and it isn't going to be assisted by
         * our cancel thread, then ask the statement to cancel the query
         * when requested.
         */
        if (timeout > 0 && !isForceAssistedTimeout()) {
            
            try {
                
                statement.setQueryTimeout(timeout);
                
                /*
                 * Some drivers silently ignore the requested timeout period,
                 * so we will fall back to our own devices in this case
                 */
                if (statement.getQueryTimeout() != timeout) {
                    
                    statement.setQueryTimeout(0);
                    this.startQueryTimeout();
                }
            }
            catch (SQLException e) {
                
                /* 
                 * Couldn't set it, so we call back to assisted mode.
                 */
                this.startQueryTimeout();
            }
        }
    }
    
    /**
     * Called by the SQLRenderer when its query is complete
     */
    public synchronized void clearStatement() {
        
        this.statement = null;
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
     * Retrieves the method in which SQL should be executed by default.
     * This can return a value of EXEC_IMMEDIATE (the default) or
     * EXEC_PREPARE.
     *
     * @return the execution mode.
     */
    public int getExecutionMode() {

        return executionMode;
    }

    /**
     * Sets the mode in which SQL will be executed by default.
     *
     * @param mode The mode of execution. This can be EXEC_IMMEDIATE
     *    or EXEC_PREPARE.
     */
    public void setExecutionMode(int mode) {

        if (mode != EXEC_IMMEDIATE && mode != EXEC_PREPARE) {

            throw new java.lang.IllegalArgumentException("Execution mode "
                + "must be EXEC_IMMEDIATE or EXEC_PREPARE");
        }

        executionMode = mode;
    }

    /**
     * Sets the mode in which SQL will be executed by default.
     *
     * @param mode The mode of execution. This can be the string
     *    "immediate" or "prepare".
     */
    public void setExecutionModeName(String mode) {

        if ("immediate".equalsIgnoreCase(mode)) {

            executionMode = EXEC_IMMEDIATE;
        }
        else if ("prepare".equalsIgnoreCase(mode)) {

            executionMode = EXEC_PREPARE;
        }
        else {

            throw new java.lang.IllegalArgumentException("Execution mode "
                + "must be 'immediate' or 'prepare'");
        }
    }

    public String getExecutionModeName() {

        if (executionMode == EXEC_PREPARE) {

            return "prepare";
        }

        return "immediate";
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

        return getProperty(connDesc.getCatalog(), SQLDriver.DATABASE_PROPERTY);
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getDomain()
     */
    public String getDomain () {

        return getProperty(connDesc.getDomain(), SQLDriver.DOMAIN_PROPERTY);
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

        return getProperty(connDesc.getPassword(), SQLDriver.PASSWORD_PROPERTY);
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getPort()
     */
    public int getPort () {

        int port = connDesc.getPort();
        if (port > 0) {
            
            return port;
        }
        
        String prop = getProperty(null, SQLDriver.PORT_PROPERTY);
        if (prop == null) {
            
            prop = "-1";
        }
        
        return Integer.parseInt(prop);
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getServer()
     */
    public String getServer () {

        return getProperty(connDesc.getServer(), SQLDriver.SERVER_PROPERTY);
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getSid()
     */
    public String getSid () {

        return getProperty(connDesc.getSid(), SQLDriver.SID_PROPERTY);
    }

    /**
     * @return
     * @see org.sqsh.ConnectionDescriptor#getUsername()
     */
    public String getUsername () {

        return getProperty(connDesc.getUsername(), SQLDriver.USER_PROPERTY);
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
    
    
    @Override
    public boolean isTerminated(String batch, char terminator) {

        return analyzer.isTerminated(batch, terminator);
    }
    

    @Override
    public Completer getTabCompleter(Session session, String line,
                    int position, String word) {

        return new DatabaseObjectCompleter(session, line, position, word);
    }

    @Override
    public String toString() {
        
        return url;
    }
    
    @Override
    public void close() {
        
        try {
            
            connection.close();
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * Used to retrieve connection values that were provided by the driver
     * definition and not necessarily explicitly provided by connection 
     * descriptor itself
     * 
     * @param descValue The value contained in the connection descriptor. If
     *   not null, this is returned.
     * @param driverVariable The driver variable that will provide the value
     *   if the connection descriptor value is null
     * @return The value that was used to connect or null if not provided
     */
    private String getProperty(String descValue, String driverVariable) {
        
        if (descValue != null) {
            
            return descValue;
        }
        
        if (connDesc.getDriver() != null) {
            
            SQLDriverManager manager = session.getDriverManager();
            SQLDriver driver = manager.getDriver(connDesc.getDriver());
            
            if (driver != null) {
                
                return driver.getVariable(driverVariable);
            }
        }
        
        return null;
    }
}
