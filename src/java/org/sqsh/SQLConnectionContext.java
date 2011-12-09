package org.sqsh;

import java.sql.Connection;
import java.sql.SQLException;

import org.sqsh.analyzers.SQLAnalyzer;

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
            ConnectionDescriptor connDesc,
            Connection conn, String url, SQLAnalyzer analyzer) {
        
        this.analyzer = analyzer;
        this.connDesc = connDesc;
        this.connection = conn;
        this.url = url;
    }
    
    /**
     * Executes SQL on the connection.
     */
    @Override
    public void eval(String batch, Session session, SQLRenderer renderer)
        throws Exception {
        
        renderer.execute(session, batch);
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
    
    
    @Override
    public boolean isTerminated(String batch, char terminator) {

        return analyzer.isTerminated(batch, terminator);
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
}
