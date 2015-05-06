/*
 * Copyright 2007-2012 Scott C. Gray
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
package org.sqsh.input.completion;

import java.sql.Connection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.parser.DatabaseObject;
import static org.sqsh.input.completion.CompletionCandidate.*;

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
        
        super(statement, completionFlags);
    }
    
    /**
     * Creates a completer.
     * 
     * @param statement The name of the statement for which it will complete
     */
    public QueryStatementCompleter (String statement) {
        
        super(statement, CATALOGS|SCHEMAS|TABLES|COLUMNS);
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
        if (refs.length > 0 && ! parseState.isEditingTableReference()) {
            
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
