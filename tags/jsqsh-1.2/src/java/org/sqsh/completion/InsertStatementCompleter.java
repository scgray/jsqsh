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
