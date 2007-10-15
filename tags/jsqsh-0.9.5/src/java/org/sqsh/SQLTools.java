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

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.sqsh.parser.SimpleSQLTokenizer;

/**
 * This class provides a bunch of static methods that help process
 * JDBC results.
 */
public class SQLTools {
    
    /**
     * Silently close a result set, ignoring any SQLExceptions.
     * 
     * @param results The results to close, null is silently ignored.
     */
    public static void close(ResultSet results) {
        
        if (results != null) {
            
            try {
                
                results.close();
            }
            catch (SQLException e) {
                
                /* IGNORED */
            }
        }
    }
    
    /**
     * Silently close a statement, ignoring any SQLExceptions.
     * 
     * @param statement The statement to close, null is silently
     *   ignored.
     */
    public static void close(Statement statement) {
        
        if (statement != null) {
            
            try {
                
                statement.close();
            }
            catch (SQLException e) {
                
                /* IGNORED */
            }
        }
    }
    
    /**
     * Silently close a connection, ignoring any SQLExceptions.
     * 
     * @param connection The connection to close, null is silently
     *   ignored.
     */
    public static void close(Connection connection) {
        
        if (connection != null) {
            
            try {
                
                connection.close();
            }
            catch (SQLException e) {
                
                /* IGNORED */
            }
        }
    }
    
    /**
     * Display SQLExceptions (and any nested exceptions) to an output stream
     * in a nicely formatted style.
     * 
     * @param out The stream to write to.
     * @param e The exception.
     */
    public static void printException(PrintStream out, SQLException e) {
        
        StringBuilder sb = new StringBuilder();
        String lineSep = System.getProperty("line.separator");
        int indent;
        int start;
        
        sb.append("SQL Exception(s) Encountered: ").append(lineSep);
        while (e != null) {
            
            start = sb.length();
            sb.append("[State: ");
            sb.append(e.getSQLState());
            sb.append("][Code: ");
            sb.append(e.getErrorCode() + "]: ");
            
            indent = sb.length() - start;
            
            LineIterator iter = new WordWrapLineIterator(e.getMessage(),
                79 - indent);
            
            int line = 0;
            while (iter.hasNext()) {
                
                if (line > 0) {
                    
                    for (int i = 0; i < indent; i++) {
                        
                        sb.append(' ');
                    }
                }
                
                sb.append(iter.next());
                sb.append(lineSep);
                ++line;
            }
            
            e = e.getNextException();
        }
        
        out.print(sb.toString());
        out.flush();
    }
    
    /**
     * Helper method available to all commands to dump any warnings
     * associated with a connection. The set of warnings is cleared
     * after display.
     * 
     * @param out The output stream to write warnings to.
     * @param conn The connection that may, or may not, contain warnings.
     */
    static public void printWarnings(PrintStream out, Connection conn) {
        
        try {
            
            SQLWarning w = conn.getWarnings();
            if (w != null) {
                
                printWarnings(out, w);
                conn.clearWarnings();
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * Helper method available to all commands to dump any warnings
     * associated with a statement. The set of warnings is cleared
     * after display.
     * 
     * @param out The output stream to write warnings to.
     * @param statement The statement that may, or may not, contain warnings.
     */
    static public void printWarnings(PrintStream out, Statement statement) {
        
        try {
            
            SQLWarning w = statement.getWarnings();
            if (w != null) {
                
                printWarnings(out, w);
                statement.clearWarnings();
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * Helper method available to all commands to dump any warnings
     * associated with a ResultSet. The set of warnings is cleared
     * after display.
     * 
     * @param out The output stream to write warnings to.
     * @param results The ResultSet that may, or may not, contain warnings.
     */
    static public void printWarnings(PrintStream out, ResultSet results) {
        
        try {
            
            SQLWarning w = results.getWarnings();
            if (w != null) {
                
                printWarnings(out, w);
                results.clearWarnings();
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * Helper method to allow all commands to dump any warnings that
     * may have occured due to a JDBC call. This method will dump
     * nothing if no warnings have ocurred.
     * 
     * @param out The stream to write to.
     * @param w The warning.
     */
    static private void printWarnings(PrintStream out, SQLWarning w) {
        
        StringBuilder sb = new StringBuilder();
        String lineSep = System.getProperty("line.separator");
        int indent;
        int start;
        
        while (w != null) {
            
            start = sb.length();
            
            String state = w.getSQLState();
            
            /*
             * I don't know if this will be true of all JDBC drivers, 
             * but for certain types of messages I don't like to
             * show the WARN and error code components.
             */
            if (state != null 
                    && "01000".equals(state) == false) {
                
                sb.append("WARN [State: ");
            	sb.append(w.getSQLState());
            	sb.append("][Code: ");
            	sb.append(w.getErrorCode() + "]: ");
            }
            
            indent = sb.length() - start;
            LineIterator iter = new WordWrapLineIterator(w.getMessage(),
                79 - indent);
            
            int line = 0;
            while (iter.hasNext()) {
                
                if (line > 0) {
                    
                    for (int i = 0; i < indent; i++) {
                        
                        sb.append(' ');
                    }
                }
                
                sb.append(iter.next());
                sb.append(lineSep);
                ++line;
            }
            
            w = w.getNextWarning();
        }
        
        out.print(sb.toString());
        out.flush();
    }
}
