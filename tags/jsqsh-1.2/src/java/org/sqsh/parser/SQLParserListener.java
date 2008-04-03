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
package org.sqsh.parser;

/**
 * A SQLParserListener registers itself with the {@link SQLParser} to be 
 * notified when specific aspects of the SQL statements have been encountered.
 */
public interface SQLParserListener {
    
    /**
     * Called when the parser hits a clause in a DML statement.  A clause
     *   would be considered the SET, FROM, WHERE, GROUP, ORDER, etc.
     * 
     * @param parser The state of the parser immediately after the point
     *   at which the clause token was found.
     * @param clause The name of the clause.
     */
    void foundClause(SQLParser parser, String clause);
    
    /**
     * Called when the parser thinks it has the beginning of a statement.
     * Statements include SELECT, INSERT, UPDATE, DELETE, IF, WHILE, CREATE,
     * DROP, etc.
     * 
     * @param parser The state of the parser immediately after the point
     *   at which the clause token was found.
     * @param clause The name of the clause.
     */
    void foundStatement(SQLParser parser, String statement);
    
    /**
     * Called when a subquery is encountered in a query.
     * 
     * @param parser The current state of the parser.
     */
    void enteredSubquery(SQLParser parser);
    
    /**
     * Called when a subquery as been exited.
     * 
     * @param parser The current state of the parser.
     */
    void exitedSubquery(SQLParser parser);
    
    /**
     * Called when the parser encounters a table that is the target of a 
     * DML statement; that is, when the target of a SELECT, INSERT, UPDATE,
     * or DELETE is found.
     * 
     * @param parser The current state of the parser at the point the 
     *   table reference was found.
     * @param tableRef The table that was encountered.
     */
    void foundTableReference(SQLParser parser, DatabaseObject tableRef);
    
    /**
     * Called when the parser hits a stored procedure execution request
     * EXEC[UTE] [@return_code =] PROCEDURE 
     * 
     * @param parser The current state of the parser at the point the 
     *   procedure reference was found.
     * @param procRef The name of the procedure.
     */
    void foundProcedureExecution(SQLParser parser, DatabaseObject procRef);
}
