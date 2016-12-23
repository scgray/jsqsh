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
package org.sqsh.completion;

import java.sql.Connection;
import java.util.Set;

import org.sqsh.parser.DatabaseObject;

public class InsertStatementCompleter
    extends GenericStatementCompleter {
    
    /**
     * Creates a completer.
     * 
     * @param statement The name of the statement for which it will complete
     * @param clause The clause that it will complete for (null if no clause
     *   applies).
     * @param completionFlags The set of completions it should perform.
     */
    public InsertStatementCompleter () {
        
        super("INSERT", null, CATALOGS|SCHEMAS|TABLES|COLUMNS);
    }

    /* (non-Javadoc)
     * @see org.sqsh.completion.GenericStatementCompleter#getCompletions(java.util.Set, java.sql.Connection, java.lang.String[], org.sqsh.completion.SQLParseState)
     */
    @Override
    public void getCompletions (Set<String> completions, Connection conn,
            String[] nameParts, SQLParseState parseState) {
        
        DatabaseObject []refs = parseState.getObjectReferences();
        int startingCompletions = completions.size();
        
        /*
         * We've got a quandry here. If we have a reference, then we could
         * have one of the following states:
         *    INSERT xyz...
         * this could be
         *    INSERT xyz<tab>
         * or it could be:
         *    INSERT xyz (<tab>)
         * We cannot really tell which. The way we try to guess is to 
         * first try to fetch columns that might belong the "xyz". If get
         * them then we are done. If we don't then we try to expand the xyz.
         */
        if (refs.length > 0) {
            
            getColumnsOfReferences(completions, conn, nameParts, parseState);
            
            /*
             * If we found column references then we are done.
             */
            if (completions.size() > startingCompletions) {
                
                return;
            }
        }
        
        /*
         * Ok, we either have no references, or a potentially paritial one.
         */
        super.getCompletions(completions, conn, nameParts, parseState);
    }
}
