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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Set;

import org.sqsh.signals.CancelingSignalHandler;
import org.sqsh.signals.SigHandler;

public class SQLRenderer {
    
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
    private boolean expand = true;
    
    /**
     * The maximum number of rows that will be rendered. A value <= 0
     * will provide unlimited output.
     */
    private int maxRows = 500;
    
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
     * <= indicates unlimited results.
     * 
     * @return the maximum number of rows that will be displayed.
     */
    public int getMaxRows () {
    
        return maxRows;
    }

    /**
     * Returns the maximum number of rows that will be displayed. A value
     * <= indicates unlimited results.
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
     * @throws SQLException Thrown when...well, you know.
     */
    public void execute (Session session, String sql)
        throws SQLException {
        
        Renderer renderer = session.getContext().getRendererManager()
            .getRenderer(session);
        execute(renderer, session, sql);
    }
    
    /**
     * Executes and displays the results from a prepared statement.
     * 
     * @param session The session that will be used for output.
     * @param statement A prepared statement that has had all of its
     *    SQL and parameters provided.
     * @throws SQLException Thrown if there is an issue.
     */
    public void execute (Session session, PreparedStatement statement) 
        throws SQLException {
        
        Renderer renderer = session.getContext().getRendererManager()
            .getRenderer(session);
        
        execute(renderer, session, null, statement);
    }
    
    /**
     * Executes and displays the results from a block of sql.
     * 
     * @param renderer The renderer that is to be used to display the output
     *   of the statement.
     * @param session The session that will be used for output.
     * @param sql A SQL statement that is to be executed for
     * @throws SQLException Thrown if there is an issue.
     */
    public void execute (Renderer renderer, Session session, String sql)
        throws SQLException {
        
        if (expand) {
            
            sql = session.expand(sql);
        }
        
        execute(renderer, session, sql, null);
    }
    
    /**
     * Executes SQL or a prepared statement.
     * 
     * @param renderer The renderer that is to be used to display the output
     *   of the statement.
     * @param session The session that will be used for output.
     * @param sql If not null, then the SQL that is to be executed for
     *   display.
     * @param preparedStatement If sql is null, then this must be non-null
     *   and must contain a prepared statement for execution.
     * @throws SQLException Thrown if there is an issue.
     */
    private void execute (Renderer renderer, Session session, String sql,
            PreparedStatement preparedStatement)
        throws SQLException {
        
        Connection conn = session.getSQLContext().getConnection();
        ResultSet resultSet = null;
        Statement statement = null;
        boolean done = false;
        CancelingSignalHandler sigHandler = null;
        boolean hasResults;
        int updateCount = -1;
        
        
        startTime = System.currentTimeMillis();
        firstRowTime = 0L;
        endTime = 0L;
        
        try {
            
            SQLTools.printWarnings(session, conn);
            
            if (sql != null) {
                
                statement = conn.createStatement();
                
                /*
             	 * Install a signal handler that will cancel our query
             	 * if the user hits CTRL-C.
             	 */
                sigHandler = new CancelingSignalHandler(statement);
                session.getSignalManager().push((SigHandler) sigHandler);
                
                hasResults = statement.execute(sql);
                SQLTools.printWarnings(session, statement);
            }
            else {
                
                statement = preparedStatement;
                
                /*
             	 * Install a signal handler that will cancel our query
             	 * if the user hits CTRL-C.
             	 */
                sigHandler = new CancelingSignalHandler(statement);
                session.getSignalManager().push((SigHandler) sigHandler);
                
                hasResults = preparedStatement.execute();
                SQLTools.printWarnings(session, statement);
            }
            
            /*
             * If we have a row limit and it is to be driver enforced, then
             * set it on the statement.
             */
            if (rowLimitMethod == LIMIT_DRIVER 
                    && maxRows > 0) {
                
                statement.setMaxRows(maxRows);
            }
            
            /*
             * If there are no row results, then fetch the update count.
             */
            if (!hasResults) {
                
                updateCount = statement.getUpdateCount();
                SQLTools.printWarnings(session, statement);
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
                        
                        displayMetadata(session, resultSet);
                    }
                    
                    nRows = displayResults(renderer, session, resultSet, null);
                    
                    /*
                     * A negative value here indicates that the results
                     * were not properly displayed to the caller, so we
                     * are bailing.
                     */
                    if (nRows < 0) {
                        
                        return;
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
                 */
                hasResults = statement.getMoreResults();
                SQLTools.printWarnings(session, statement);
                
                /*
                 * And check the update count.
                 */
                if (!hasResults) {
                    
                    updateCount = statement.getUpdateCount();
                	SQLTools.printWarnings(session, statement);
                }
                
                /*
                 * We are done if we have either explicitly chosen to be
                 * done (i.e. a cancel) or if there are neither row results
                 * nor update counts left.
                 */
                done = (done || (hasResults == false && updateCount < 0));
                
                /*
                 * If we are finished and we are being asked to show query
                 * statistics, then show them.
                 */
                if (done && showTimings) {
                                
                    endTime = System.currentTimeMillis();
                                
                    if (firstRowTime > 0L) {
                        footer.append("(first row: "
                            + (firstRowTime - startTime) + "ms; total: "
                        	+ (endTime - startTime) + "ms)");
                    }
                    else {
                                    
                        footer.append("(total: "
                            + (endTime - startTime) + "ms)");
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
            
            if (sigHandler != null) {
                
                session.getSignalManager().pop();
            }
            
            SQLTools.close(resultSet);
            
            /*
             * If we created the statement, then close it. If we are
             * using the statement passed in, then its up to the caller
             * to close it.
             */
            if (preparedStatement == null) {
                
                SQLTools.close(statement);
            }
        }
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
        ResultSetMetaData meta = resultSet.getMetaData();
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
                    
                    Object value;
                    
                    /*
                     * This is silly (I'd use stronger language, but I
                     * think I'm mellowing in my old age). For an oracle
                     * TIMESTAMP column, when I ask the driver for the
                     * datatype it reports java.sql.Timestamp. However, 
                     * when I call the driver's ResultSet.getObject() it
                     * returns some sort of native internal representation
                     * of a TIMESTAMP column that is  *not* a 
                     * java.sql.Timestamp and thus jsqsh blows up when it
                     * trys to format it as one.  So, instead I do a special
                     * case to force the driver to return it to me as a 
                     * timestamp.
                     */
                    if (columns[idx].getNativeType() == Types.TIMESTAMP) {
                        
                        value = resultSet.getTimestamp(i);
                    }
                    else {
                        
                        value = resultSet.getObject(i);
                    }
                    
                    if (resultSet.wasNull()) {
                        
                        row[idx] = formatter.getNull();
                    }
                    else {
                        
                        row[idx] = columns[idx].getFormatter().format(value);
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
     * @param resultSet The result set.
     */
    private void displayMetadata(Session session, ResultSet resultSet) {
        
        Renderer renderer = session.getRendererManager().getCommandRenderer(
            session);
        ColumnDescription []cols = new ColumnDescription[8];
        
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
        cols[6] = new ColumnDescription("SCHEMA", -1);
        cols[7] = new ColumnDescription("TABLE", -1);
        
        renderer.header(cols);
        
        try {
            
            ResultSetMetaData meta = resultSet.getMetaData();
            
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                
                String row[] = new String[8];
                row[0] = meta.getColumnName(i);
                row[1] = meta.getColumnLabel(i);
                row[2] = meta.getColumnTypeName(i);
                row[3] = Integer.toString(meta.getColumnDisplaySize(i));
                row[4] = Integer.toString(meta.getPrecision(i));
                row[5] = Integer.toString(meta.getScale(i));
                row[6] = meta.getSchemaName(i);
                row[7] = meta.getTableName(i);
                
                renderer.row(row);
            }
            
            renderer.flush();
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.out, e);
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
     * Returns a description of a column.
     * @param meta Metadata about the column.
     * @param idx Index of the column.
     * @return The description
     * @throws SQLException Thrown if the type cannot be displayed.
     */
    private ColumnDescription getDescription(ResultSetMetaData meta, int idx)
        throws SQLException {
        
        DataFormatter formatter = sqshContext.getDataFormatter();
        boolean supported = true;
        ColumnDescription.Type colType =
            ColumnDescription.Type.STRING;
        ColumnDescription.Alignment alignment =
            ColumnDescription.Alignment.LEFT;
        Formatter format = null;
        
        int type = meta.getColumnType(idx);
        switch (type) {
            
            case Types.BIGINT:
                format = formatter.getLongFormatter();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
            
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                format = formatter.getByteFormatter(
                    meta.getColumnDisplaySize(idx));
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
            /* case Types.NVARCHAR:*/
            /* case Types.LONGNVARCHAR: */
            case Types.LONGVARCHAR:
            /* case Types.NCHAR: */
                format = formatter.getStringFormatter(
                    meta.getColumnDisplaySize(idx));
                break;
                
            case Types.CLOB:
            /* case Types.NCLOB: */
                format = formatter.getClobFormatter();
                break;
                
            case Types.DATE:
                format = formatter.getDateFormatter();
                break;
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                int precision = meta.getPrecision(idx);
                int scale = meta.getScale(idx);
                
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
                
            default:
                supported = false;
                break;
        }
        
        if (supported == false) {
            
            throw new SQLException(
                "Column #" + idx 
                    + (meta.getColumnLabel(idx) == null 
                        ? ": "
                        : " [" + meta.getColumnLabel(idx) + "]: ")
                    + "Datatype is not currently supported by jsqsh "
                    + "(type "
                    + SQLTools.getTypeName(meta.getColumnType(idx))
                    + ", #" + type + ")");
        }
        
        ColumnDescription c = new ColumnDescription(
            meta.getColumnLabel(idx),  format.getMaxWidth(), alignment,
            ColumnDescription.OverflowBehavior.WRAP);
        c.setType(colType);
        c.setNativeType(type);
        c.setFormatter(format);
        
        return c;
    }
}
