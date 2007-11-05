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
     * Executes the provided SQL.
     * @param sql The SQL to be executed.
     */
    public void execute (Session session, String sql)
        throws SQLException {
        
        Connection conn = session.getSQLContext().getConnection();
        ResultSet resultSet = null;
        Statement statement = null;
        int nRows = 0;
        boolean done = false;
        CancelingSignalHandler sigHandler = null;
        
        if (expand) {
            
            sql = session.expand(sql);
        }
        
        startTime = System.currentTimeMillis();
        firstRowTime = 0L;
        endTime = 0L;
        
        try {
            
            statement = conn.createStatement();
            SQLTools.printWarnings(session.err, conn);
            
            /*
             * If we have a row limit and it is to be driver enforced, then
             * set it on the statement.
             */
            if (rowLimitMethod == LIMIT_DRIVER 
                    && maxRows > 0) {
                
                statement.setMaxRows(maxRows);
            }
            
            /*
             * Install a signal handler that will cancel our query
             * if the user hits CTRL-C.
             */
            sigHandler = new CancelingSignalHandler(statement);
            session.getSignalManager().push((SigHandler) sigHandler);
            
            statement.execute(sql);
            SQLTools.printWarnings(session.err, statement);
            
            /*
             * Get ahold of the renderer that will be used for this
             * session.
             */
            Renderer  renderer = 
                sqshContext.getRendererManager().getRenderer(session);
            StringBuilder footer = new StringBuilder();
            
            while (!done) {
                
                resultSet = statement.getResultSet();
                SQLTools.printWarnings(session.err, statement);
                
                if (resultSet != null) {
                    
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
                            
                            footer.append(", query cancelled to limit results");
                            done = true;
                        }
                        else {
                            
                            footer.append(", first ");
                        	footer.append(maxRows);
                        	footer.append(" rows shown");
                        }
                    }
                }
                else {
                    
                    nRows = statement.getUpdateCount();
                    SQLTools.printWarnings(session.err, statement);
                    
                    if (nRows >= 0) {
                        
                        footer.append(nRows + " row"
                            + ((nRows != 0) ? "s" : "")
                        	+ " affected");
                    } 
                    else {
                        
                        footer.append("ok.");
                    }
                }
                
                if (done == false
                        && statement.getMoreResults() == false
                        && statement.getUpdateCount() < 0) {
                    
                    SQLTools.printWarnings(session.err, statement);
                    endTime = System.currentTimeMillis();
                    
                    if (firstRowTime > 0L) {
                        footer.append(" (first row: "
                            + (firstRowTime - startTime) + "ms; total: "
                        	+ (endTime - startTime) + "ms)");
                    }
                    else {
                        
                        footer.append(" (total: "
                            + (endTime - startTime) + "ms)");
                    }
                    
                    done = true;
                }
                
                if (footer.length() > 0) {
                    
                    renderer.footer(footer.toString());
                    footer.setLength(0);
                }
            }
        }
        finally {
            
            if (sigHandler != null) {
                
                session.getSignalManager().pop();
            }
            
            SQLTools.close(resultSet);
            SQLTools.close(statement);
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
        
        SQLTools.printWarnings(session.err, resultSet);
        
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
            
            SQLTools.printWarnings(session.err, resultSet);
            
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
                    
                    Object value = resultSet.getObject(i);
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
                    + "Datatype is not currently supported by sqsh "
                    + "(type #" + type + ")");
        }
        
        ColumnDescription c = new ColumnDescription(
            meta.getColumnLabel(idx),  format.getMaxWidth(), alignment,
            ColumnDescription.OverflowBehavior.WRAP);
        c.setType(colType);
        c.setFormatter(format);
        
        return c;
    }
}
