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

public class SQLRenderer {
    
    SqshContext sqshContext;
    
    /**
     * Whether or not variable expansion will take place.
     */
    private boolean expand = true;
    
    /**
     * The maximum number of rows that will be rendered. A value <= 0
     * will provide unlimited output.
     */
    private int maxRows = 500;
    
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
            if (maxRows > 0) {
                
                statement.setMaxRows(maxRows);
            }
            
            /*
             * Install a signal handler that will cancel our query
             * if the user hits CTRL-C.
             */
            sigHandler = new CancelingSignalHandler(statement);
            session.getSignalManager().push(sigHandler);
            
            statement.execute(sql);
            while (!done) {
                
                resultSet = statement.getResultSet();
                if (resultSet != null) {
                    
                    nRows = displayResults(session, resultSet, null);
                    
                    /*
                     * A negative value here indicates that the results
                     * were not properly displayed to the caller, so we
                     * are bailing.
                     */
                    if (nRows < 0) {
                        
                        return;
                    }
                    
                    session.out.print(nRows + " row"
                        + ((nRows != 0) ? "s" : "")
                        + " in results");
                }
                else {
                    
                    nRows = statement.getUpdateCount();
                    if (nRows >= 0) {
                        
                        session.out.print(nRows + " row"
                            + ((nRows != 0) ? "s" : "")
                        	+ " affected");
                    } 
                    else {
                        
                        session.out.print("ok.");
                    }
                }
                
                if (statement.getMoreResults() == false
                        && statement.getUpdateCount() < 0) {
                        
                    endTime = System.currentTimeMillis();
                    
                    if (firstRowTime > 0L) {
                        session.out.println(" (first row: "
                            + (firstRowTime - startTime) + "ms; total: "
                        	+ (endTime - startTime) + "ms)");
                    }
                    else {
                        
                        session.out.println(" (total: "
                            + (endTime - startTime) + "ms)");
                    }
                    
                    done = true;
                }
                else {
                    
                    session.out.println();
                }
            }
        }
        finally {
            
            if (sigHandler != null) {
                
                session.getSignalManager().pop();
            }
            
            /*
             * Clean up our statement and result set.
             */
            if (statement != null) {
                
                try {
                    
                    statement.close();
                }
                catch (SQLException e) {
                    
                    /* IGNORED */
                }
            }
            
            if (resultSet != null) {
                
                try {
                    
                    resultSet.close();
                }
                catch (SQLException e) {
                    
                    /* IGNORED */
                }
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
    public int displayResults(Session session, ResultSet resultSet,
            Set<Integer>displayCols)
        throws SQLException {
        
        ColumnDescription []columns = getDescription(resultSet, displayCols);
        int nCols = resultSet.getMetaData().getColumnCount();
        Renderer  renderer = 
            sqshContext.getRendererManager().getRenderer(session, columns);
        ResultSetMetaData meta = resultSet.getMetaData();
        int rowCount = 0;
        
        while (resultSet.next()) {
            
            ++rowCount;
            if (firstRowTime == 0L && rowCount == 1) {
                
                firstRowTime = System.currentTimeMillis();
            }
            
            String row[] = new String[columns.length];
            int idx = 0;
            for (int i = 1; i <= nCols; i++) {
                
                if (displayCols == null || displayCols.contains(i)) {
                    
                    row[idx] = getFormattedValue(resultSet, meta, i);
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
                
                colDesc[idx] = new ColumnDescription(meta.getColumnLabel(i),
                	getColumnWidth(meta, i), getColumnAlignment(meta, i),
                	ColumnDescription.OverflowBehavior.WRAP);
                
                ++idx;
            }
        }
        
        return colDesc;
    }
    
    /**
     * Returns the number of characters that will be required to display
     * a column.
     * @param meta Metadata about the column.
     * @param idx Index of the column.
     * @return The size
     * @throws SQLException Thrown if the type cannot be displayed.
     */
    private int getColumnWidth(ResultSetMetaData meta, int idx)
        throws SQLException {
        
        DataFormatter formatter = sqshContext.getDataFormatter();
        
        int type = meta.getColumnType(idx);
        switch (type) {
            
            case Types.BIGINT:
                return formatter.getLongWidth();
            
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return formatter.getBytesWidth(meta.getColumnDisplaySize(idx));
                
            case Types.BIT:
                return formatter.getShortWidth();
                
            case Types.BLOB:
                return -1;
                
            case Types.BOOLEAN:
                return formatter.getBooleanWidth();
                
            case Types.CHAR:
            case Types.VARCHAR:
            /* case Types.NVARCHAR:*/
            /* case Types.LONGNVARCHAR: */
            case Types.LONGVARCHAR:
            /* case Types.NCHAR: */
                return meta.getColumnDisplaySize(idx);
                
            case Types.CLOB:
            /* case Types.NCLOB: */
                return -1;
                
            case Types.DATE:
                return formatter.getDateWidth();
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                int precision = meta.getPrecision(idx);
                int scale = meta.getScale(idx);
                
                return formatter.getBigDecimalWidth(precision, scale);
                
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                return formatter.getDoubleWidth();
                
            case Types.INTEGER:
                return formatter.getIntWidth();
            
            case Types.SMALLINT:
            case Types.TINYINT:
                return formatter.getShortWidth();
                
            case Types.TIME:
                return formatter.getTimeWidth();
                
            case Types.TIMESTAMP:
                return formatter.getDatetimeWidth();
                
            default:
                break;
        }
        
        throw new SQLException(
            "Column #" + idx 
                + (meta.getColumnLabel(idx) == null 
                    ? ": "
                    : " [" + meta.getColumnLabel(idx) + "]: ")
                + "Datatype is not currently supported by sqsh "
                + "(type #" + type + ")");
    }
    
    private String getFormattedValue(ResultSet res, 
            ResultSetMetaData meta, int idx)
        throws SQLException {
        
        DataFormatter formatter = sqshContext.getDataFormatter();
        
        int type = meta.getColumnType(idx);
        String value = null;
        
        /*
         * Types that are commented out below are because it appears that
         * only java 6 supports them and I don't want to force it on
         * the world yet.
         */
        switch (type) {
            
            case Types.BIGINT:
                value = formatter.formatLong(res.getLong(idx));
                break;
            
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                value = formatter.formatBytes(res.getBytes(idx));
                break;
                
            case Types.BIT:
                value = formatter.formatShort(res.getByte(idx));
                break;
                
            case Types.BOOLEAN:
                value = formatter.formatBoolean(res.getBoolean(idx));
                break;
                
            case Types.CHAR:
            case Types.VARCHAR:
                value = res.getString(idx);
                break;
                
            /* case Types.NVARCHAR: */
            /* case Types.LONGNVARCHAR: */
            case Types.LONGVARCHAR:
            /* case Types.NCHAR: */
                value = res.getString(idx);
                break;
                
                
            case Types.CLOB:
            /* case Types.NCLOB: */
                value = "*CLOB*";
                break;
                
            case Types.DATE:
                value = formatter.formatDate(res.getDate(idx));
                break;
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                value = formatter.formatBigDecimal(res.getBigDecimal(idx));
                break;
                
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                value = formatter.formatDouble(res.getDouble(idx));
                break;
                
            case Types.INTEGER:
                value = formatter.formatInt(res.getInt(idx));
                break;
                
            case Types.SMALLINT:
            case Types.TINYINT:
                value = formatter.formatShort(res.getShort(idx));
                break;
                
            case Types.TIME:
                value = formatter.formatTime(res.getTime(idx));
                break;
                
            case Types.TIMESTAMP:
                value = formatter.formatDatetime(res.getTimestamp(idx));
                break;
                
            default:
                break;
        }
        
        if (res.wasNull()) {
            
            value = formatter.getNull();
        }
        
        if (value == null) {
            
            throw new SQLException(
                "Column #" + idx 
                    + (meta.getColumnLabel(idx) == null 
                        ? ": "
                        : " [" + meta.getColumnLabel(idx) + "]: ")
                    + "Datatype is not currently supported by sqsh "
                    + "(type #" + type + ")");
        }
        
        return value;
    }
    
    private ColumnDescription.Alignment getColumnAlignment(
            ResultSetMetaData meta, int idx)
        throws SQLException {
        
        int type = meta.getColumnType(idx);
        switch (type) {
            
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return ColumnDescription.Alignment.RIGHT;
                
            default:
                return ColumnDescription.Alignment.LEFT;
        }
    }
}
