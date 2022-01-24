/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh.completion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.parser.DatabaseObject;

/**
 * Interface for classes wishing to perform completions for specific
 * SQL statements. 
 */
public abstract class SQLStatementCompleter {
    
    private static final Logger LOG = 
        Logger.getLogger("org.sqsh.completion.SQLStatementCompleter");
    
    protected String statement;
    protected String clause = null;
    
    /**
     * Creates a completer for a specified statement.
     * 
     * @param statement The statement
     */
    public SQLStatementCompleter (String statement) {
        
        this.statement = statement;
    }
    
    /**
     * Creates a completer for a specified statement and clause.
     * 
     * @param statement The statement
     * @param clause The clause
     */
    public SQLStatementCompleter (String statement, String clause) {
        
        this.statement = statement;
        this.clause = clause;
    }
    
    /**
     * @return the statement
     */
    public String getStatement () {
    
        return statement;
    }

    /**
     * @return the clause
     */
    public String getClause () {
    
        return clause;
    }
    
    /**
     * Called to retrieve list of completions based upon that current
     * SQL statement being entered by the user and the partial database 
     * object name that the user is typing.
     * 
     * @param completions The current set of completions. The implementor
     *   of this method is expected to add any completions it finds (if any)
     *   to this set before returning.
     * @param conn Database connection to be used to perform object name
     *   completions.
     * @param nameParts This is an array of portions of the object 
     *   name that the user has entered thus far. If the user has
     *   entered nothing, then the array will be of length zero. If the
     *   array has two elements, that means the user has entered 
     *   "name.name", etc.
     * @param parseState This represents the information that was gleaned by
     *   parsing the current SQL statement.
     */
    public abstract void getCompletions(Set<String> completions,
            Connection conn, String []nameParts,
            SQLParseState parseState);
    
    /**
     * A helper method available to implementors of SQLStatementCompletion
     * to determine all of the possible completions that match database
     * objects that are directly referenced by the query. This is primarily
     * useful for completion of WHERE clause or SELECT clauses based upon 
     * the contents of the FROM clause.
     * 
     * @param completions The current set of completions. The implementor
     *   of this method is expected to add any completions it finds (if any)
     *   to this set before returning.
     * @param nameParts This is an array of portions of the object 
     *   name that the user has entered thus far.
     * @param parseState This represents the information that was gleaned by
     *   parsing the current SQL statement.
     */
    protected void getReferencedCompletions(Set<String> completions,
            String []nameParts, SQLParseState parseState) {
        
        DatabaseObject []referencedObjects = parseState.getObjectReferences();
        
        /*
         * If there are no objects being referenced in the current SQL 
         * statement then there is no work to do.
         */
        if (referencedObjects == null
                || referencedObjects.length == 0) {
            
            return;
        }
        
        /*
         * Now, move through the referenced objects. We will then compare
         * those objects to the information that the user has provided to
         * determine possible completions.
         */
        for (DatabaseObject ref : referencedObjects) {
            
            /*
             * If the reference has an alias and the user either has not
             * entered anything or has only entered part of the name of
             * the object, then the alias could be part of our completion set.
             */
            if (ref.getAlias() != null
                    &&  (nameParts.length == 0 
                         || (nameParts.length == 1 
                              && ref.getAlias().startsWith(nameParts[0])))) {
                
                completions.add(ref.getAlias());
            }
        
            /*
             * The logic for the catalog is similar.
             */
            if (ref.getCatalog() != null
                    && (nameParts.length == 0 
                         || (nameParts.length == 1 
                              && ref.getCatalog().startsWith(nameParts[0])))) {
                
                completions.add(ref.getCatalog());
            }
            
            /*
             * Schema has three cases.
             *   1. The FROM has a dbo[.table] and the user has entered nothing
             *   2. The FROM has a dbo.table and the user has entered db<tab>
             *   3. The FROM has a catalog.dbo[.table] and the user 
             *      has entered catalog.db<tab>
             */
            if (ref.getSchema() != null) {
                
                if (nameParts.length == 0) {
                    
                    completions.add(ref.getSchema());
                }
                else if (nameParts.length == 1
                        && ref.getSchema().startsWith(nameParts[0])) {
                    
                    completions.add(ref.getSchema());
                }
                else if (nameParts.length == 2 
                        && ref.getCatalog() != null
                        && ref.getCatalog().equals(nameParts[0])
                        && ref.getSchema().startsWith(nameParts[1])) {
                    
                    completions.add(ref.getSchema());
                }
            }
            
            /*
             * Object name has four cases.
             *   1. The FROM clause has an object in it and the user hasn't
             *      entered anything.
             *   2. The FROM has a table and the user has entered ta<tab>
             *   3. The FROM has a dbo.table and the user has entered 
             *      dbo.ta<tab>
             *   4. The FROM has a catalog.dbo.table and the user has entered 
             *      catalog.dbo.ta<tab>
             */
            if (ref.getName() != null) {
                
                if (nameParts.length == 0) {
                    
                    completions.add(ref.getName());
                }
                else if (nameParts.length == 1
                        && ref.getName().startsWith(nameParts[0])) {
                    
                    completions.add(ref.getName());
                }
                else if (nameParts.length == 2 
                        && ref.getSchema() != null
                        && ref.getSchema().equals(nameParts[0])
                        && ref.getName().startsWith(nameParts[1])) {
                    
                    completions.add(ref.getName());
                }
                else if (nameParts.length == 3 
                        && ref.getCatalog() != null
                        && ref.getCatalog().equals(nameParts[0])
                        && ref.getSchema() != null
                        && ref.getSchema().equals(nameParts[1])
                        && ref.getName().startsWith(nameParts[2])) {
                    
                    completions.add(ref.getName());
                }
            }
        }
    }
    
    /**
     * This is similar to the getReferencedCompletions() except that it
     * only attempts to complete column names that belong to objects
     * that are referenced in the query.
     * 
     * @param completions The current set of completions. The implementor
     *   of this method is expected to add any completions it finds (if any)
     *   to this set before returning.
     * @param conn Connection with which to work.
     * @param nameParts This is an array of portions of the object 
     *   name that the user has entered thus far.
     * @param parseState This represents the information that was gleaned by
     *   parsing the current SQL statement.
     */
    protected void getColumnsOfReferences(Set<String> completions,
            Connection conn,  String []nameParts, SQLParseState parseState) {
        
        DatabaseObject []referencedObjects = parseState.getObjectReferences();
        
        /*
         * If there are no objects being referenced in the current SQL 
         * statement then there is no work to do.
         */
        if (referencedObjects == null
                || referencedObjects.length == 0) {
            
            return;
        }
        
        /*
         * Now, move through the referenced objects. We will then compare
         * those objects to the information that the user has provided to
         * determine possible completions.
         */
        for (DatabaseObject ref : referencedObjects) {
            
            /*
             * This one requires some help from the database.
             * In this case we will try to look up columns of referenced
             * tables. This leads to the following cases:
             *   1. <tab>
             *   2. col<tab>
             *   3. table.col<tab>
             *   4. alias.col<tab>
             *   5. schema.table.col<tab>
             *   6. catalog.schema.table.col<tab>
             */
            if (nameParts.length == 0) {
                
                getColumns(completions, conn, 
                    (ref.getCatalog() == null 
                        ? getCurrentCatalog(conn) : ref.getCatalog()),
                    ref.getSchema(), ref.getName(), "");
            }
            else if (nameParts.length == 1) {
                
                getColumns(completions, conn, 
                    (ref.getCatalog() == null 
                        ? getCurrentCatalog(conn) : ref.getCatalog()),
                    ref.getSchema(), ref.getName(), nameParts[0]);
            }
            else if (nameParts.length == 2
                    && ref.getName() != null
                    && ref.getName().equals(nameParts[0])) {
                
                getColumns(completions, conn, 
                    (ref.getCatalog() == null 
                        ? getCurrentCatalog(conn) : ref.getCatalog()),
                    ref.getSchema(), ref.getName(), nameParts[1]);
            }
            else if (nameParts.length == 2
                    && ref.getAlias() != null
                    && ref.getAlias().equals(nameParts[0])) {
                
                getColumns(completions, conn, 
                    (ref.getCatalog() == null 
                        ? getCurrentCatalog(conn) : ref.getCatalog()),
                    ref.getSchema(), ref.getName(), nameParts[1]);
            }
            else if (nameParts.length == 3
                    && ref.getSchema() != null
                    && ref.getSchema().equals(nameParts[0])
                    && ref.getName() != null
                    && ref.getName().equals(nameParts[1])) {
                
                getColumns(completions, conn, 
                    (ref.getCatalog() == null 
                        ? getCurrentCatalog(conn) : ref.getCatalog()),
                    ref.getSchema(), ref.getName(), nameParts[2]);
            }
            else if (nameParts.length == 4
                    && ref.getCatalog() != null
                    && ref.getCatalog().equals(nameParts[0])
                    && ref.getSchema() != null
                    && ref.getSchema().equals(nameParts[1])
                    && ref.getName() != null
                    && ref.getName().equals(nameParts[2])) {
                
                getColumns(completions, conn, 
                    ref.getCatalog(), ref.getSchema(), ref.getName(),
                    nameParts[3] + "%");
            }
        }
    }
    
    /**
     * Helper method to retrieve the set of catalogs that match 
     * a name provided.
     * 
     * @param completions The current set of object completions.
     * @param conn The connection to the database.
     * @param name A partially completed catalog name.
     */
    protected void getCatalogs(Set<String> completions,
            Connection conn, String name) {
        
        int count = 0;
        
        try {
            
            ResultSet results = conn.getMetaData().getCatalogs();
            while (results.next()) {
                
                String catalog = results.getString(1);
                if (name == null || catalog.startsWith(name)) {
                    
                    ++count;
                    completions.add(catalog);
                }
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Found " + count + " catalogs matching '" + name + "'");
        }
    }
    
    /**
     * Helper method to retrieve the set of catalogs that match 
     * a name provided.
     * 
     * @param completions The current set of object completions.
     * @param conn The connection to the database.
     * @param catalog The catalog containing the schema.
     * @param name A partially completed catalog name.
     */
    protected void getSchemas(Set<String> completions,
            Connection conn, String catalog, String name) {
        
        /*
         * Currently this method does nothing, but is here as a marker
         * for future work. As of this writing only the JDBC with 
         * Java 6 supports queries for schemas and since I don't
         * want to mandate java 6 yet, I'm leaving this as a stub.
         */
    }
    
    /**
     * Gathers the set of tables that matches requested criteria
     * @param completions The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param schema The schema (owner) to look for
     * @param tablePrefix The prefix of the table.
     */
    protected void getTables(Set<String> completions, Connection conn,
            String catalog, String schema, String tablePrefix) {
        
        int count = 0;
        
        try {
            
            ResultSet results = conn.getMetaData().getTables(
                (catalog == null ? "%" : catalog),
                (schema == null ? "%" : schema),
                (tablePrefix == null ? "%" : tablePrefix + "%"),
                null);
            
            while (results.next()) {
                
                completions.add(results.getString(3));
                ++count;
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Found " + count + " tables matching "
                + "catalog " + catalog
                + ", schema " + schema
                + ", table " + tablePrefix);
        }
    }
    
    /**
     * Gathers the set of columns that matches requested criteria
     * @param completions The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param schema The schema (owner) to look for
     * @param table The table name
     * @param table The column prefix
     */
    protected void getColumns(Set<String> completions, Connection conn,
            String catalog, String schema, String table, String columnPrefix) {
        
        int count = 0;
        
        try {
            
            ResultSet results = conn.getMetaData().getColumns(
                (catalog == null ? "%" : catalog),
                (schema == null ? "%" : schema),
                (table == null ? "%" : table),
                (columnPrefix == null ? "%" : columnPrefix + "%"));
            
            while (results.next()) {
                
                completions.add(results.getString(4));
                ++count;
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Found " + count + " columns matching "
                + "catalog " + catalog
                + ", schema " + schema
                + ", table " + table
                + ", column " + columnPrefix);
        }
    }
    
    /**
     * This method is used to look up available columns based upon tables
     * that are referenced within the current SQL statement.  That is,
     * if the user does:
     * <pre>
     *   select count(*) from master..sysobjects as o where o.i&lt;tab&gt;
     * </pre>
     * this logic will see the 'o.' as a reference to the alias to 
     * master..sysobjects and will attempt to look for all columns 
     * that start with 'i'.
     * 
     * @param completions The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param tableRefs The set of table names and aliases that are
     *   contained in the SQL
     * @param alias If non-null the alias name by which the table
     *   is being referred.
     * @param columnPrefix the portion of the column name entered so far.
     */
    protected void getColumnsOfAliases(Set<String> completions, Connection conn,
            String catalog,  DatabaseObject []tableRefs,
            String alias, String columnPrefix) {
        
        for (DatabaseObject ref : tableRefs) {
            
            if (alias == null
                    || (alias != null && alias.equals(ref.getAlias()))) {
                
                getColumns(completions, conn, 
                    (ref.getCatalog() == null ? catalog : ref.getCatalog()),
                    (ref.getSchema() == null ? "%" : ref.getSchema()),
                    ref.getName(),  columnPrefix);
            }
        }
    }
    
    /**
     * Returns set of available procedures.
     * 
     * @param completions The current set of completions.
     * @param conn The connection
     * @param catalog Catalog containing the procedure (or null)
     * @param schema The schema containing the procedure (or null)
     * @param procPrefix The prefix of the procedure.
     */
    protected void getProcedures(Set<String> completions, Connection conn,
            String catalog, String schema, String procPrefix) {
        
        int count = 0;
        
        try {
            
            ResultSet results = conn.getMetaData().getProcedures(
                    (catalog == null ? "%" : catalog),
                    (schema == null ? "%" : schema),
                    (procPrefix == null ? "%" : procPrefix + "%"));
            
            while (results.next()) {
                
                String proc = results.getString(3);
                completions.add(proc);
                ++count;
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Found " + count + " procedures matching "
                + "catalog " + catalog
                + ", schema " + schema
                + ", proc " + procPrefix);
        }
    }
    
    /**
     * Returns the set of parameters available for a given stored
     * procedure.
     * 
     * @param completions The current completion set.
     * @param proc The name of the procedure.
     * @param paramPart The part of the parameter name that the
     *   user has typed so far.
     */
    protected void getProcedureParameters(Set<String> completions,
            Connection conn, String catalog, String schema, String proc,
            String paramPart) {
        
        int count = 0;
        
        try {
            
            ResultSet results = conn.getMetaData().getProcedureColumns(
                    (catalog == null ? "%" : catalog),
                    (schema == null ? "%" : schema),
                    (proc == null ? "%" : proc),
                    (paramPart == null ? "%" : paramPart + "%"));
            
            while (results.next()) {
                
                String name = results.getString(4);
                
                /*
                 * Many drivers have a special column called RETURN_VALUE
                 * to indicate the datatype of the value returned by the
                 * procedure...I ignore these.
                 */
                if (!name.equals("RETURN_VALUE")) {
                    
                    completions.add(name);
                    ++count;
                }
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Found " + count + " parameters matching "
                + "catalog " + catalog
                + ", schema " + schema
                + ", proc " + proc
                + ", param " + paramPart);
        }
    }
    
    /**
     * Helper method to return the current catalog for a connection.
     * 
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
     * Helper method to return the parts of a name as a displayable
     * string.
     * 
     * @param nameParts parts of the name
     * @return displayable string.
     */
    protected String getNameString(String []nameParts) {
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nameParts.length; i++) {
            
            if (i > 0) {
                
                sb.append('.');
            }
            
            sb.append('[').append(nameParts[i]).append(']');
        }
        
        return sb.toString();
    }
}
