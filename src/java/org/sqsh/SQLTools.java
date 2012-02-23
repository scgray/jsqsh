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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
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
     * Return the current catalog for a connection.
     * 
     * @param connection The connection
     * @return The catalog.
     */
    public static String getCatalog(Connection connection) {
        
        try {
            
            return connection.getCatalog();
        }
        catch (SQLException e) {
            
            return null;
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
            sb.append(e.getErrorCode());
            sb.append("]: ");
            sb.append(e.getMessage());
            sb.append(lineSep);
            
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
     * @param session The session to use for writing
     * @param conn The connection that may, or may not, contain warnings.
     */
    static public void printWarnings(Session session, Connection conn) {
        
        try {
            
            SQLWarning w = conn.getWarnings();
            if (w != null) {
                
                printWarnings(session, w);
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
     * @param session The session to use for writing
     * @param statement The statement that may, or may not, contain warnings.
     */
    static public void printWarnings(Session session, Statement statement) {
        
        try {
            
            SQLWarning w = statement.getWarnings();
            if (w != null) {
                
                printWarnings(session, w);
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
     * @param session The session to use for writing
     * @param results The ResultSet that may, or may not, contain warnings.
     */
    static public void printWarnings(Session session, ResultSet results) {
        
        try {
            
            SQLWarning w = results.getWarnings();
            if (w != null) {
                
                printWarnings(session, w);
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
     * @param session The session to use for writing
     * @param w The warning.
     */
    static private void printWarnings(Session session, SQLWarning w) {
        
        StringBuilder sb = new StringBuilder();
        String lineSep = System.getProperty("line.separator");
        int indent;
        int start;
        boolean isError = false;
        
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
                
                isError = true;
                
                sb.append("WARN [State: ");
            	sb.append(w.getSQLState());
            	sb.append("][Code: ");
            	sb.append(w.getErrorCode() + "]: ");
            }
            
            sb.append(w.getMessage());
            sb.append(lineSep);
            
            w = w.getNextWarning();
        }
        
        if (isError) {
            
            session.err.print(sb.toString());
            session.err.flush();
        }
        else {
            
            session.out.print(sb.toString());
            session.out.flush();
        }
        
    }
    
    /**
     * Given a database object identifier (the name of an object)
     * attempts to determine if the identifier needs quotes around
     * it. The rule is pretty simple, if the identifier contains
     * anything other than letters, digits, or an underscore, it is
     * quoted.
     * 
     * @param identifier The identifier to check
     * @return The identifier or the identifier with quotes, if 
     *   necessary.
     */
    public static String quoteIdentifier (String identifier) {

        int len = identifier.length();
        boolean needQuotes = false;
        for (int j = 0; needQuotes == false && j < len; j++) {

            char ch = identifier.charAt(j);
            if (!(Character.isLetter(ch)
                    || Character.isDigit(ch)
                    || ch == '_')) {

                needQuotes = true;
            }
        }
        
        if (needQuotes) {
            
            StringBuilder sb = new StringBuilder();
            sb.append('"')
                .append(identifier)
                .append('"');
            
            return sb.toString();
        }
        
        return identifier;
    }
    
    /**
     * Given a SQL data type, returns true if the type should be quoted
     * when displayed.
     * @param type The type
     * @return true fi the type should be quoted.
     */
    public static boolean needsQuotes (int type) {
        
        boolean needsQuotes = false;
        
        switch (type) {
        
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
        case Types.BLOB:
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
        case Types.LONGVARCHAR:
        case Types.NCHAR:
        case Types.CLOB:
        case Types.NCLOB:
        case Types.DATE:
        case Types.TIME:
        case Types.TIMESTAMP:
        case Types.SQLXML:
            needsQuotes = true;
            break;
            
        default:
            break;
        }
        
        return needsQuotes;
    }
    
    
    /**
     * This method is used to convert a {@link java.sql.Type} datatype
     * value to a displayable string. This conversion is done by reflecting
     * the {@link java.sql.Type} class.  The reason this method exists
     * is that the actual string returned by 
     * {@link java.sql.ResultSetMetaData#getColumnTypeName(int)} is dependant
     * upon the driver and does not reflect the actual JDBC datatype.
     * 
     * @param type The type id
     * @return The name of the type
     */
    public static String getTypeName(int type) {
        
        Field []fields = Types.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            
            int flags = fields[i].getModifiers();
            if (Modifier.isPublic(flags)
                    && Modifier.isStatic(flags)
                    && fields[i].getType() == Integer.TYPE) {
                
                try {
                    
                    int typeValue = fields[i].getInt(null);
                	if (typeValue == type) {
                    
                    	return fields[i].getName();
                	}
                }
                catch (IllegalAccessException e) {
                    
                    /* IGNORED */
                }
            }
        }
        
        return "[TYPE #" + type + "]";
    }
    
    /**
     * Used to parse a database object name of the forms:
     * <pre>
     *    a
     *    a.b
     *    a.b.c
     *    a.b.c.d
     * </pre>
     * and does its best to determine which part of the name is
     * what (e.g. "a.b" is assumed to be "schema.name".
     * 
     * <p>Currently the parsing logic doesn't deal with any sort of
     * quoting, such as [my schema].[my table] or "my schema"."my table";
     * it simply looks at the dots and does the work.
     * 
     * @param name The name to be parsed.
     * @return A parsed version of the name.
     */
    public static ObjectDescription parseObjectName (String name) {
        
        return new ObjectDescription(name);
    }
    
    /**
     * Returned by parseObjectName() as an object description.
     */
    public static class ObjectDescription {
        
        private String name = null;
        private String column = null;
        private String schema = null;
        private String catalog = null;
        
        /**
         * Creates a new object description by parsing the 
         * provided name.
         */
        protected ObjectDescription (String str) {
            
            String []parts = str.split("\\.");
            
            switch (parts.length) {
                
                case 0 :
                    break;
                
                case 1 : 
                    name = parts[0];
                    break;
                
                case 2 :
                    schema = parts[0];
                    name = parts[1];
                    break;
                    
                case 3 :
                    catalog = parts[0];
                    schema = parts[1];
                    name = parts[2];
                    break;
                    
                case 4 :
                default :
                    catalog = parts[0];
                    schema = parts[1];
                    name = parts[2];
                    column = parts[3];
                    break;
            }
        }

        
        /**
         * @return the name
         */
        public String getName () {
        
            return name;
        }

        /**
         * @return the column
         */
        public String getColumn () {
        
            return column;
        }

        /**
         * @return the schema
         */
        public String getSchema () {
        
            return schema;
        }
        
        /**
         * @return the catalog
         */
        public String getCatalog () {
        
            return catalog;
        }
        
        public String toString() {
            
            StringBuilder sb = new StringBuilder();
            if (catalog != null) {
                
                sb.append("[").append(catalog).append("]");
            }
            if (schema != null) {
                
                sb.append("[").append(schema).append("]");
            }
            if (schema != null) {
                
                sb.append("[").append(name).append("]");
            }
            if (column != null) {
                
                sb.append("[").append(column).append("]");
            }
            
            return sb.toString();
        }
    }
}
