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
 * Implementation of SQLParserListener that provides NO-OP's for all of 
 * the methods dictated by the interface. 
 */
public class AbstractParserListener
    implements SQLParserListener {

    /** {@inheritDoc} */
    public void foundClause (SQLParser parser, String clause) {

        /* IGNORED */
    }

    /** {@inheritDoc} */
    public void foundStatement (SQLParser parser, String statement) {

        /* IGNORED */
    }
    
    /** {@inheritDoc} */
    public void enteredSubquery(SQLParser parser) {
        
        /* IGNORED */
    }
    
    /** {@inheritDoc} */
    public void exitedSubquery(SQLParser parser) {
        
        /* IGNORED */
    }

    /** {@inheritDoc} */
    public void foundTableReference (SQLParser parser, DatabaseObject tableRef) {

        /* IGNORED */
    }
    
    public void foundProcedureExecution (SQLParser parser,
            DatabaseObject procRef) {

        /* IGNORED */
    }
}
