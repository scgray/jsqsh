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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;

/**
 * Responsible for doing completion when the user hits the completion key.
 * This class only works if you are using the native readline.
 */
public class TabCompleter
    implements ReadlineCompleter {
    
    protected enum QuoteType { NONE, BRACKET, QUOTES };

    private SqshContext sqshContext;
    private Iterator<String> iter = null;
    private QuoteType  quote = QuoteType.NONE;
    
    
    public TabCompleter(SqshContext ctx) {
        
        this.sqshContext = ctx;
    }
    
    /**
     * Called when the user hits the completion key. See the javadoc 
     * for {@link org.gnu.readline.ReadlineCompleter} for how this
     * method is induced.
     * 
     * @param word The text up to the word break character.
     * @param state 0=initial search request, >1 the request for the N'th
     *    completion entry.
     */
    public String completer (String word, int state) {
        
        Connection conn = sqshContext.getCurrentSession().getConnection();
        if (word == null || conn == null) {
            
            iter = null;
            return null;
        }
        
        /*
         * A state greater than 0 indicates that the caller is in the
         * process of iterating over our current result set.
         */
        if (state > 0) {
            
            return next();
        }
        
        char ch = (word.length() == 0 ? 'x' : word.charAt(0));
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
         * The word that was found by our caller is not nearly enough
         * to figure out the whole object name, so we will use the
         * entire line to do our work.
         */
         String wholeLine = Readline.getLineBuffer();
         
        /*
         * Now, take the line and find the name of the object that the user
         * is currently sitting on. Object names are multi-part because
         * you can have "table" or "table.column" or "owner.table", etc.
         */
        String []parts = doFindObject(wholeLine);
        if (parts.length == 0) {
            
            return null;
        }
        
        /*
         * Now we have the hard part. We have a list of object name parts
         * that can be very ambiguous, so we may have to run a number of
         * queries to determine what the user meant.
         */
        TreeSet<String> set = new TreeSet<String>();
        
        /*
         * This may be needed a lot.
         */
        String catalog = getCurrentCatalog(conn);
        
        /*
        System.out.println();
        for (int i = 0; i < parts.length; i++) {
            
            System.out.println("#" + i + " = " + parts[i]);
        }
        */
        
        /*
         * A single name could be:
         *   1. Catalog name
         *   2. Table name
         *   3. Procedure name
         */
        if (parts.length == 1) {
            
            getCatalogs(set, conn, parts[0]);
            getTables(set, conn, catalog, "%", parts[0]);
            getProcedures(set, conn, catalog, "%", parts[0]);
        }
        else if (parts.length == 2) {
           
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
            getColumns(set, conn, catalog, null, parts[0], parts[1]);
            getTables(set, conn, catalog, parts[0], parts[1]);
            getProcedures(set, conn, catalog, parts[0], parts[1]);
        }
        else if (parts.length == 3) {
            
            /*
             * Three parts could be:
             * 
             *    1. catalog.schema.table
             *    2. catalog.schema.procedure
             *    3. schema.table.column
             */
            getTables(set, conn, parts[0], parts[1], parts[2]);
            getProcedures(set, conn, parts[0], parts[1], parts[2]);
            getColumns(set, conn, catalog, parts[0], parts[1], parts[2]);
        }
        else if (parts.length == 4) {
            
            /*
             * database.table.owner.column
             */
            getColumns(set, conn, parts[0], parts[1], parts[2], parts[3]);
        }
        
        if (set.size() == 0) {
            
            return null;
        }
        
        iter = set.iterator();
        return next();
    }
    
    /**
     * Returns the next word.
     * 
     * @return the next word null if there are no more.
     */
    private String next() {
        
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
     * Returns the current catalog for a connection.
     * @param conn  The connection
     * @return The current catalog or null if there is none.
     */
    private String getCurrentCatalog(Connection conn) {
        
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
    private void getCatalogs(Set<String> set, Connection conn, String name) {
        
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
    private void getTables(Set<String> set, Connection conn,
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
    private void getColumns(Set<String> set, Connection conn,
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
     * Gathers the set of tables that matches requested criteria
     * @param set The set that the table names will be added to.
     * @param conn The connection to use.
     * @param catalog The catalog to look in.
     * @param schema The schema (owner) to look for
     * @param tablePrefix The prefix of the table.
     */
    private void getProcedures(Set<String> set, Connection conn,
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
     * Processes the current line of input looking for the last object
     * name on the line (or at least what could possibly be an object
     * name). 
     * @param line The line to be processed.
     * @return The object name broken into its component parts. If there
     * was no object found an empty list will be returned.
     */
    private String[] doFindObject(String line) {
        
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
            if (Character.isLetter(ch) || ch == '_') {
                
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
}
        