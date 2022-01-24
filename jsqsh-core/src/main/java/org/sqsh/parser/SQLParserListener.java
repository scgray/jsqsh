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
     * @param statement the statement type that was found
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
