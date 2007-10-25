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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Set;

import org.sqsh.signals.CancelingSignalHandler;
import org.sqsh.signals.SigHandler;

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
            SQLTools.printWarnings(session.err, conn);
            
            if (maxRows > 0) {
                
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
                    
                    nRows = displayResults(renderer, session, resultSet, null);
                    
                    /*
                     * A negative value here indicates that the results
                     * were not properly displayed to the caller, so we
                     * are bailing.
                     */
                    if (nRows < 0) {
                        
                        return;
                    }
                    
                    footer.append(nRows + " row"
                        + ((nRows != 0) ? "s" : "")
                        + " in results");
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
                
                if (statement.getMoreResults() == false
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
        int width = -1;
        boolean supported = true;
        ColumnDescription.Type colType =
            ColumnDescription.Type.STRING;
        ColumnDescription.Alignment alignment =
            ColumnDescription.Alignment.LEFT;
        
        int type = meta.getColumnType(idx);
        switch (type) {
            
            case Types.BIGINT:
                width = formatter.getLongWidth();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
            
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                width = formatter.getBytesWidth(meta.getColumnDisplaySize(idx));
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.BIT:
                width = formatter.getShortWidth();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.BLOB:
                width = Integer.MAX_VALUE;
                break;
                
            case Types.BOOLEAN:
                width = formatter.getBooleanWidth();
                break;
                
            case Types.CHAR:
            case Types.VARCHAR:
            /* case Types.NVARCHAR:*/
            /* case Types.LONGNVARCHAR: */
            case Types.LONGVARCHAR:
            /* case Types.NCHAR: */
                width = meta.getColumnDisplaySize(idx);
                break;
                
            case Types.CLOB:
            /* case Types.NCLOB: */
                width = Integer.MAX_VALUE;
                break;
                
            case Types.DATE:
                width = formatter.getDateWidth();
                break;
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                int precision = meta.getPrecision(idx);
                int scale = meta.getScale(idx);
                
                width = formatter.getBigDecimalWidth(precision, scale);
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                width = formatter.getDoubleWidth();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.INTEGER:
                width = formatter.getIntWidth();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
            
            case Types.SMALLINT:
            case Types.TINYINT:
                width = formatter.getShortWidth();
                colType = ColumnDescription.Type.NUMBER;
                alignment = ColumnDescription.Alignment.RIGHT;
                break;
                
            case Types.TIME:
                width = formatter.getTimeWidth();
                break;
                
            case Types.TIMESTAMP:
                width = formatter.getDatetimeWidth();
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
            meta.getColumnLabel(idx),  width, alignment,
            ColumnDescription.OverflowBehavior.WRAP);
        c.setType(colType);
        
        return c;
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
                long longValue = res.getLong(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatLong(longValue);
                break;
            
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                byte []binaryValue = res.getBytes(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatBytes(binaryValue);
                break;
                
            case Types.BIT:
                byte bitValue = res.getByte(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatShort(bitValue);
                break;
                
            case Types.BOOLEAN:
                boolean boolValue = res.getBoolean(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatBoolean(boolValue);
                break;
                
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
            /* case Types.NVARCHAR: */
            /* case Types.LONGNVARCHAR: */
            /* case Types.NCHAR: */
                value = res.getString(idx);
                if (res.wasNull()) {
                    
                    value = formatter.getNull();
                }
                break;
                
            case Types.DATE:
                Date dateValue = res.getDate(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatDate(dateValue);
                break;
                
            case Types.DECIMAL:
            case Types.NUMERIC:
                BigDecimal bigDecValue = res.getBigDecimal(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatBigDecimal(bigDecValue);
                break;
                
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                double doubleValue = res.getDouble(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatDouble(doubleValue);
                break;
                
            case Types.INTEGER:
                int intValue = res.getInt(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatInt(intValue);
                break;
                
            case Types.SMALLINT:
            case Types.TINYINT:
                short shortValue = res.getShort(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatShort(shortValue);
                break;
                
            case Types.TIME:
                Time timeValue = res.getTime(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatTime(timeValue);
                break;
                
            case Types.TIMESTAMP:
                Timestamp tsValue = res.getTimestamp(idx);
                value = res.wasNull() ?  formatter.getNull()
                        :  formatter.formatDatetime(tsValue);
                break;
                
            default:
                break;
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
}
