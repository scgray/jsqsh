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
import java.util.List;

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
    
    /**
     * Given a SQL statement or batch of SQL statements, this method
     * performs a <b>very</b> loose parse of the SQL attempting to determine
     * tables that are referenced within the INSERT, SELECT, UPDATE, and
     * DELETE clauses contained therein. I must stress the looseness of
     * this parsing, it is very sloppy and is only a best-guess logic
     * at best.
     * 
     * @param sql The SQL statement to be parsed.
     * @param allStatements If true, then table names referenced in all
     *   SQL statements will be returned, otherwise just the last statement
     *   in the batch will be considered.
     * @return A list of tables references in the SQL (if they can be found)
     * and empty list will be returned otherwise.
     */
    public static TableReference[] getTableReferences(String sql,
            boolean allStatements) {
        
        List<TableReference> tableRefs = new ArrayList<TableReference>();
        SimpleSQLTokenizer tokenizer = new SimpleSQLTokenizer(sql);
        
        /*
         * We keep track of whether or not we are nested in a set of
         * parentheses. If we are, then we assume we could be part of
         * a sub-query. If we are not, then we clear our current set of
         * table references for each new INSERT/UPDATE/DELETE/SELECT we
         * hit.
         */
        int parenCount = 0;
        
        String token = tokenizer.next();
        while (token != null) {
            
            if ("DELETE".compareToIgnoreCase(token) == 0) {
                
                if (allStatements == false
                        && parenCount == 0) {
                    
                    tableRefs.clear();
                }
                doDelete(tableRefs, tokenizer);
            }
            else if ("SELECT".compareToIgnoreCase(token) == 0) {
                
                if (allStatements == false
                        && parenCount == 0) {
                    
                    tableRefs.clear();
                }
            }
            else if ("UPDATE".compareToIgnoreCase(token) == 0
                    || "INSERT".compareToIgnoreCase(token) == 0) {
                
                if (allStatements == false 
                        && parenCount == 0) {
                    
                    tableRefs.clear();
                }
                
                doInsertUpdate(tableRefs, tokenizer);
            }
            else if ("FROM".compareToIgnoreCase(token) == 0) {
                
                doFrom(tableRefs, tokenizer);
            }
            else if ("(".equals(token)) {
                
                ++parenCount;
            }
            else if (")".equals(token)) {
                
                --parenCount;
            }
            
            token = tokenizer.next();
        }
        
        return tableRefs.toArray(new TableReference[0]);
    }
    
    /**
     * Called after the tokenizer hits a DELETE statement and attempts
     * to determine the table name that is being deleted.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private static void doDelete(List<TableReference>tableRefs, 
            SimpleSQLTokenizer tokenizer) {
        
        String token = tokenizer.next();
        if (token != null && "FROM".compareToIgnoreCase(token) == 0) {
            
            token = tokenizer.next();
        }
        
        if (token != null) {
            
            tableRefs.add(doObjectName(tokenizer, token));
        }
    }
    
    /**
     * Called after the tokenizer hits an INSERT/UPDATE statement and attempts
     * to determine the table name that is being updated or inserted into.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private static void doInsertUpdate(List<TableReference>tableRefs, 
            SimpleSQLTokenizer tokenizer) {
        
        String token = tokenizer.next();
        if (token != null) {
            
            tableRefs.add(doObjectName(tokenizer, token));
        }
    }
    
    /**
     * Called after the tokenizer hits a FROM clause and attempts
     * to determine the tables that are being selected from. Currently
     * this parser cannot understand ANSI inner/outer join syntax.
     * 
     * @param tableRefs The list of references found thus far.
     * @param tokenizer The current parsing state.
     */
    private static void doFrom(List<TableReference>tableRefs, 
            SimpleSQLTokenizer tokenizer) {
        
        String token = tokenizer.next();
        boolean done = false;
        while (token != null && !done) {
            
            TableReference table = null;
            
            /*
             * The starting token is expected to be the name of an
             * object that we are selecting from.
             */
            char ch = token.charAt(0);
            if (Character.isLetter(ch) || ch == '_') {
                
                table = doObjectName(tokenizer, token);
                
                /*
                 * Next we could have the keyword 'as', if so, ignore it.
                 */
                token = tokenizer.next();
                if (token != null
                        && "AS".compareToIgnoreCase(token) == 0) {
                    
                    token = tokenizer.next();
                }
                
                /*
                 * Next we could have the alias for the table.
                 */
                if (token != null
                        && "WHERE".compareToIgnoreCase(token) != 0
                        && (token.charAt(0) == '_'
                            || Character.isLetter(token.charAt(0)))) {
                    
                    table.setAlias(token);
                    token = tokenizer.next();
                }
                
                /*
                 * At this point we have parsed:
                 *     FROM table [AS alias]
                 * At this point I am going to skip forward until I hit 
                 * a comma or a WHERE. I do this because some databases
                 * allow for hints and various other alterations following
                 * the table name and I don't want to worry about the 
                 * specifics of that syntax.
                 * 
                 * Basically, this allows for logic that handles:
                 * 
                 *    FROM table [AS ALIAS] [STUFF], table ...
                 */
                while (token != null 
                        && "WHERE".compareToIgnoreCase(token) != 0
                        && token.equals(",") == false) {
                            
                    token = tokenizer.next();
                }
                
                if (token != null
                        && token.equals(",")) {
                    
                    token = tokenizer.next();
                }
                else {
                    
                    done = true;
                }
                
                tableRefs.add(table);
            }
            else {
                
                done = true;
            }
        }
        
        if (token != null) {
            
            tokenizer.unget(token);
        }
    }
    
    
    /**
     * Called when the parser has hit an object it thinks is the name of
     * a database object. This purpose of this method is to try to see
     * if the object is a multi.part.name and will take care of sucking
     * in the rest of the name.
     * 
     * @param tokenizer The tokenizer
     * @param name The object name (or at least the first part)
     * @return The full object.name.
     */
    private static TableReference doObjectName(
            SimpleSQLTokenizer tokenizer, String name) {
        
        List<String> parts = new ArrayList<String>();
        parts.add(name);
        
        String token = tokenizer.next();
        if (token != null && token.equals(".")) {
            
            while (token != null && token.equals(".")) {
                
                token = tokenizer.next();
                if (token != null) {
                    
                    if (token.equals(".")) {
                        
                        parts.add("");
                        tokenizer.unget(token);
                    }
                    else {
                        
                        parts.add(token);
                    }
                    
                    token = tokenizer.next();
                }
            }
        }
        
        if (token != null) {
            
            tokenizer.unget(token);
        }
        
        String catalog = null;
        String owner = null;
        String table = null;
        if (parts.size() == 1) {
            
            table = parts.get(0);
        }
        else if (parts.size() == 2) {
            
            owner = parts.get(0);
            table = parts.get(1);
        }
        else if (parts.size() == 3) {
            
            catalog = parts.get(0);
            owner = parts.get(1);
            table = parts.get(2);
        }
        
        return new TableReference(catalog, owner, table);
    }
    
    /**
     * This class is used by getTableReferences() to return the set
     * of tables that are referenced by a SQL statement.
     */
    public static class TableReference {
        
        private String database;
        private String owner;
        private String table;
        private String alias;
        
        public TableReference (String database, String owner, String table) {
            
            this.database = database;
            this.owner = owner;
            this.table = table;
        }
    
        public String getDatabase() {
            
            return database;
        }
        
        public String getOwner() {
            
            return owner;
        }
        
        public String getTable() {
            
            return table;
        }
        
        public String getAlias() {
            
            return alias;
        }
        
        public void setAlias(String alias) {
            
            this.alias = alias;
        }
    }
}
