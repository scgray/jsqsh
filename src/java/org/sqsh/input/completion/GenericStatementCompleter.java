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
package org.sqsh.input.completion;

import java.sql.Connection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NameParser;

import org.sqsh.parser.DatabaseObject;

/**
 * This completer is used to perform completions when specific SQL statements
 * are being edited. It is considered "generic" because instances of it 
 * can be configured with a "scope" of completions that are valid for it.
 */
public class GenericStatementCompleter
    extends SQLStatementCompleter {
    
    private static final Logger LOG = 
        Logger.getLogger("org.sqsh.completion.GenericStatementCompleter");
    
    /**
     * Flag to constructor to indicate that the completer should complete
     * catalog names.
     */
    public static final int CATALOGS = (1<<0);
    
    /**
     * Flag to constructor to indicate that the completer should complete
     * schema names.
     */
    public static final int SCHEMAS = (1<<1);
    
    /**
     * Flag to constructor to indicate that the completer should complete
     * table names.
     */
    public static final int TABLES = (1<<2);
    
    /**
     * Flag to constructor to indicate that the completer should complete
     * column names.
     */
    public static final int COLUMNS = (1<<3);
    
    /**
     * Flag to constructor to indicate that the completer should complete
     * procedure names.
     */
    public static final int PROCEDURES = (1<<4);
    
    /**
     * Set of flags that indicate what this should complete.
     */
    private int completionFlags = 0;
    
    /**
     * Creates a completer.
     * 
     * @param statement The name of the statement for which it will complete
     * @param clause The clause that it will complete for (null if no clause
     *   applies).
     * @param completionFlags The set of completions it should perform.
     */
    public GenericStatementCompleter(String statement, String clause,
            int completionFlags) {
        
        super(statement, clause);
        this.completionFlags = completionFlags;
    }
    

    @Override
    public void getCompletions (Set<String> completions, Connection conn,
            String[] nameParts, SQLParseState parseState) {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine(statement + ": Looking for "
                + getCompletionNames() + " for input "
                + getNameString(nameParts));
        }
        
        /*
         * Do catalog completion if requested. This only applies if the
         * user has not entered anything (completes with all catalogs) or
         * they have only entered one word so far.
         */
        if ((completionFlags & CATALOGS) != 0) {
            
            if (nameParts.length == 0) {
                
                getCatalogs(completions, conn, null);
            }
            else if (nameParts.length == 1) {
                
                getCatalogs(completions, conn, nameParts[0]);
            }
        }
        
        /*
         * Schemas are completed if we have no input from the user, or
         * the user has entered sch<tab> or catalog.sch<tab>.
         */
        if ((completionFlags & SCHEMAS) != 0) {
            
            if (nameParts.length == 0) {
                
                getSchemas(completions, conn, getCurrentCatalog(conn), null);
            }
            else if (nameParts.length == 1) {
                
                getSchemas(completions, conn, getCurrentCatalog(conn),
                    nameParts[0]);
            }
            else if (nameParts.length == 2) {
                
                getSchemas(completions, conn, nameParts[0], nameParts[1]);
            }
        }
        
        /*
         * Tables are completed if we have:
         *   1. No input
         *   2. ta<tab>
         *   3. dbo.ta<tab>
         *   4. catalog.dbo.tab<tab>
         * the user has entered
         */
        if ((completionFlags & TABLES) != 0) {
            
            if (nameParts.length == 0) {
                
                getTables(completions, conn, getCurrentCatalog(conn),
                    null, null);
            }
            else if (nameParts.length == 1) {
                
                getTables(completions, conn, getCurrentCatalog(conn),
                    null, nameParts[0]);
            }
            else if (nameParts.length == 2) {
                
                getTables(completions, conn, getCurrentCatalog(conn),
                    nameParts[0], nameParts[1]);
            }
            else if (nameParts.length == 3) {
                
                getTables(completions, conn, nameParts[0],
                    nameParts[1], nameParts[2]);
            }
        }
        
        /*
         * When completing column names, its a little easier, we only have
         * one case:
         * 
         * 1. The user has typed table.co<tab>
         * 2. The user has typed schema.table.co<tab>
         * 3. The user has typed catalog.schema.table.co<tab>
         */
        if ((completionFlags & COLUMNS) != 0) {
            
            if (nameParts.length == 2) {
                
                getColumns(completions, conn, getCurrentCatalog(conn),
                    null, nameParts[0], nameParts[1]);
            }
            else if (nameParts.length == 3) {
                
                getColumns(completions, conn, getCurrentCatalog(conn),
                    nameParts[0], nameParts[1], nameParts[2]);
            }
            else if (nameParts.length == 4) {
                
                getColumns(completions, conn, nameParts[0],
                    nameParts[1], nameParts[2], nameParts[3]);
            }
        }
        
        /*
         * Procedures can be:
         *   1. <tab>
         *   1. proc<tab>
         *   2. schema.proc<tab>
         *   3. catalog.schema.proc<tab>
         */
        if ((completionFlags & PROCEDURES) != 0) {
            
            if (nameParts.length == 0) {
                
                getProcedures(completions, conn,  
                    getCurrentCatalog(conn), null, null);
            }
            else if (nameParts.length == 1) {
                
                getProcedures(completions, conn,  
                    getCurrentCatalog(conn), null, nameParts[0]);
            }
            else if (nameParts.length == 2) {
                
                getProcedures(completions, conn,  
                    getCurrentCatalog(conn), nameParts[0], nameParts[1]);
            }
            else if (nameParts.length == 3) {
                
                getProcedures(completions, conn,  
                    nameParts[0], nameParts[1], nameParts[2]);
            }
        }
    }
    
    /**
     * Return completions as a string.
     */
    protected String getCompletionNames() {
        
        StringBuilder sb = new StringBuilder();
        if ((completionFlags & CATALOGS) != 0) {
            
            sb.append("CATALOGS");
        }
        if ((completionFlags & SCHEMAS) != 0) {
            
            sb.append(sb.length() > 0 ? ", " : "");
            sb.append("SCHEMAS");
        }
        if ((completionFlags & TABLES) != 0) {
            
            sb.append(sb.length() > 0 ? ", " : "");
            sb.append("TABLES");
        }
        if ((completionFlags & COLUMNS) != 0) {
            
            sb.append(sb.length() > 0 ? ", " : "");
            sb.append("COLUMNS");
        }
        if ((completionFlags & PROCEDURES) != 0) {
            
            sb.append(sb.length() > 0 ? ", " : "");
            sb.append("PROCEDURES");
        }
        
        return sb.toString();
    }
}
