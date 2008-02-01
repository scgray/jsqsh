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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.sqsh.parser.AbstractParserListener;
import org.sqsh.parser.DatabaseObject;
import org.sqsh.parser.SQLParser;

/**
 * This class is used to represent the state of the current SQL statment 
 * being parsed during the completion process. It is intended to be passed
 * to a {@link SQLParser} and as the parser moves through the SQL, it 
 * accumulates information about the last SQL statement encountered in the
 * parsed block of SQL.
 */
public class SQLParseState
    extends AbstractParserListener {
    
    /**
     * I maintain table references for the queries in stack to allow
     * for subqueries. As we nest into a subquery a new list of 
     * references is added which is discarded when we leave the
     * context of the subquery.
     */
    private Stack<List<DatabaseObject>> refStack
        = new Stack<List<DatabaseObject>>();
    
    /*
     * A place to push the clause in whcih a subquery is located.
     */
    private Stack<String> clauseStack 
        = new Stack<String>();
    
    /**
     * The current statement type.
     */
    private String statement = null;
    
    /**
     * The name of the clause the user is currently sitting in.
     */
    private String clause = null;
    
    /**
     * Creates the object.
     */
    public SQLParseState() {
        
        refStack.push(new ArrayList<DatabaseObject>());
    }
    
    /**
     * Returns the name of the last SQL statement that was encountered. 
     * You can find the set of available statements by looking at the source
     * code for {@link SQLParser}.
     * @return The name of the most recent statement or NULL if a statement
     *   was not encountered. The returned name will always be in upper case.
     */
    public String getStatement() {
        
        return statement;
    }
    
    /**
     * Returns the last SQL statement clause (WHERE, ORDER BY, etc.) 
     * encountered while parsing the SQL. Not all statements will have clauses,
     * in which case null will be returned.
     * 
     * @return The most recent encountered clause (in upper case) or null
     *   if no clause was found or is applicable for the current statement.
     */
    public String getCurrentClause() {
        
        return clause;
    }
    
    /**
     * Returns the set of database objects that are referenced by the
     * current statement (as returned by {@link #getStatement()}).
     * 
     * @return The set of database objects referenced by the current
     *   statement or null if none are referenced.
     */
    public DatabaseObject[] getObjectReferences() {
        
        if (refStack.size() == 1) {
            
            return refStack.peek().toArray(new DatabaseObject[0]);
        }
        else {
            
            ArrayList<DatabaseObject> refs = 
                new ArrayList<DatabaseObject>();
            for (int i = 0; i < refStack.size(); i++) {
                
                refs.addAll(refStack.get(i));
            }
            
            return refs.toArray(new DatabaseObject[0]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void enteredSubquery (SQLParser parser) {

        /*
         * As we enter the sub query we push a new "context" onto 
         * our stack.
         */
        refStack.push(new ArrayList<DatabaseObject>());
        clauseStack.push(clause);
    }

    /** {@inheritDoc} */
    @Override
    public void exitedSubquery (SQLParser parser) {

        /*
         * And it gets discarded as we leave the subquery.
         */
        refStack.pop();
        clause = clauseStack.pop();
    }

    /** {@inheritDoc} */
    @Override
    public void foundClause (SQLParser parser, String clause) {

        this.clause = clause;
    }

    /** {@inheritDoc} */
    @Override
    public void foundStatement (SQLParser parser, String statement) {
        
        this.statement = statement;
        clause = null;

        /*
         * Each time we hit a new statement we discard any context
         * of the previous statement we encountered.
         */
        while (refStack.size() > 1) {
            
            refStack.pop();
        }
        
        /*
         * There is always a root element in the stack, so we will
         * just clear it out to save object creations.
         */
        if (refStack.size() == 1) {
            
            refStack.peek().clear();
        }
        else {
            
            refStack.push(new ArrayList<DatabaseObject>());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void foundTableReference (SQLParser parser,
            DatabaseObject tableRef) {

        /*
         * Add table references to the current "stack".
         */
        refStack.peek().add(tableRef);
    }

    /** {@inheritDoc} */
    @Override
    public void foundProcedureExecution (SQLParser parser,
            DatabaseObject procRef) {

        refStack.peek().add(procRef);
    }
}
