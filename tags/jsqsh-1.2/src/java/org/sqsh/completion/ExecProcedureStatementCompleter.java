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
 * For an EXE[CUTE] PROC[EDURE] statement, it will attempt to complete
 * the name of the procedure that will be executed and, following that,
 * to complete any parameters required for the procedure.
 */
public class ExecProcedureStatementCompleter
    extends SQLStatementCompleter {
    
    private static final Logger LOG = 
        Logger.getLogger("org.sqsh.completion.ExecProcedureCompleter");
    
    /**
     * Creates a completer.
     * 
     * @param statement The name of the statement for which it will complete
     * @param clause The clause that it will complete for (null if no clause
     *   applies).
     * @param completionFlags The set of completions it should perform.
     */
    public ExecProcedureStatementCompleter () {
        
        super("EXECUTE", null);
    }

    @Override
    public void getCompletions (Set<String> completions, Connection conn,
            String[] nameParts, SQLParseState parseState) {
        
        DatabaseObject []refs = parseState.getObjectReferences();
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("EXECUTE PROC: Completing for "
                + getNameString(nameParts));
        }
        
        /*
         * If there are references, then we probably want to complete 
         * the parameter that the user is typing...
         */
        if (refs.length > 0) {
            
            /*
         	 * ...but, we have a small issue. We can't tell the 
         	 * different between:
         	 *    INSERT proc<tab>
         	 * and:
         	 *    EXEC proc (<tab>)
         	 * To tell the difference we check to see if the name the user is
         	 * typing matches the name of the proc that is being referenced.
         	 */
            if (nameParts.length > 0) {
                
                DatabaseObject currentObject = new DatabaseObject(nameParts);
                for (DatabaseObject referencedObject : refs) {
                    
                    if (referencedObject.equals(currentObject)) {
                        
                        if (LOG.isLoggable(Level.FINE)) {
    
                            LOG.fine("Attempting to complete procedure user is "
                                + "currently editing ("
                                + currentObject.toString() + ")");
                        }
                        
                        /*
                         * Ok, so the object being referenced following the
                         * EXECUTE call is the same name as what the cursor
                         * is sitting on right now. This means we want to
                         * complete procedure calls.
                         */
                        getProcedures(completions, conn, 
                            (currentObject.getCatalog() == null
                                    ? getCurrentCatalog(conn)
                                    : currentObject.getCatalog()),
                            currentObject.getSchema(),
                            currentObject.getName());
                        
                        return;
                    }
                }
            }
            
            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("Attempting to complete parameters for "
                    + refs[0].toString() + " using user input "
                    + getNameString(nameParts));
            }
            
            /*
             * If we got here, then we are not editing a referenced object,
             * so we want to complete procedure parameters instead.
             */
            getProcedureParameters(completions, conn, 
                (refs[0].getCatalog() == null ? getCurrentCatalog(conn)
                        : refs[0].getCatalog()),
                refs[0].getSchema(), refs[0].getName(),
                (nameParts.length == 0 ? null : nameParts[0]));
            
            return;
        }
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Looking for procedures matching user input: " 
                + getNameString(nameParts));
        }
        
        /*
         * If we got here, have no object references, so we want to supply
         * the user a list of procedures.
         */
        getProcedures(completions, conn,
            getCurrentCatalog(conn), null, null);
    }
}
