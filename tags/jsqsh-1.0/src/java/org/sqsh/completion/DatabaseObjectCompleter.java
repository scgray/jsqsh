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
package org.sqsh.completion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.Session;
import org.sqsh.parser.AbstractParserListener;
import org.sqsh.parser.SQLParser;
import org.sqsh.parser.DatabaseObject;

/**
 * This class implements tab completion of database objects based upon 
 * a rudamentary parsing of the current SQL buffer.  One of the main
 * limitations that it has is that it cannot <b>exactly</b> determine
 * where the cursor is sitting inside of the SQL buffer because readline
 * doesn't quite give us the ability to determine (or, more exactly, the
 * JNI interface to readline doesn't expose it).  The logic this implements
 * tries really really hard to determine where you are in the SQL but
 * may it get it wrong from time to time.
 */
public class DatabaseObjectCompleter
    extends Completer {
    
    private static final Logger LOG = 
        Logger.getLogger("org.sqsh.completion.DatabaseObjectCompleter");
    
    /**
     * Used internally to keep track of what sort of quoting was taking
     * place on the object name that the user was completing.
     */
    private enum QuoteType { NONE, BRACKET, QUOTES };
    
    /**
     * The SQL statement that the user was building.
     */
    private String sql;
    private QuoteType quote = QuoteType.NONE;
    private Iterator<String> iter = null;
    
    public DatabaseObjectCompleter(Session session,
            String line, int position, String word) {
        
        super(session, line, position, word);
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Completing '"
                + line + "' at position "
                + position + ", current word '" + word + "'");
        }
        
        /*
         * Take a look at the first character in the word that the user is
         * sitting on. If it is a SQL object quote, then we will retain that
         * information to ensure that all completions are completed with
         * the quotes left in place.
         */
        char ch = (word.length() == 0 ? (char) 0 : word.charAt(0));
        if (ch == '[') {
            
            quote = QuoteType.BRACKET;
        }
        else if (ch == '"') {
            
            quote = QuoteType.QUOTES;
        }
        else {
            
            quote = QuoteType.NONE;
        }
        
        /*
         * First we will blast through the current line of input up to
         * the point at which the user hit the completion key and look for
         * the name of the object that the user was sitting on. This
         * name will be returned as its component parts.
         */
        String nameParts[] = getObjectName(line.substring(0, position));
        
        if (LOG.isLoggable(Level.FINE)) {

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nameParts.length; i++) {
                
                if (i > 0) {
                    
                    sb.append('.');
                }
                sb.append('[');
                sb.append(nameParts[i]);
                sb.append(']');
            }
            
            LOG.fine("   Parsed object name is: " + sb.toString());
        }
        
        /*
         * Our total SQL statement is what the user has entered to date
         * plus the line of input that the user is working on.
         */
        sql = session.getBufferManager().getCurrent().toString() + line;
        
        /*
         * Now, we will parse the SQL and accumulate information about the
         * statement that the user is currently working on.
         */
        StatementInfo info = new StatementInfo();
        SQLParser parser = new SQLParser(info);
        parser.parse(sql);
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("   Parsing state:");
            LOG.fine("      Statement  = " + info.getStatement());
            LOG.fine("      Clause     = " + info.getCurrentClause());
            
            StringBuilder sb = new StringBuilder();
            int idx = 0;
            for (DatabaseObject o : info.getObjectReferences()) {
                
                if (idx > 0) {
                    
                    sb.append(", ");
                }
                ++idx;
                
                sb.append(o.toString());
            }
            LOG.fine("      References = " + sb.toString());
        }
        
        /*
         * Grab ahold of the tables that are being referenced by the
         * current SQL statement.
         */
        DatabaseObject []tableRefs = info.getObjectReferences();
        Set completions = null;
        
        if (info.getStatement() != null) {
            
            if ("EXECUTE".equals(info.getStatement())) {
                
                completions = getExecuteCompletions(info, tableRefs, nameParts);
            }
            else {
                
                completions = getQueryCompletions(info, tableRefs, nameParts);
            }
        }
        
        
        if (completions != null) {
            
            iter = completions.iterator();
        }
        else {
            
            iter = null;
        }
    }
    
    /**
     * Returns the next string available in the completion list.
     * @return The next string in the completion list or null if no
     *   more strings are available.
     */
    public String next() {
        
        if (iter == null || iter.hasNext() == false) {
                
            return null;
        }
            
        String s = iter.next();
        if (quote == QuoteType.BRACKET) {
                
            return "[" + s + "]";
        }
        else if (quote == QuoteType.QUOTES) {
                
            return '"' + s + '"';
        }
            
        return s;
    }
    
    /**
     * Used to process completions for SELECT, INSERT, UPDATE, or
     * DELETE statements.
     * 
     * @param info The state of the query.
     * 
     * @param tableRefs The set of tables that are referenced by the
     *    query.
     * @param nameParts The name that the user is currently typing in.
     * @return The set of completions.
     */
    private Set<String> getQueryCompletions(StatementInfo info,
        DatabaseObject []tableRefs, String []nameParts) {
        
        /*
         * If the user hasn't completed enough of the statement to
         * know which tables the query is referencing, or if the user
         * is still sitting in the from clause, then generate all possible
         * completions.
         */
        if (tableRefs.length == 0 
                || info.getCurrentClause() == null
                || "FROM".equals(info.getCurrentClause())) {
            
            return getAllCompletions(nameParts);
        }
        else {
            
            return getReferencedCompletions(tableRefs, nameParts);
        }
    }
    
    /**
     * Determines possible completions for an EXECUTE statement. These
     * can be procedure names or column names.
     * 
     * @param info Information about the current state of the parsed
     *   SQL.
     * @param objectRefs The objects that are referenced so far in 
     *   the SQL. This could be empty or the procedure name.
     * @param nameParts The name(s) that the user has entered so far.
     * @return The completion set.
     */
    private Set<String> getExecuteCompletions(StatementInfo info,
        DatabaseObject []objectRefs, String []nameParts) {
        
        Set<String> set = new TreeSet<String>();
        Connection conn = session.getConnection();
        
        /*
         * If the parser found the name of the procedure then we
         * are working on completing a parameter name...
         */
        if (objectRefs.length > 0) {
            
            DatabaseObject o = objectRefs[0];
            getProcedureParameters(set, o, 
                (nameParts.length > 0 ? nameParts[0] : null));
        }
        
        /*
         * I have a small problem here. If there user hits:
         * 
         *    EXEC proc<tab>
         *    
         * I don't have enough information at my disposal to tell 
         * the different between this:
         * 
         *    EXEC proc <tab>
         * 
         * In the first case the user was trying to complete the 
         * procedure name and in the second case the user was trying
         * to complete the parameters for the procedure.
         * 
         * Since I can't tell the difference, I am going to try to
         * guess. If I cannot find any parameters for "proc" then I
         * assume that the procedure doesn't exist and that we are 
         * trying to complete it.
         */
        if (set.size() == 0) {
            
            String catalog = getCurrentCatalog(conn);
            
            if (nameParts.length == 0) {
                
                getCatalogs(set, conn, "%");
                getProcedures(set, conn, catalog, "%", "%");
            }
            else if (nameParts.length == 1) {
                
                getCatalogs(set, conn, nameParts[0]);
                getProcedures(set, conn, catalog, "%", nameParts[0]);
            }
            else if (nameParts.length == 2) {
                
                getProcedures(set, conn, catalog, nameParts[0], nameParts[1]);
            }
            else {
                
                getProcedures(set, conn,
                    nameParts[0], nameParts[1], nameParts[2]);
            }
        }
        
        return set;
    }
    
    
    private Set<String> getReferencedCompletions(DatabaseObject []tableRefs,
        String []nameParts) {
        
        Set<String> set = new TreeSet<String>();
        Connection conn = session.getConnection();
        
        /*
         * This may be needed a lot.
         */
        String catalog = getCurrentCatalog(conn);
        
        /*
         * If nothing was entered, then it could be anything, really.
         */
        if (nameParts.length == 0) {
            
            getReferencedAliases(set, tableRefs, null);
            getReferencedCatalogs(set, tableRefs, null);
            getReferencedSchemas(set, tableRefs, null, null);
            getReferencedTables(set, tableRefs, null, null, null);
            getReferencedColumns(set, tableRefs, null, null, null, null);
        }
        else  if (nameParts.length == 1) {
            
            /*
             * 1. alias
             * 2. catalog
             * 3. schema
             * 4. table
             * 5. column
             */
            getReferencedAliases(set, tableRefs, nameParts[0]);
            getReferencedCatalogs(set, tableRefs, nameParts[0]);
            getReferencedSchemas(set, tableRefs, null, nameParts[0]);
            getReferencedTables(set, tableRefs, null, null, nameParts[0]);
            getReferencedColumns(set, tableRefs, null, null, null, nameParts[0]);
        }
        else if (nameParts.length == 2) {
           
            /*
             * 1. alias.column
             * 2. catalog.schema
             * 2. schema.table
             * 3. table.column
             */
            getReferencedColumns(set, tableRefs, nameParts[0], nameParts[1]);
            getReferencedSchemas(set, tableRefs, nameParts[0], nameParts[1]);
            getReferencedTables(set, tableRefs, null, nameParts[0], nameParts[1]);
            getReferencedColumns(set, tableRefs, null, null,
                nameParts[0], nameParts[1]);
        }
        else if (nameParts.length == 3) {
            
            /*
             * 1. catalog.schema.table
             * 2. schema.table.column
             */
            getReferencedTables(set, tableRefs,
                nameParts[0], nameParts[1], nameParts[2]);
            getReferencedColumns(set, tableRefs, null, 
                nameParts[0], nameParts[1], nameParts[2]);
        }
        else if (nameParts.length == 4) {
            
            getReferencedColumns(set, tableRefs, 
                nameParts[0], nameParts[1], nameParts[2], nameParts[3]);
        }
        
        return set;
    }
    
    /**
     * Returns the set of database/catalog names that are referenced in 
     * the current query that start with the supplied name.
     * 
     * @param set The place to put matches.
     * @param tableRefs The set of tables referenced by the current query.
     * @param name The name of the catalog the user is typing or null if
     *   none has been entered yet.
     */
    protected void getReferencedCatalogs(Set<String> set, 
            DatabaseObject []tableRefs, String name) {
        
        if ("".equals(name)) {
            
            name = null;
        }
        
        for (DatabaseObject ref : tableRefs) {
            
            if (ref.getDatabase() != null) {
                
                if (name == null 
                        || ref.getDatabase().startsWith(name)) {
                
                    set.add(ref.getDatabase());
                }
            }
        }
    }
    
    /**
     * Returns the set of aliases that start with the supplied name
     * 
     * @param set The place to put matches.
     * @param tableRefs The set of tables referenced by the current query.
     */
    protected void getReferencedAliases(Set<String> set, 
            DatabaseObject []tableRefs, String alias) {
        
        if ("".equals(alias)) {
            
            alias = null;
        }
        
        for (DatabaseObject ref : tableRefs) {
            
            if (ref.getAlias() != null) {
                
                if (alias == null 
                        || ref.getAlias().startsWith(alias)) {
                
                    set.add(ref.getAlias());
                }
            }
        }
    }
    
    /**
     * Returns the set of schema names that are referenced in 
     * the current query that start with the supplied name.
     * 
     * @param set The place to put matches.
     * @param tableRefs The set of tables referenced by the current query.
     */
    protected void getReferencedSchemas(Set<String> set,
            DatabaseObject []tableRefs, String catalog, String schema) {
        
        if ("".equals(catalog)) {
            
            catalog = null;
        }
        if ("".equals(schema)) {
            
            schema = null;
        }
        
        for (DatabaseObject ref : tableRefs) {
            
            if (ref.getOwner() != null) {
            
                if ((catalog == null && ref.getDatabase() == null)
                    || catalog != null && catalog.equals(ref.getDatabase())) {
                
                    if (schema == null 
                        || (ref.getOwner().startsWith(schema))) {
                    
                        set.add(ref.getOwner());
                    }
            	}
            }
        }
    }
    
    /**
     * Returns the set of schema names that are referenced in 
     * the current query that start with the supplied name.
     * 
     * @param set The place to put matches.
     * @param tableRefs The set of tables referenced by the current query.
     */
    protected void getReferencedTables(Set<String> set,
            DatabaseObject []tableRefs,
            String catalog, String schema, String table) {
        
        if ("".equals(catalog)) {
            
            catalog = null;
        }
        if ("".equals(schema)) {
            
            schema = null;
        }
        if ("".equals(table)) {
            
            table = null;
        }
        
        for (DatabaseObject ref : tableRefs) {
            
            if ((catalog == null && ref.getDatabase() == null)
                    || catalog != null && catalog.equals(ref.getDatabase())) {
                
                if ((schema == null && ref.getOwner() == null)
                    || schema != null && schema.equals(ref.getOwner())) {
                    
                    if (table == null
                            || ref.getName().startsWith(table)) {
                        
                        set.add(ref.getName());
                    }
            	}
            }
        }
    }
    
    /**
     * Returns the set of columns that could belong to the current request.
     * 
     * @param set The set of completions found thus far.
     * @param tableRefs The tables referenced by the query.
     * @param catalog The catalog the user listed, or null is none was provided.
     * @param schema The schema the user listed, or null is none was provided.
     * @param table The table the user listed, or null is none was provided.
     * @param column The portion of the column name the user provided or null
     *    if none was provided.
     */
    protected void getReferencedColumns(Set<String> set,
            DatabaseObject []tableRefs,
            String catalog, String schema, String table, String column) {
        
        Connection conn = session.getConnection();
        
        if ("".equals(catalog)) {
            
            catalog = null;
        }
        if ("".equals(schema)) {
            
            schema = null;
        }
        if ("".equals(table)) {
            
            table = null;
        }
        if ("".equals(column)) {
            
            column = null;
        }
        
        for (DatabaseObject ref : tableRefs) {
            
            if (catalog == null
                    || (catalog != null && catalog.equals(ref.getDatabase()))) {
                
                if (schema == null
                    || schema != null && schema.equals(ref.getOwner())) {
                    
                    if (table == null
                            || ref.getName().equals(table)) {
                        
                        catalog = (catalog == null ?
                                getCurrentCatalog(conn) : catalog);
                        schema = (schema == null ? "%" : schema);
                        table = ref.getName();
                        column = (column == null ? "%" : column);
                        
                        getColumns(set, conn, catalog, schema, table, column);
                    }
            	}
            }
        }
    }
    
    /**
     * Another method for looking up column names.
     * @param set
     * @param tableRefs
     * @param alias
     * @param column
     */
    protected void getReferencedColumns(Set<String> set,
            DatabaseObject []tableRefs, String alias, String column) {
        
        Connection conn = session.getConnection();
        
        for (DatabaseObject ref : tableRefs) {
            
            if (ref.getAlias() != null
                    && ref.getAlias().equals(alias)) {
                
                String catalog = (ref.getDatabase() == null 
                        ? getCurrentCatalog(conn) : ref.getDatabase());
                String schema = (ref.getOwner() == null
                        ? "%" : ref.getOwner());
                String table = ref.getName();
                
                getColumns(set, conn, catalog, schema, table, column);
            }
        }
    }
    
    /**
     * Returns all possible completions for the partially qualified object
     * name provided.
     * 
     * @param nameParts The name of a database object to be completed.
     * @return The list of completions or null if none are avialable.
     */
    private Set<String> getAllCompletions(String []nameParts) {
        
        Set<String> set = new TreeSet<String>();
        Connection conn = session.getConnection();
        
        /*
         * The completion list would be way to large without any 
         * qualifiers.
         */
        if (nameParts.length == 0) {
            
            return null;
        }
        
        /*
         * This may be needed a lot.
         */
        String catalog = getCurrentCatalog(conn);
        
        /*
         * A single name could be:
         *   1. Catalog name
         *   2. Table name
         *   3. Procedure name
         */
        if (nameParts.length == 1) {
            
            getCatalogs(set, conn, nameParts[0]);
            getTables(set, conn, catalog, "%", nameParts[0]);
            getProcedures(set, conn, catalog, "%", nameParts[0]);
        }
        else if (nameParts.length == 2) {
           
            /*
             * If we have two parts ("name.name") we could have:
             * 
             *    1. catalog.schema
             *    2. table.column
             *    3. schema.table
             *    4. schema.procedure
             *    
             *  For now I am going to skip #1 since I don't have a convenient
             *  way (in JDK 5) to ask for a list of schema in a catalog.
             */
            getColumns(set, conn, catalog, (String) null, nameParts[0],
                nameParts[1]);
            getTables(set, conn, catalog, nameParts[0], nameParts[1]);
            getProcedures(set, conn, catalog, nameParts[0], nameParts[1]);
        }
        else if (nameParts.length == 3) {
            
            /*
             * Three parts could be:
             * 
             *    1. catalog.schema.table
             *    2. catalog.schema.procedure
             *    3. schema.table.column
             */
            getTables(set, conn, nameParts[0], nameParts[1], nameParts[2]);
            getProcedures(set, conn, nameParts[0], nameParts[1], nameParts[2]);
            getColumns(set, conn, catalog, nameParts[0], nameParts[1],
                nameParts[2]);
        }
        else if (nameParts.length == 4) {
            
            /*
             * database.table.owner.column
             */
            getColumns(set, conn, nameParts[0], nameParts[1], nameParts[2],
                nameParts[3]);
        }
        
        return set;
    }
    
    /**
     * Returns the set of parameters available for a given stored
     * procedure.
     * 
     * @param set The current completion set.
     * @param proc The name of the procedure.
     * @param paramPart The part of the parameter name that the
     *   user has typed so far.
     */
    protected void getProcedureParameters(Set<String> set,
            DatabaseObject proc, String paramPart) {
        
        Connection conn = session.getConnection();
        String catalog = getCurrentCatalog(conn);
        String schema = "%";
        
        if (proc.getDatabase() != null) {
            
            catalog = proc.getDatabase();
        }
        if (proc.getOwner() != null && proc.getOwner().length() > 0) {
            
            schema = proc.getOwner();
        }
        if (paramPart == null) {
            
            paramPart = "%";
        }
        else {
            
            paramPart = paramPart + "%";
        }
        
        try {
            
            ResultSet results = conn.getMetaData().getProcedureColumns(
                catalog, schema, proc.getName(), paramPart);
            
            while (results.next()) {
                
                String name = results.getString(4);
                set.add(name);
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * Returns the current catalog for a connection.
     * @param conn  The connection
     * @return The current catalog or null if there is none.
     */
    protected String getCurrentCatalog(Connection conn) {
        
        try {
            
            return conn.getCatalog();
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        return null;
    }
    
    /**
     * Returns the set of catalogs that start with a name.
     * 
     * @param set The current set of object names.
     * @param conn The connection to the database.
     * @param name The prefix of the catalog name.
     */
    protected void getCatalogs(Set<String> set, Connection conn, String name) {
        
        try {
            
            ResultSet results = conn.getMetaData().getCatalogs();
            while (results.next()) {
                
                String catalog = results.getString(1);
                if (catalog.startsWith(name)) {
                    
                    set.add(catalog);
                }
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    
    /**
     * Gathers the set of tables that matches requested criteria
     * @param set The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param schema The schema (owner) to look for
     * @param tablePrefix The prefix of the table.
     */
    protected void getTables(Set<String> set, Connection conn,
            String catalog, String schema, String tablePrefix) {
        
        if (schema == null || schema.equals("")) {
            
            schema = "%";
        }
        
        try {
            
            ResultSet results = conn.getMetaData().getTables(
                catalog, schema, tablePrefix + "%", null);
            
            while (results.next()) {
                
                String table = results.getString(3);
                set.add(table);
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * Gathers the set of columns that matches requested criteria
     * @param set The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param schema The schema (owner) to look for
     * @param table The table name
     * @param table The column prefix
     */
    protected void getColumns(Set<String> set, Connection conn,
            String catalog, String schema, String table, String columnPrefix) {
        
        if (schema == null || schema.equals("")) {
            
            schema = "%";
        }
        
        try {
            
            ResultSet results = conn.getMetaData().getColumns(
                catalog, schema, table, columnPrefix + "%");
            
            while (results.next()) {
                
                String column = results.getString(4);
                set.add(column);
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * This method is used to look up available columns based upon tables
     * that are referenced within the current SQL statement.  That is,
     * if the user does:
     * <pre>
     *   select count(*) from master..sysobjects as o where o.i<tab>
     * </pre>
     * this logic will see the 'o.' as a reference to the alias to 
     * master..sysobjects and will attempt to look for all columns 
     * that start with 'i'.
     * 
     * @param set The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param tableRefs The set of table names and aliases that are
     *   contained in the SQL
     * @param alias If non-null the alias name by which the table
     *   is being referred.
     * @param columnPrefix the portion of the column name entered so far.
     */
    protected void getColumns(Set<String> set, Connection conn,
            String catalog,  DatabaseObject []tableRefs,
            String alias, String columnPrefix) {
        
        for (DatabaseObject ref : tableRefs) {
            
            if (alias == null
                    || (alias != null && alias.equals(ref.getAlias()))) {
                
                getColumns(set, conn, 
                    (ref.getDatabase() == null ? catalog : ref.getDatabase()),
                    (ref.getOwner() == null ? "%" : ref.getOwner()),
                    ref.getName(),  columnPrefix);
            }
        }
    }
    
    
    /**
     * Gathers the set of tables that matches requested criteria
     * @param set The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param schema The schema (owner) to look for
     * @param tablePrefix The prefix of the table.
     */
    protected void getProcedures(Set<String> set, Connection conn,
            String catalog, String schema, String procPrefix) {
        
        if (schema == null || schema.equals("")) {
            
            schema = "%";
        }
        
        try {
            
            ResultSet results = conn.getMetaData().getProcedures(
                catalog, schema, procPrefix + "%");
            
            while (results.next()) {
                
                String proc = results.getString(3);
                set.add(proc);
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
    }
    
    /**
     * This method walks a line of input from the user up to the point
     * where their cursor is sitting and tries to figure out if the cursor
     * is sitting on an object name. If the cursor is on an object name
     * then the name of the object will be returned.
     * 
     * @param line The line to be processed.
     * @return The object name broken into its component parts. If there
     * was no object found an empty list will be returned.
     */
    protected String[] getObjectName(String line) {
        
        int idx = 0;
        int len = line.length();
        List<String> parts = new ArrayList<String>();
        
        /*
         * Ok, here's what we are doing. When we hit a 
         * word, "quoted word", or [bracketed word] we take that
         * word and append it to our list of object name parts and
         * we continue to do so as long as there is a '.' between
         * those word. As soon as we hit something that is not a '.'
         * we clear out our current list and keep moving on.
         */
        while (idx < len) {
            
            char ch = line.charAt(idx);
            if (Character.isLetter(ch)
                    || ch == '_'
                    || ch == '@') {
                
                idx = doWord(parts, line, idx);
            }
            else if (ch == '"') {
                
                idx = doQuotedWord(parts, line, idx);
            }
            else if (ch == '[') {
                
                idx = doBracketedWord(parts, line, idx);
            }
            else if (ch == '.') {
                
                ++idx;
                if (idx == len || line.charAt(idx) == '.') {
                    
                    parts.add("");
                }
            }
            else {
                
                if (parts.size() > 0) {
                    
                    parts.clear();
                }
                
                ++idx;
            }
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Called by doFindObject() when it has landed on the beginning of 
     * a "word" in a line.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
        word.append(line.charAt(idx));
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = false;
        while (!done) {
            
            if (idx >= len) {
                
                done = true;
            }
            else {
                
                char ch = line.charAt(idx);
                if (Character.isLetter(ch)
                            || Character.isDigit(ch)
                            || ch == '_') {
                    
                    word.append(ch);
                    ++idx;
                }
                else {
                    
                    done = true;
                }
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
    
    /**
     * Called by doFindObject() when it has landed on the beginning of 
     * a quoted word.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doQuotedWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = (idx >= len);
        while (idx < len && !done) {
                        
            char ch = line.charAt(idx);
            if (ch == '"') {
                
                ++idx;
                if (idx < (len - 1) && line.charAt(idx+1) == '"') {
                    
                    word.append('"');
                }
                else {
                    
                    done = true;
                }
            }
            else {
                
                word.append(ch);
                ++idx;
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
    
    /**
     * Called by doBracketedWord() when it has landed on the beginning of 
     * a bracketed word.
     * 
     * @param parts The list of object parts that we have found thus far.
     *   The newly parsed word will be appended to this list.
     * @param line The line being parsed.
     * @param idx The index of the first character of the word.
     * @return The index after parsing.
     */
    private int doBracketedWord(List<String> parts, String line, int idx) {
        
        StringBuilder word = new StringBuilder();
        int len = line.length();
                    
        /*
         * Skip forward until we hit the likely end.
         */
        ++idx;
        boolean done = (idx >= len);
        while (idx < len && !done) {
                        
            char ch = line.charAt(idx);
            if (ch == ']') {
                
                ++idx;
                done = true;
            }
            else {
                
                ++idx;
                word.append(ch);
            }
        }
        
        parts.add(word.toString());
        return idx;
    }
    
    /**
     * This class is used to accumulate information about the SQL statement
     * that the user is entering. 
     */
    private static class StatementInfo
        extends AbstractParserListener {
        
        /**
         * I maintain table references for the queries in stack to allow
         * for subqueries. As we nest into a subquery a new list of 
         * references is added which is discarded when we leave the
         * context of the subquery.
         */
        private Stack<List<DatabaseObject>> refStack
            = new Stack<List<DatabaseObject>>();
        
        /*
         * A place to push the clause in whcih a subquery is located.
         */
        private Stack<String> clauseStack 
            = new Stack<String>();
        
        /**
         * The current statement type.
         */
        private String statement = null;
        
        /**
         * The name of the clause the user is currently sitting in.
         */
        private String clause = null;
        
        public StatementInfo() {
            
            refStack.push(new ArrayList<DatabaseObject>());
        }
        
        public String getStatement() {
            
            return statement;
        }
        
        public String getCurrentClause() {
            
            return clause;
        }
        
        /**
         * @return The set of tables being referenced by the query. An
         * empty list will be returned if there are no table references.
         */
        public DatabaseObject[] getObjectReferences() {
            
            if (refStack.size() == 1) {
                
                return refStack.peek().toArray(new DatabaseObject[0]);
            }
            else {
                
                ArrayList<DatabaseObject> refs = 
                    new ArrayList<DatabaseObject>();
                for (int i = 0; i < refStack.size(); i++) {
                    
                    refs.addAll(refStack.get(i));
                }
                
                return refs.toArray(new DatabaseObject[0]);
            }
        }

        public void enteredSubquery (SQLParser parser) {

            /*
             * As we enter the sub query we push a new "context" onto 
             * our stack.
             */
            refStack.push(new ArrayList<DatabaseObject>());
            clauseStack.push(clause);
        }

        public void exitedSubquery (SQLParser parser) {

            /*
             * And it gets discarded as we leave the subquery.
             */
            refStack.pop();
            clause = clauseStack.pop();
        }

        public void foundClause (SQLParser parser, String clause) {

            this.clause = clause;
        }

        public void foundStatement (SQLParser parser, String statement) {
            
            this.statement = statement;
            clause = null;

            /*
             * Each time we hit a new statement we discard any context
             * of the previous statement we encountered.
             */
            while (refStack.size() > 1) {
                
                refStack.pop();
            }
            
            /*
             * There is always a root element in the stack, so we will
             * just clear it out to save object creations.
             */
            if (refStack.size() == 1) {
                
                refStack.peek().clear();
            }
            else {
                
                refStack.push(new ArrayList<DatabaseObject>());
            }
        }

        public void foundTableReference (SQLParser parser,
                DatabaseObject tableRef) {

            /*
             * Add table references to the current "stack".
             */
            refStack.peek().add(tableRef);
        }

        /* (non-Javadoc)
         * @see org.sqsh.parser.AbstractParserListener#foundProcedureExecution(org.sqsh.parser.SQLParser, org.sqsh.parser.DatabaseObject)
         */
        @Override
        public void foundProcedureExecution (SQLParser parser,
                DatabaseObject procRef) {

            refStack.peek().add(procRef);
        }
        
    }
}
