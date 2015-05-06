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
package org.sqsh.parser;

import static org.sqsh.input.completion.CompletionCandidate.*;

/**
 * Implementation of SQLParserListener that provides NO-OP's for all of 
 * the methods dictated by the interface. 
 */
public class AbstractParserListener
    implements SQLParserListener {
    
    protected boolean isEditingTableReference;
    
    /** {@inheritDoc} */
    @Override
    public void reset() {
        
        isEditingTableReference = false;
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isEditingTableReference() {
        
        return isEditingTableReference;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setEditingTableReference(boolean tf) {
        
        this.isEditingTableReference = tf;
    }

    /** {@inheritDoc} */
    @Override
    public void foundClause (SQLParser parser, String clause) {

        /* IGNORED */
    }

    /** {@inheritDoc} */
    @Override
    public void foundStatement (SQLParser parser, String statement) {

        /* IGNORED */
    }
    
    /** {@inheritDoc} */
    @Override
    public void enteredSubquery(SQLParser parser) {
        
        /* IGNORED */
    }
    
    /** {@inheritDoc} */
    @Override
    public void exitedSubquery(SQLParser parser) {
        
        /* IGNORED */
    }

    /** {@inheritDoc} */
    @Override
    public void foundTableReference (SQLParser parser, DatabaseObject tableRef) {

        /* IGNORED */
    }
    
    /** {@inheritDoc} */
    @Override
    public void foundProcedureExecution (SQLParser parser,
            DatabaseObject procRef) {

        /* IGNORED */
    }
}
