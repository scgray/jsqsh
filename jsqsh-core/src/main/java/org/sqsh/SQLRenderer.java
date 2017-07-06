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

import org.sqsh.signals.CancelingSignalHandler;
import org.sqsh.signals.SignalManager;
import org.sqsh.util.TimeUtils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class SQLRenderer {
    
    private static final Logger LOG = Logger.getLogger(SQLRenderer.class.getName());
    
    SqshContext sqshContext;
    
    /**
     * Used by setRowLimitMethod() to indicate that the result set size
     * should be limited by the JDBC driver's {@link Statement#setMaxRows(int)}
     * method. For some implementations this method has side effects such
     * as limiting number of rows affected by an update/delete as well.
     */
    public static final int LIMIT_DRIVER  = 1;
    
    /**
     * Used by setRowLimitMethod() to indicate that result set size
     * should be limited by canceling the current query. This method
     * can have negative effects when calling stored procedures or
     * SQL that returns multiple result sets as it will completely stop
     * execution of the entire batch.
     */
    public static final int LIMIT_CANCEL  = 2;
    
    /**
     * Used by setRowLimitMethod() to indicate that result set size
     * should be limited by silently discarding rows beyond the specified
     * row limit. This is the safest method but can take extra time to 
     * process and discard unwanted rows.
     */
    public static final int LIMIT_DISCARD = 3;
    
    
    /**
     * If set to true, then result set metadata 
     */
    private boolean showMetadata = false;
    
    /**
     * Whether or not variable expansion will take place.
     */
    private boolean expand = false;
    
    /**
     * The maximum number of rows that will be rendered. A value <= 0
     * will provide unlimited output.
     */
    private int maxRows = 0;
    
    /**
     * Specifies the mechanism that will be used to limit the rowcount
     * specified by {@link #setMaxRows(int)}.
     */
    private int rowLimitMethod = LIMIT_DISCARD;
    
    /**
     * This causes update counts to be discarded rather than displayed. It
     * can help clean up certain output.
     */
    private boolean noCount = false;
    
    /**
     * Controls whether or not jsqsh will display query timing information
     * for each query executed.
     */
    private boolean showTimings = true;
    
    /**
     * This is to support buggy drivers (umm, the Apache Hive driver. Wow
     * does it have a lot of boogs) that do not return a proper -1 updateCount()
     * when they are really finished with results. Setting this to a value above
     * zero indicates that jsqsh should stop processing after this many updateCounts
     * have been received.
     */
    private int maxUpdateCount = 0;
    
    private long startTime;
    private long firstRowTime;
    private long endTime;
    
    /**
     * Creates a renderer.
     */
    public SQLRenderer (SqshContext context) {
        
        this.sqshContext = context;
    }

    /**
     * @return whether or not the SQL will have variable expansion
     *    performed upon it before execution.
     */
    public boolean isExpand () {
    
        return expand;
    }
    
    /**
     * @param expand whether or not the SQL will have variable expansion
     *    performed upon it before execution.
     */
    public void setExpand (boolean expand) {
    
        this.expand = expand;
    }
    
    /**
     * This is to support buggy drivers (umm, the Apache Hive driver. Wow
     * does it have a lot of boogs) that do not return a proper -1 updateCount()
     * when they are really finished with results. Setting this to a value above
     * zero indicates that jsqsh should stop processing after this many updateCounts
     * have been received.
     */
    public void setMaxUpdateCount(int maxUpdateCount) {
    
        this.maxUpdateCount = maxUpdateCount;
    }
    
    /**
     * @return The current maximum update count.  A value &lt;= 0 indicates that
     *  there is no limit
     */
    public int getMaxUpdateCount() {
    
        return maxUpdateCount;
    }

    /**
     * @return whether or not result set metadata is displayed.
     */
    public boolean isShowMetadata () {
    
        return showMetadata;
    }
    
    /**
     * @param showMetadata Sets whether or not result set metadata is
     *   displayed as a separate result set.
     */
    public void setShowMetadata (boolean showMetadata) {
    
        this.showMetadata = showMetadata;
    }
    
    /**
     * Returns whether or not jsqsh will display query timing
     * information.
     * 
     * @return true if timings are being displayed
     */
    public boolean isShowTimings () {
    
        return showTimings;
    }

    
    /**
     * Sets whether or not jsqsh will display query timing
     * information.
     * 
     * @param showTimings whether or not to display timing information.
     */
    public void setShowTimings (boolean showTimings) {
    
        this.showTimings = showTimings;
    }

    /**
     * Returns the maximum number of rows that will be displayed. A value
     * &lt;= indicates unlimited results.
     * 
     * @return the maximum number of rows that will be displayed.
     */
    public int getMaxRows () {
    
        return maxRows;
    }

    /**
     * Returns the maximum number of rows that will be displayed. A value
     * &lt;= indicates unlimited results.
     * @param maxRows the maximum number of rows that will be displayed.
     */
    public void setMaxRows (int maxRows) {
    
        this.maxRows = maxRows;
    }
    
    /**
     * @return Whether or not empty update counts are displayed.
     */
    public boolean isNoCount () {
    
        return noCount;
    }

    
    /**
     * Sets whether or not empty update counts will be displayed. These
     * are the "(152 rows affected)" messages that are generated due to
     * updates and deletes. This has no effect on the messages that are
     * generated due to row results.
     * 
     * @param noCount true if update counts are not to be displayed.
     */
    public void setNoCount (boolean noCount) {
    
        this.noCount = noCount;
    }

    /**
     * @return The current mechanism to be used to limit rows specified
     * by {@link #setMaxRows(int)}.
     */
    public int getRowLimitMethod () {
    
        return rowLimitMethod;
    }
    
    /**
     * @return Returns the mechanism used to limit result rows as a displayable
     *      string.
     */
    public String getRowLimitMethodName() {
        
        switch (rowLimitMethod) {
            
            case LIMIT_DRIVER: return "driver";
            case LIMIT_CANCEL: return "cancel";
            default:
                return "discard";
        }
    }

    /**
     * @param rowLimitMethod Sets the mechanism to be used to limit 
     * rows specified by {@link #setMaxRows(int)}. Valid values are
     * {@link #LIMIT_CANCEL}, {@link #LIMIT_DISCARD}, or {@link #LIMIT_DRIVER}.
     */
    public void setRowLimitMethod (int rowLimitMethod) {
    
        if (rowLimitMethod != LIMIT_CANCEL
                && rowLimitMethod != LIMIT_DRIVER
                && rowLimitMethod != LIMIT_DISCARD) {
            
            throw new IllegalArgumentException("Invalid method ("
                + rowLimitMethod + ")");
        }
        
        this.rowLimitMethod = rowLimitMethod;
    }
    
    /**
     * Sets the row limiting method.
     * @param name The name of the method. Valid values are "cancel",
     * "discard", or "driver".
     */
    public void setRowLimitMethodName (String name) {
        
        name = name.toLowerCase();
        if ("cancel".equals(name)) {
            
            setRowLimitMethod(LIMIT_CANCEL);
        }
        else if ("discard".equals(name)) {
            
            setRowLimitMethod(LIMIT_DISCARD);
        }
        else if ("driver".equals(name)) {
            
            setRowLimitMethod(LIMIT_DRIVER);
        }
        else {
            
            throw new IllegalArgumentException("Invalid method ("
                + name + "): Valid values are 'cancel', 'discard', "
                + "or 'driver'");
        }
    }

    /**
     * Executes a SQL statement using the default renderer.
     * @param session The session to be used as an output handle
     *  (and where the database connection comes from).
     * @param sql  The SQL to execute
     * 
     * @return true if the SQL executed without error (warnings do not
     *   count towards errors), false if there was at least one error
     *   raised during the execution of the SQL.
     *   
     * @throws SQLException Thrown when...well, you know.
     */
    public boolean execute (Session session, String sql)
        throws SQLException {
        
        Renderer renderer = session.getContext().getRendererManager()
            .getRenderer(session);
        
        return execute(renderer, session, sql);
    }
    
    /**
     * Executes and displays the results from a prepared statement.
     * 
     * @param session The session that will be used for output.
     * @param sql A prepared statement that has had all of its
     *    SQL and parameters provided.
     * 
     * @return true if the SQL executed without error (warnings do not
     *   count towards errors), false if there was at least one error
     *   raised during the execution of the SQL.
     *   
     * @throws SQLException Thrown if there is an issue.
     */
    @SuppressWarnings("resource")
    public boolean executeCall (Session session, String sql)
        throws SQLException {
        
        boolean ok = true;
        Renderer renderer = session.getContext().getRendererManager()
            .getRenderer(session);
        
        CallableStatement statement = null;
        
        Connection conn = session.getConnection();
        CancelingSignalHandler sigHandler = null;
        SignalManager sigMan = SignalManager.getInstance();
        DataFormatter formatter = session.getDataFormatter();
        
        if (conn == null) {
            
            throw new SQLException("No database connection has been established");
        }
        
        SQLConnectionContext sqlContext = 
            (SQLConnectionContext) session.getConnectionContext();
        
        try {
            
            /*
             * Start the visual timer. This will do nothing if the facility has
             * not been enabled.  Note that the facility is stopped on error
             * or when the first row has been received.
             */
            session.startVisualTimer();
            
            /*
             * Cannot pass by reference, so fake it with an array
             */
            boolean []hasReturnMarker = new boolean[1];
            hasReturnMarker[0] = false;
            
            /*
             * Parse out a { call .. } statement, looking for inout parameters
             * like "?=10" and returning the values for those parameters.
             */
            List<String> inoutParameterValues = new ArrayList<String>();
            sql = getInoutParameterValues(sql, inoutParameterValues, hasReturnMarker);
            int inoutParamIdx = 0;
            
            statement = conn.prepareCall(sql);
            
            /*
             * Fetch information for the parameters.  Any out or inout parameter
             * needs to be "registered" to let the server know we are interested
             * in seeing the value of that parameter.
             */
            ParameterMetaData paramMeta = statement.getParameterMetaData();
            int nParams = paramMeta.getParameterCount();
            List<Integer> outputParams = new ArrayList<Integer>();
            for (int i = 1; i <= nParams; i++) {
                
                int paramMode = paramMeta.getParameterMode(i);
                
                // System.out.println("parameter #" 
                //   + i + " = " + paramMeta.getParameterTypeName(i));
                
                if (paramMode == ParameterMetaData.parameterModeOut
                    || paramMode == ParameterMetaData.parameterModeInOut) {
                    
                    outputParams.add(i);
                    
                    /*
                     * If the parameter type is "OTHER", then it may be a cursor. This
                     * is the suck because there is no standard for CURSOR data type :(
                     * So, I have hard coded logic to deal with how different drivers 
                     * handle cursor.
                     */
                    int paramType = paramMeta.getParameterType(i);
                    if (paramType == Types.OTHER) {
                        
                        /*
                         * TODO: I really want to make handling of "other" data types
                         * pluggable by driver. But, at the moment I'm lazy and have
                         * some irritating time constraints, so hard coded for now.
                         */
                        
                        String paramTypeName = paramMeta.getParameterTypeName(i);
                        
                        if (paramTypeName.equals("CURSOR")) {
                            
                            String driverName = sqlContext.getConnectionDescriptor().getDriver();
                            if (driverName.startsWith("db2")) {
                                
                                statement.registerOutParameter(i, SqshTypes.DB2_CURSOR);
                            }
                            else if (driverName.startsWith("ora")) {
                                
                                statement.registerOutParameter(i, SqshTypes.ORACLE_CURSOR);
                            }
                            else {
                                
                                System.err.println("Output parameter #" + i + " appears to be"
                                    + " a cursor of a type that jsqsh does not know how to"
                                    + " deal with (unfortunately, cursor handling is not a"
                                    + " JDBC standard, so every driver is different). If you would"
                                    + " like this fixed, please send an e-mail to scottgray1-at-gmail.com"
                                    + " with the details of which JDBC driver you are using");
                                SQLTools.close(statement);
                                return false;
                            }
                        }
                        else {
                            
                            System.err.println("Output parameter #" + i + " appears to be of a"
                                + "data type not supported by the JDBC standard (type \"" 
                                + paramTypeName + "\"). If you would like jsqsh to support this"
                                + " type, please send an e-mail to scottgray1-at-gmail.com with"
                                + " information about your JDBC driver, target database platform"
                                + " and this type name");
                        }
                    }
                    else {
                        
                        statement.registerOutParameter(i, paramType);
                    }
                }
                
                if (paramMode == ParameterMetaData.parameterModeInOut) {
                    
                    if (inoutParamIdx >= inoutParameterValues.size()) {
                        
                        session.err.println("Parameter #" + i + "is an INOUT "
                            + "parameters and must specify a value with the "
                            + "syntax '?=<value>'");
                        SQLTools.close(statement);
                        return false;
                    }
                    
                    statement.setString(i, inoutParameterValues.get(inoutParamIdx++));
                }
            }
            
            sigHandler = new CancelingSignalHandler(statement);
            sigMan.push(sigHandler);
            
            startTime = System.currentTimeMillis();
            
            boolean hasResults = statement.execute();
            
            session.stopVisualTimer();
            
            ok = execute(renderer, session, statement, hasResults);
            
            /*
             * Display any output parameters
             */
            if (outputParams.size() > 0) {
                
                /*
                 * If any of the output parameters were result sets then display
                 * then as a regular result set.
                 */
                Iterator<Integer> iter = outputParams.iterator();
                
                while (iter.hasNext()) {
                    
                    int idx = iter.next();
                    
                    Object o = statement.getObject(idx);
                    if (o instanceof ResultSet) {
                            
                        session.out.println();
                        session.out.println("Parameter #" + idx + " CURSOR:");
                            
                        displayResults(renderer, session, (ResultSet) o, null);
                        
                        iter.remove();
                    }
                }
            }
            
            /*
             * Are there any output parameters left after displaying the output
             * of cursors?
             */
            if (outputParams.size() > 0) {
            
                int nOuts = outputParams.size();
                
                ColumnDescription []cols = new ColumnDescription[nOuts];
                String []row = new String[nOuts];
                for (int i = 0; i < nOuts; i++) {
                    
                    int idx = outputParams.get(i);
                    
                    String name = "Param #" + idx;
                    if (idx == 1 && hasReturnMarker[0] == true) {
                        
                        name = "Return Code";
                    }
                    
                    cols[i] = getDescription(name,
                        paramMeta.getParameterType(idx),
                        paramMeta.getPrecision(idx),
                        paramMeta.getScale(idx));
                    
                    Object val = statement.getObject(idx);
                    if (statement.wasNull() || val == null) {
                        
                        row[i] = formatter.getNull();
                    }
                    else {
                    
                        row[i] = cols[i].getFormatter().format(val);
                    }
                }
                
                session.out.println();
                renderer.header(cols);
                renderer.row(row);
                renderer.flush();
            }
        }
        finally {
            
            session.stopVisualTimer();
            
            if (sigHandler != null) {
                
                sigMan.pop();
            }
            
            SQLTools.close(statement);
        }
        
        return ok;
        
    }
    
    /**
     * Given a <code>{ [?=] call ... }</code> statement, atttempts to parse
     * out "inout" values that are specified as ?=&lt;value&gt;. So the string
     * <pre>
     *    { ?= call myproc(10, 5, ?, ?=21) }
     * </pre>
     * has two input parameters (10 and 5) and one output parameter (?) and
     * one in/out parameter (with an input value of 21). This method would return
     * the value "21".
     * 
     * @param sql The call statement to parse out
     * @return A list of values to provide as initial values for in/out parameters
     */
    private String getInoutParameterValues(String sql, List<String> inoutParams,
        boolean []hasReturnMarker) {
        
        int idx = 0;
        int len = sql.length();
        
        int chunkStart = 0;
        
        // Search for the open brace
        idx = SQLParseUtil.skipWhitespace(sql, len, idx);
        if (idx >= len || sql.charAt(idx) != '{') {
            
            return sql;
        }
        
        // Search for either "call" or "?="
        ++idx;
        idx = SQLParseUtil.skipWhitespace(sql, len, idx);
        
        if (idx >= len) {
            
            return sql;
        }
        
        // We have the initial ?= (or do we??)
        if (sql.charAt(idx) == '?') {
            
            ++idx;
            idx = SQLParseUtil.skipWhitespace(sql, len, idx);
            
            if (idx >= len || sql.charAt(idx) != '=') {
                
                return sql;
            }
            
            hasReturnMarker[0] = true;
            ++idx;
            idx = SQLParseUtil.skipWhitespace(sql, len, idx);
        }
        
        // Better have a "call" word here
        if (idx >= len || ! (sql.regionMatches(true, idx, "call", 0, 4))) {
            
            return sql;
        }
        
        // Skip "call"
        idx += 4;
        
        /*
         * While we find inout parameters, we have to convert "?=<value>" to
         * just "?" while we go, so this buffer holds the new string.
         */
        StringBuilder sb = new StringBuilder(sql.length());
        
        /*
         * From here, the logic gets a little more hokey. I'm not going to
         * actually parse this thing out properly, but going to scan it for
         * occurances of ? that do not appear inside of quoted strings.
         */
        while (idx < len) {
            
            char ch = sql.charAt(idx);
            switch (ch) {
            
            case '\'': 
            case '"': 
                idx = SQLParseUtil.skipQuotedString(sql, len, idx);
                break;
            
            case '?':
                // Skip the question mark
                ++idx; 
                idx = SQLParseUtil.skipWhitespace(sql, len, idx);
                
                // Did we hit a '?='?
                if (idx < len && sql.charAt(idx) == '=') {
                    
                    // Copy everything from the previous chunk into the statement
                    sb.append(sql, chunkStart, idx);
                    
                    // Skip the '='
                    ++idx;
                    idx = SQLParseUtil.skipWhitespace(sql, len, idx);
                    
                    if (idx >= len) {
                        
                        inoutParams.add("");
                    }
                    else if (sql.charAt(idx) == '\'' || sql.charAt(idx) == '"') {
                        
                        char quote = sql.charAt(idx);
                        
                        // We have a quoted value like "?='hello world'"
                        int startIdx = idx;
                        idx = SQLParseUtil.skipQuotedString(sql, len, idx);
                        
                        String value = sql.substring(startIdx+1, idx-1);
                        
                        // Were there nested quotes like 'Scott''s book', then
                        // we need to de-double the quotes.
                        if (value.indexOf(quote) >= 0) {
                            
                            value = value.replace(
                                    String.valueOf(quote) + quote, 
                                    String.valueOf(quote));
                        }
                        
                        inoutParams.add(value);
                    }
                    else {
                        
                        // We have a simple constant value like "?=10"
                        int startIdx = idx++;
                        while (idx < len) {
                            
                            char ch2 = sql.charAt(idx);
                            if (Character.isWhitespace(ch2)
                                || ch2 == ',' || ch2 == ')' || ch2 == '(' ) {
                                
                                break;
                            }
                            
                            ++idx;
                        }
                        
                        inoutParams.add(sql.substring(startIdx, idx));
                    }
                    
                    chunkStart = idx;
                }
                break;
            default:
                ++idx;
            }
        }
        
        sb.append(sql, chunkStart, len);
        return sb.toString();
    }
    
    /**
     * Executes and displays the results from a callable statement.
     * 
     * @param session The session that will be used for output.
     * @param sql A statement to be prepared and call with the parameters
     *   provided
     *
     * @return true if the SQL executed without error (warnings do not
     *   count towards errors), false if there was at least one error
     *   raised during the execution of the SQL.
     *   
     * @throws SQLException Thrown if there is an issue.
     */
    public boolean executeCall (Session session, String sql,
            CallParameter []params)
        throws SQLException {
        
        boolean ok = true;
        Renderer renderer = session.getContext().getRendererManager()
            .getRenderer(session);
        
        CallableStatement statement = null;
        Connection conn = session.getConnection();
        CancelingSignalHandler sigHandler = null;
        SignalManager sigMan = SignalManager.getInstance();
        
        if (conn == null) {
            
            throw new SQLException("No database connection has been established");
        }
        
        try {
            
            /*
             * Start the visual timer. This will do nothing if the facility has
             * not been enabled.  Note that the facility is stopped on error
             * or when the first row has been received.
             */
            session.startVisualTimer();
            
            statement = conn.prepareCall(sql);
            bindParameters(statement, params);
            
            sigHandler = new CancelingSignalHandler(statement);
            sigMan.push(sigHandler);
            
            startTime = System.currentTimeMillis();
            
            boolean hasResults = statement.execute();
            
            session.stopVisualTimer();
            
            ok = execute(renderer, session, statement, hasResults);
            
            /*
             * If there were any output parameters, then try to display 
             * them.
             */
            for (CallParameter param : params) {
                
                if (param.getDirection() == CallParameter.OUTPUT
                        || param.getDirection() == CallParameter.INOUT) {
                    
                    if (param.getType() == SqshTypes.ORACLE_CURSOR) {
                        
                        
                        ResultSet rs = 
                            (ResultSet) statement.getObject(param.getIdx());
                        
                        try {
                            
                            displayResults(renderer, session, rs, null);
                        }
                        finally {
                            
                            SQLTools.close(rs);
                        }
                    }
                }
            }
        }
        finally {
            
            session.stopVisualTimer();
            
            if (sigHandler != null) {
                
                sigMan.pop();
            }
            
            SQLTools.close(statement);
        }
        
        return ok;
    }
    
    /**
     * Executes and displays the results from a parpared statement.
     * 
     * @param session The session that will be used for output.
     * @param sql A sql statement to be prepared and called with the
     *    parameters provided
     *
     * @return true if the SQL executed without error (warnings do not
     *   count towards errors), false if there was at least one error
     *   raised during the execution of the SQL.
     *   
     * @throws SQLException Thrown if there is an issue.
     */
    public boolean executePrepare (Session session, String sql,
            CallParameter []params)
        throws SQLException {
        
        boolean ok = true;
        Renderer renderer = session.getContext().getRendererManager()
            .getRenderer(session);
        
        PreparedStatement statement = null;
        Connection conn = session.getConnection();
        CancelingSignalHandler sigHandler = null;
        SignalManager sigMan = SignalManager.getInstance();
        
        if (conn == null) {
            
            throw new SQLException("No database connection has been established");
        }
        
        try {
            
            session.startVisualTimer();
            
            statement = conn.prepareStatement(sql);
            bindParameters(statement, params);
            
            sigHandler = new CancelingSignalHandler(statement);
            sigMan.push(sigHandler);
            
            startTime = System.currentTimeMillis();
            
            boolean hasResults = statement.execute();
            
            session.stopVisualTimer();
            
            ok = execute(renderer, session, statement, hasResults);
        }
        finally {
            
            session.stopVisualTimer();
            
            if (sigHandler != null) {
                
                sigMan.pop();
            }
            
            SQLTools.close(statement);
        }
        
        return ok;
    }
    
    
    
    /**
     * Executes and displays the results from a block of sql.
     * 
     * @param renderer The renderer that is to be used to display the output
     *   of the statement.
     * @param session The session that will be used for output.
     * @param sql A SQL statement that is to be executed for
     * 
     * @return true if the SQL executed without error (warnings do not
     *   count towards errors), false if there was at least one error
     *   raised during the execution of the SQL.
     *   
     * @throws SQLException Thrown if there is an issue.
     */
    public boolean execute (Renderer renderer, Session session, String sql)
        throws SQLException {
        
        boolean ok = true;

        if (expand) {
            
            sql = session.expand(sql);
        }
        
        Statement statement = null;
        Connection conn = session.getConnection();
        CancelingSignalHandler sigHandler = null;
        SignalManager sigMan = SignalManager.getInstance();
        
        if (conn == null) {
            
            throw new SQLException("No database connection has been established");
        }
        
        /*
         * Since we grabbed the connection, above we know we have a 
         * SQLConnectionContext.
         */
        SQLConnectionContext ctx = 
            (SQLConnectionContext) session.getConnectionContext();
        
        try {
            
            session.startVisualTimer();
            
            if (ctx.getExecutionMode() == SQLConnectionContext.EXEC_PREPARE) {

                statement = conn.prepareStatement(sql);
                
                initStatement(ctx, session, statement);
                
                sigHandler = new CancelingSignalHandler(statement);
                sigMan.push(sigHandler);
                
                startTime = System.currentTimeMillis();
                
                boolean hasResults = ((PreparedStatement) statement).execute();
                
                session.stopVisualTimer();
                
                ok = execute(renderer, session, statement, hasResults);
            }
            else  {

                statement = conn.createStatement();
                
                initStatement(ctx, session, statement);
                
                sigHandler = new CancelingSignalHandler(statement);
                sigMan.push(sigHandler);
                
                startTime = System.currentTimeMillis();
                
                boolean hasResults = statement.execute(sql);
                
                session.stopVisualTimer();
                
                ok = execute(renderer, session, statement, hasResults);
            }
        }
        finally {
            
            session.stopVisualTimer();
            
            /*
             * Take the statement away from the SQLConnectionContext.
             */
            ctx.clearStatement();
            
            if (sigHandler != null) {
                
                sigMan.pop();
            }
            
            SQLTools.close(statement);
        }
        
        return ok;
    }
    
    /**
     * Called for each newly created statement to initialize it based upon session
     * settings.
     * @param ctx The connection
     * @param session The session
     * @param statement The statement
     */
    private void initStatement(SQLConnectionContext ctx, Session session, Statement statement) {
        
        /*
         * Registers the statement handle back with the context, which
         * allows the ctx.cancel() to do its work to cancel the statement
         */
        ctx.setStatement(statement);
        
        int fetchSize = session.getFetchSize();
        if (fetchSize > 0) {
            
            try {
                
                statement.setFetchSize(fetchSize);
            }
            catch (SQLException e) {
                
                session.err.println("WARNING: Requested fetch size "
                   + fetchSize + " is invalid: " + e.getMessage()
                   + ". Resetting to -1");
                session.setFetchSize(-1);
            }
        }
    }
    
    /**
     * Used to bind parameters to a prepared or callable statement.
     * 
     * @param statement The statement
     * @param params The parameters to bind
     * @throws SQLException Thrown if something goes wrong.
     */
    private void bindParameters (PreparedStatement statement,
            CallParameter []params)
        throws SQLException {
        
        for (CallParameter param : params) {
            
            String value = param.getValue();
            
            try {
                
                switch (param.getType()) {
                    
                    case Types.VARCHAR:
                    case Types.CHAR:
                        statement.setString(param.getIdx(), value);
                        break;
                    
                    case Types.BOOLEAN:
                        if (value == null || value.length() == 0) {
                                        
                            statement.setNull(param.getIdx(), Types.BOOLEAN);
                        }
                        else {
                                        
                            statement.setBoolean(param.getIdx(), 
                                Boolean.valueOf(value));
                        }
                        break;
                                    
                    case Types.DOUBLE:
                        if (value == null || value.length() == 0) {
                                        
                            statement.setNull(param.getIdx(), Types.DOUBLE);
                        }
                        else {
                                        
                            statement.setDouble(param.getIdx(),
                                Double.valueOf(value));
                        }
                        break;
                                    
                    case Types.FLOAT:
                        if (value == null || value.length() == 0) {
                                        
                            statement.setNull(param.getIdx(), Types.FLOAT);
                        }
                        else {
                                        
                            statement.setFloat(param.getIdx(),
                                Float.valueOf(value));
                        }
                        break;
                                    
                    case Types.INTEGER:
                        if (value == null || value.length() == 0) {
                                        
                            statement.setNull(param.getIdx(), Types.INTEGER);
                        }
                        else {
                                        
                            statement.setInt(param.getIdx(),
                                Integer.valueOf(value));
                        }
                        break;
                                    
                    case Types.BIGINT:
                        if (value == null || value.length() == 0) {
                                        
                            statement.setNull(param.getIdx(), Types.BIGINT);
                        }
                        else {
                                        
                            statement.setLong(param.getIdx(),
                                Long.valueOf(value));
                        }
                        break;
                                    
                    case SqshTypes.ORACLE_CURSOR:
                        if (statement instanceof CallableStatement) {
                                        
                            ((CallableStatement) statement)
                                .registerOutParameter(param.getIdx(),
                                    SqshTypes.ORACLE_CURSOR);
                        }
                        break;
                                
                                    
                    default:
                        throw new SQLException("Unrecognized parameter type");
                }
            }
            catch (NumberFormatException e) {
                        
                throw new SQLException ("Invalid number format '"
                    + value + "' provided for type '" + param.getType() + "'");
            }
        }
    }
    
    /**
     * Executes SQL or a prepared statement.
     * 
     * @param renderer The renderer that is to be used to display the output
     *   of the statement.
     * @param session The session that will be used for output.
     * @param statement The statement that was just executed.
     * 
     * @return true if the SQL executed without error (warnings do not
     *   count towards errors), false if there was at least one error
     *   raised during the execution of the SQL.
     *   
     * @throws SQLException Thrown if there is an issue.
     */
    private boolean execute (Renderer renderer, Session session,
            Statement statement, boolean hasResults)
        throws SQLException {
        
        Connection conn = session.getConnection();
        ResultSet resultSet = null;
        boolean done = false;
        int updateCountCount = 0; /* # of updateCounts we have seen */
        int updateCount = -1;
        boolean ok = true;
        
        if (conn == null) {
            
            throw new SQLException("No database connection has been established");
        }
        
        firstRowTime = 0L;
        endTime = 0L;
        
        try {
            
            SQLTools.printWarnings(session, conn);
            
            /*
             * If we have a row limit and it is to be driver enforced, then
             * set it on the statement.
             */
            if (rowLimitMethod == LIMIT_DRIVER && maxRows > 0) {
                
                statement.setMaxRows(maxRows);
            }
            
            /*
             * If there are no row results, then fetch the update count.
             */
            if (!hasResults) {
                
                updateCount = statement.getUpdateCount();
                SQLTools.printWarnings(session, statement);
                
                if (updateCount >= 0) {
                    
                    ++updateCountCount;
                }
            }
            
            StringBuilder footer = new StringBuilder();
            do {
                
                /*
                 * Record the number of rows returned. -1 indicates no
                 * rows were displayed.
                 */
                if (hasResults) {
                    
                    int nRows = -1;
                    
                    resultSet = statement.getResultSet();
                    SQLTools.printWarnings(session, statement);
                    
                    if (showMetadata) {
                        
                        displayMetadata(session, resultSet.getMetaData());
                    }
                    
                    if (renderer.isDiscard()) {
                        
                        nRows = discardResults(session, resultSet, renderer);
                    }
                    else {
                        
                        nRows = displayResults(renderer, session, resultSet, null);
                    }
                    
                    /*
                     * A negative value here indicates that the results
                     * were not properly displayed to the caller, so we
                     * are bailing.
                     */
                    if (nRows < 0) {
                        
                        return ok;
                    }
                    
                    footer.append(nRows);
                    footer.append(" row");
                    if (nRows != 1) {
                            
                        footer.append('s');
                    }
                    footer.append(" in results");
                    
                    /*
                     * If the row count is greater than our max then the
                     * query results were limited in some fashion, so we
                     * let the user know.
                     */
                    if (maxRows > 0 && nRows > maxRows) {
                        
                        if (rowLimitMethod == LIMIT_CANCEL) {
                            
                            footer.append(", query cancelled to limit results ");
                            done = true;
                        }
                        else {
                            
                            footer.append(", first ");
                            footer.append(maxRows);
                            footer.append(" rows shown ");
                        }
                    }
                    
                    SQLTools.close(resultSet);
                    resultSet = null;
                }
                else {
                    
                    if (noCount == false) {
                        
                        if (updateCount >= 0) {
                            
                            footer.append(updateCount + " row"
                                + ((updateCount != 1) ? "s" : "")
                                + " affected ");
                        } 
                        else {
                            
                            footer.append("ok. ");
                        }
                    }
                }
                
                /*
                 * Check for more results. 
                 * 
                 * PLEASE READ: The try/catch block is TEMPORARY and only here to
                 * support the Hive driver. Yes, instead of doing something simple
                 * like returning false here, they declare they do not support the
                 * method. The same is true of getUpdateCount() by the way.
                 */
                try {
                    
                    hasResults = statement.getMoreResults();
                    SQLTools.printWarnings(session, statement);
                    
                    if (hasResults) {
                        
                        updateCountCount = 0;
                    }
                }
                catch (SQLException e) {
                    
                    if (e.getMessage().equals("Method not supported")) {
                        
                        session.err.println("WARNING: Statement class " 
                            + statement.getClass().getName()
                            + " does not support getMoreResults()");
                                        
                        hasResults = false;
                        done = true;
                    }
                    else {
                        
                        throw e;
                    }
                }
                
                /*
                 * And check the update count.
                 */
                if (!done && !hasResults) {
                    
                    updateCount = statement.getUpdateCount();
                    SQLTools.printWarnings(session, statement);
                    
                    if (updateCount >= 0) {
                        
                        ++updateCountCount;
                    }
                }
                
                
                /*
                 * We are done if we have either explicitly chosen to be
                 * done (i.e. a cancel) or if there are neither row results
                 * nor update counts left.
                 */
                done = (done || (hasResults == false && updateCount < 0) 
                    || (maxUpdateCount > 0 && updateCountCount >= maxUpdateCount));
                
                /*
                 * If we are finished and we are being asked to show query
                 * statistics, then show them.
                 */
                if (done && showTimings) {
                                
                    endTime = System.currentTimeMillis();
                                
                    if (firstRowTime > 0L) {
                        footer.append("(first row: "
                            +  TimeUtils.millisToDurationString(firstRowTime - startTime) + "; total: "
                            +  TimeUtils.millisToDurationString(endTime - startTime) + ")");
                    }
                    else {
                                    
                        footer.append("(total: "
                            +  TimeUtils.millisToDurationString(endTime - startTime) + ")");
                    }
                }
                
                if (footer.length() > 0) {
                    
                    renderer.footer(footer.toString());
                    footer.setLength(0);
                }
            }
            while (!done);
        }
        finally {
            
            /*
             * Ensure the timer service has been stopped even if we get an 
             * exception.
             */
            session.stopVisualTimer();
            
            SQLTools.close(resultSet);
        }
        
        return ok;
    }
    
    /**
     * Used when collecting timing information to fetch all rows, but never
     * look at them (discard)
     * 
     * @param session The session discarding
     * @param resultSet The result to discard
     * @param renderer THe discarding renderer
     * @return the number of rows discarded
     * @throws SQLException You know...
     */
    private int discardResults(Session session, ResultSet resultSet, Renderer renderer)
        throws SQLException {

        SQLTools.printWarnings(session, resultSet);

        DataFormatter formatter = sqshContext.getDataFormatter();
        ColumnDescription []columns = getDescription(resultSet, null);
        String[] row = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {

            row[i] = formatter.getNull();
        }

        renderer.header(columns);

        int rowCount = 0;
        while (resultSet.next()) {
            
            SQLTools.printWarnings(session, resultSet);
            ++rowCount;
            if (firstRowTime == 0L && rowCount == 1) {

                firstRowTime = System.currentTimeMillis();
            }
            
            /*
             * Check to see if we have hit the limit on the number of
             * rows we are to process.
             */
            if (maxRows > 0 && rowCount > maxRows) {
                
                if (rowLimitMethod == LIMIT_CANCEL) {
                    
                    resultSet.getStatement().cancel();
                    break;
                }
                else if (rowLimitMethod == LIMIT_DISCARD) {
                    
                    continue;
                }
            }

            if (!renderer.row(row)) {

                return -1;
            }
        }

        if (!renderer.flush()) {

            return -1;
        }
        
        return rowCount;
    }
    
    /**
     * Displays a result set.
     * @param session The session used for output.
     * @param resultSet The result set to display
     * @param displayCols If non-null, can contains a set of column
     *     numbers that are to be displayed.
     * @return The number of rows displayed.
     * @throws SQLException Thrown if something bad happens.
     */
    public int displayResults(Renderer renderer, Session session,
            ResultSet resultSet, Set<Integer>displayCols)
        throws SQLException {
        
        SQLTools.printWarnings(session, resultSet);
        
        DataFormatter formatter = sqshContext.getDataFormatter();
        ColumnDescription []columns = getDescription(resultSet, displayCols);
        int nCols = resultSet.getMetaData().getColumnCount();
        int rowCount = 0;
        
        /*
         * Display the header
         */
        renderer.header(columns);
        
        while (resultSet.next()) {
            
            SQLTools.printWarnings(session, resultSet);
            
            ++rowCount;
            if (firstRowTime == 0L && rowCount == 1) {
                
                firstRowTime = System.currentTimeMillis();
            }
            
            /*
             * Check to see if we have hit the limit on the number of
             * rows we are to process.
             */
            if (maxRows > 0 && rowCount > maxRows) {
                
                if (rowLimitMethod == LIMIT_CANCEL) {
                    
                    resultSet.getStatement().cancel();
                    break;
                }
                else if (rowLimitMethod == LIMIT_DISCARD) {
                    
                    continue;
                }
            }
            
            String row[] = new String[columns.length];
            int idx = 0;
            for (int i = 1; i <= nCols; i++) {
                
                if (displayCols == null || displayCols.contains(i)) {
                    
                    Object value = null;
                    boolean wasNull = false;
                    boolean wasError = false;
                    
                    /*
                     * With certain drivers I've had problems with resultSet.getObject()
                     * so for those data types that I run into this issue I am
                     * calling the "correct" getter method.
                     */
                    try {
                        
                        switch (columns[idx].getNativeType()) {
                    
                        case Types.TIMESTAMP:
                            value = resultSet.getTimestamp(i);
                            break;
                        case Types.VARCHAR:
                        case Types.CHAR:
                        case Types.LONGVARCHAR:
                            value = resultSet.getString(i);
                            break;
                        default:
                            value = resultSet.getObject(i);
                        }
                        
                        wasNull = resultSet.wasNull();
                    }
                    catch (SQLException e) {
                        
                        LOG.fine("Row #" + rowCount + ", column " + i 
                                + ", driver error decoding value: " + e.getMessage());
                        
                        session.setException(e);
                        wasError = true;
                        wasNull = false;
                    }
                    
                    if (wasNull) {
                        
                        row[idx] = formatter.getNull();
                    }
                    else {
                        
                        if (wasError) {
                            
                            row[idx] = "*ERROR*";
                        }
                        else if (value == null) {
                            
                            session.err.println("WARNING: Row #" 
                                + rowCount + ", column " + i + ", driver indicated "
                                + "a value present, but returned NULL");
                            row[idx] = formatter.getNull();
                        }
                        else {
                            
                            row[idx] = columns[idx].getFormatter().format(value);
                        }
                    }
                    
                    ++idx;
                }
            }
            
            if (renderer.row(row) == false) {
                
                return -1;
            }
        }
        
        if (renderer.flush() == false) {
            
            return -1;
        }
        
        return rowCount;
    }
    
    /**
     * Called to render the result set metadata as a table. This is
     * primarily for debugging purposes.
     * 
     * @param session The session.
     * @param meta Metadata for the result set
     */
    public void displayMetadata(Session session, ResultSetMetaData meta) {
        
        Renderer renderer = session.getRendererManager().getCommandRenderer(
            session);
        ColumnDescription []cols = new ColumnDescription[10];
        
        cols[0] = new ColumnDescription("NAME", -1);
        cols[1] = new ColumnDescription("LABEL", -1);
        cols[2] = new ColumnDescription("TYPE", -1);
        cols[3] = new ColumnDescription("SIZE", -1, 
            ColumnDescription.Alignment.RIGHT,
            ColumnDescription.OverflowBehavior.TRUNCATE);
        cols[4] = new ColumnDescription("PREC", -1,
            ColumnDescription.Alignment.RIGHT,
            ColumnDescription.OverflowBehavior.TRUNCATE);
        cols[5] = new ColumnDescription("SCALE", -1,
            ColumnDescription.Alignment.RIGHT,
            ColumnDescription.OverflowBehavior.TRUNCATE);
        cols[6] = new ColumnDescription("NULLABLE", -1,
            ColumnDescription.Alignment.RIGHT,
            ColumnDescription.OverflowBehavior.TRUNCATE);
        cols[7] = new ColumnDescription("CATALOG", -1);
        cols[8] = new ColumnDescription("SCHEMA", -1);
        cols[9] = new ColumnDescription("TABLE", -1);
        
        renderer.header(cols);
        
        try {
            
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                
                String schema = null;
                String catalog = null;
                String table = null;
                try { schema = meta.getSchemaName(i); }
                    catch (SQLException e) { /* IGNORED */ }
                try { catalog = meta.getCatalogName(i); }
                    catch (SQLException e) { /* IGNORED */ }
                try { table = meta.getTableName(i); }
                    catch (SQLException e) { /* IGNORED */ }
                
                String row[] = new String[10];
                row[0] = meta.getColumnName(i);
                row[1] = meta.getColumnLabel(i);
                row[2] = meta.getColumnTypeName(i);
                row[3] = Integer.toString(meta.getColumnDisplaySize(i));
                row[4] = Integer.toString(meta.getPrecision(i));
                row[5] = Integer.toString(meta.getScale(i));
                row[6] = Integer.toString(meta.isNullable(i));
                row[7] = catalog;
                row[8] = schema;
                row[9] = table;
                
                renderer.row(row);
            }
            
            renderer.flush();
        }
        catch (SQLException e) {
            
            SQLTools.printException(session, e);
        }
    }
    
    /**
     * Called to render the result set metadata as a table. This is
     * primarily for debugging purposes.
     * 
     * @param session The session.
     * @param meta Metadata for the result set
     */
    public void displayParameterMetadata(Session session, ParameterMetaData meta) {
        
        Renderer renderer = session.getRendererManager().getCommandRenderer(
            session);
        ColumnDescription []cols = new ColumnDescription[8];
        
        cols[0] = new ColumnDescription("PARAM", -1,
            ColumnDescription.Alignment.RIGHT,
            ColumnDescription.OverflowBehavior.TRUNCATE);
        cols[1] = new ColumnDescription("TYPE", -1);
        cols[2] = new ColumnDescription("TYPE NAME", -1);
        cols[3] = new ColumnDescription("PREC", -1, 
            ColumnDescription.Alignment.RIGHT,
            ColumnDescription.OverflowBehavior.TRUNCATE);
        cols[4] = new ColumnDescription("SCALE", -1, 
            ColumnDescription.Alignment.RIGHT,
            ColumnDescription.OverflowBehavior.TRUNCATE);
        cols[5] = new ColumnDescription("MODE", -1);
        cols[6] = new ColumnDescription("SIGNED", -1);
        cols[7] = new ColumnDescription("NULLABLE", -1);
        
        renderer.header(cols);
        
        try {
            
            for (int i = 1; i <= meta.getParameterCount(); i++) {
                
                String row[] = new String[8];
                row[0] = Integer.toString(i);
                row[1] = SQLTools.getTypeName(meta.getParameterType(i));
                row[2] = meta.getParameterTypeName(i);
                row[3] = Integer.toString(meta.getPrecision(i));
                row[4] = Integer.toString(meta.getScale(i));
                row[5] = SQLTools.getParameterMode(meta.getParameterMode(i));
                row[6] = meta.isSigned(i) ? "Y" : "N";
                row[7] = SQLTools.getParameterNullability(meta.isNullable(i));
                
                renderer.row(row);
            }
            
            renderer.flush();
        }
        catch (SQLException e) {
            
            SQLTools.printException(session, e);
        }
    }
    
    /**
     * Returns a description of the provided result set.
     * 
     * @param resultSet The result set
     * @param displayCols If non-null, can contains a set of column
     *     numbers that are to be displayed.
     * @return A description of the result set.
     * @throws SQLException Thrown if there is a problem.
     */
    private ColumnDescription[] getDescription(ResultSet resultSet,
            Set<Integer>displayCols)
        throws SQLException {
        
        ResultSetMetaData meta = resultSet.getMetaData();
        int nCols = meta.getColumnCount();
        int nDisplay = 
            (displayCols != null ? displayCols.size() : nCols);
        ColumnDescription []colDesc = new ColumnDescription[nDisplay];
        
        int idx = 0;
        for (int i = 1; i <= nCols; i++) {
            
            if (displayCols == null || displayCols.contains(i)) {
                
                colDesc[idx] = getDescription(meta, i);
                ++idx;
            }
        }
        
        return colDesc;
    }
    
    /**
     * Return a description of how to format a column.
     * 
     * @param meta The metadata for a result set
     * @param idx The index of the of column to fetch in the result set
     * @return A description of how to format that column
     * @throws SQLException
     */
    public ColumnDescription getDescription(ResultSetMetaData meta, int idx)
        throws SQLException {
        
        int type = meta.getColumnType(idx);
        int precision = 0;
        int scale = 0;
        
        switch (type) {
        
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
                precision = meta.getColumnDisplaySize(idx);
                break;
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                precision = meta.getPrecision(idx);
                scale = meta.getScale(idx);
                break;
                
            default:
                break;
        }
        
        return getDescription(
            meta.getColumnLabel(idx),
            type,
            precision,
            scale);
    }
    
    /**
     * Returns a description of how to format a column
     * 
     * @param name The name of the column
     * @param type The data type (java.sql.Type) of the column
     * @param precision Precision of the column (if applicable)
     * @param scale The scale of the column (if applicable)
     * @return The description of how to format the column
     */
    public ColumnDescription getDescription(String name, int type, int precision, int scale) {
        
        DataFormatter formatter = sqshContext.getDataFormatter();
        ColumnDescription.Type colType =
            ColumnDescription.Type.STRING;
        ColumnDescription.Alignment alignment =
            ColumnDescription.Alignment.LEFT;
        Formatter format = null;
        
        switch (type) {
            
            case Types.BIGINT:
                format = formatter.getLongFormatter();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
            
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                format = formatter.getByteFormatter(precision);
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.BIT:
                format = formatter.getBitFormatter();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.BLOB:
                format = formatter.getBlobFormatter();
                break;
                
            case Types.BOOLEAN:
                format = formatter.getBooleanFormatter();
                break;
                
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
                format = formatter.getStringFormatter(precision);
                break;
                
            case Types.CLOB:
            case Types.NCLOB:
                format = formatter.getClobFormatter();
                break;
                
            case Types.DATE:
                format = formatter.getDateFormatter();
                break;
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                format = formatter.getBigDecimalFormatter(precision, scale);
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.DOUBLE:
            case Types.FLOAT:
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                format = formatter.getDoubleFormatter();
                break;
                
            case Types.REAL:
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                format = formatter.getFloatFormatter();
                break;
                
            case Types.INTEGER:
                format = formatter.getIntFormatter();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
            
            case Types.SMALLINT:
                format = formatter.getShortFormatter();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
            
            case Types.TINYINT:
                format = formatter.getTinyIntFormatter();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.TIME:
                format = formatter.getTimeFormatter();
                break;
                
            case Types.TIMESTAMP:
                format = formatter.getDatetimeFormatter();
                break;
                
            case Types.SQLXML:
                format = formatter.getXMLFormatter();
                break;
            
            case Types.ARRAY:
                format = formatter.getArrayFormatter(this);
                break;
                
            default:
                format = formatter.getUnsupportedTypeFormatter();
                break;
        }
        
        ColumnDescription c = new ColumnDescription(
            name,  format.getMaxWidth(), alignment,
            ColumnDescription.OverflowBehavior.WRAP);
        c.setType(colType);
        c.setNativeType(type);
        c.setFormatter(format);
        
        return c;
    }
}
