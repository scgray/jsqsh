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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.parser.DatabaseObject;

/**
 * This completer is used to complete object names in SQL statements
 * that explicitly reference tables via a FROM clause. This includes
 * SELECT, UPDATE, DELETE, etc. It allows for the scope of the completion
 * to be narrowed by the tables contained in the FROM clause.
 */
public class QueryStatementCompleter
    extends GenericStatementCompleter {
    
    private static final Logger LOG = 
        Logger.getLogger("org.sqsh.completion.QueryStatementCompleter");
    
    /**
     * Creates a completer.
     * 
     * @param statement The name of the statement for which it will complete
     * @param clause The clause that it will complete for (null if no clause
     *   applies).
     * @param completionFlags The set of completions it should perform.
     */
    public QueryStatementCompleter (String statement, String clause,
            int completionFlags) {
        
        super(statement, clause,  completionFlags);
    }
    
    /**
     * Creates a completer.
     * 
     * @param statement The name of the statement for which it will complete
     * @param clause The clause that it will complete for (null if no clause
     *   applies).
     */
    public QueryStatementCompleter (String statement, String clause) {
        
        super(statement, clause,  CATALOGS|SCHEMAS|TABLES|COLUMNS);
    }
    
    /**
     * Creates a completer.
     * 
     * @param statement The name of the statement for which it will complete
     */
    public QueryStatementCompleter (String statement) {
        
        super(statement, null, CATALOGS|SCHEMAS|TABLES|COLUMNS);
    }

    /* (non-Javadoc)
     * @see org.sqsh.completion.GenericStatementCompleter#getCompletions(java.util.Set, java.sql.Connection, java.lang.String[], org.sqsh.completion.SQLParseState)
     */
    @Override
    public void getCompletions (Set<String> completions, Connection conn,
            String[] nameParts, SQLParseState parseState) {
        
        DatabaseObject []refs = parseState.getObjectReferences();
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine(statement + ": Looking for "
                + getCompletionNames() + " for input "
                + getNameString(nameParts));
        }
        
        /*
         * If the query is referencing an object name, then we (probably)
         * want to complete using only the objects referenced in the query
         * or columns of those objects. This rule does not apply if the user
         * is editing the FROM clause right now.
         */
        if (refs.length > 0
                && (parseState.getCurrentClause() == null
                   || parseState.getCurrentClause().equals("FROM") == false)) {
            
            /*
             * ...however, we have a small issue. The user *could* be
             * editing one of those referenced objects right now, like so:
             *    FROM dbo.sys<tab>
             * According to our parser "dbo.sys" is a referenced table.
             * I attempt to detect this scenario by comparing the name the
             * user is currently editing to the objects being referenced.
             */
            boolean isEditingReference = false;
            if (nameParts.length > 0) {
                
                DatabaseObject currentObject = new DatabaseObject(nameParts);
                for (DatabaseObject referencedObject : refs) {
                    
                    if (referencedObject.equals(currentObject)) {
                        
                        if (LOG.isLoggable(Level.FINE)) {
    
                            LOG.fine("User is editing a referenced object");
                            isEditingReference = true;
                        }
                        
                        break;
                    }
                }
            }
            
            /*
             * If we have referenced objects and the user is not editing
             * one of them, then only complete using the objects that
             * the user is referencing.
             */
            if (!isEditingReference) {
                
                if (LOG.isLoggable(Level.FINE)) {

                    LOG.fine("Completing " + getNameString(nameParts)
                        + " using only referenced object names");
                        
                }
                
                getReferencedCompletions(completions, nameParts, parseState);
                getColumnsOfReferences(completions, conn, nameParts, parseState);
                return;
            }
        }
        
        /*
         * At this point we have no referenced objects, or we are in the
         * FROM clause or the user is editing the name of a referenced
         * object, so we want to go complete based upon all available
         * object names.
         */
        super.getCompletions(completions, conn, nameParts, parseState);
    }
}
