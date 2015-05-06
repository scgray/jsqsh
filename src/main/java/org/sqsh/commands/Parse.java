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
package org.sqsh.commands;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.SQLConnectionContext;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.parser.AbstractParserListener;
import org.sqsh.parser.SQLParser;
import org.sqsh.parser.DatabaseObject;

/**
 * Implements the \parse command.
 */
public class Parse
    extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Argv(program="\\parse", min=0, max=0)
        public List<String> arguments = new ArrayList<String>();
    }
    
    /**
     * Return our overridden options.
     */
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        BufferManager bufferMan = session.getBufferManager();
        String sql = bufferMan.getCurrent().toString();
        SQLParser parser = new SQLParser(
                ((SQLConnectionContext) session.getConnectionContext()).getIdentifierNormalizer(),
                new ParserListener(session.out));
        
        parser.parse(sql);
        
        if (session.isInteractive()) {
            
            bufferMan.newBuffer();
        }
        else {
            
            bufferMan.getCurrent().clear();
        }
        
        return 0;
    }
    
    private static class ParserListener
        extends AbstractParserListener {
        
        private PrintStream out;
        
        public ParserListener (PrintStream out) {
            
            this.out = out;
        }

        /**
         * @see org.sqsh.parser.SQLParserListener#enteredSubquery(org.sqsh.parser.SQLParser)
         */
        public void enteredSubquery (SQLParser parser) {

            out.println("====> SUBQUERY-BEGIN");
        }
        
        /**
         * @see org.sqsh.parser.SQLParserListener#exitedSubquery(org.sqsh.parser.SQLParser)
         */
        public void exitedSubquery (SQLParser parser) {

            out.println("<==== SUBQUERY-END");
        }

        /**
         * @see org.sqsh.parser.SQLParserListener#foundClause(org.sqsh.parser.SQLParser, java.lang.String)
         */
        public void foundClause (SQLParser parser, String clause) {

            out.println("   CLAUSE: " + clause);
        }

        /**
         * @see org.sqsh.parser.SQLParserListener#foundStatement(org.sqsh.parser.SQLParser, java.lang.String)
         */
        public void foundStatement (SQLParser parser, String statement) {

            out.println("STATEMENT: " + statement);
        }

        /**
         * @see org.sqsh.parser.SQLParserListener#foundTableReference(org.sqsh.parser.SQLParser, org.sqsh.parser.DatabaseObject)
         */
        public void foundTableReference (SQLParser parser,
                DatabaseObject tableRef) {
            
            out.println("   TABLE-REF: " + tableRef.toString());
        }
        
        /**
         * @see org.sqsh.parser.SQLParserListener#foundTableReference(org.sqsh.parser.SQLParser, org.sqsh.parser.DatabaseObject)
         */
        public void foundProcedureExecution (SQLParser parser,
                DatabaseObject procRef) {
            
            out.println("   PROC-EXEC: " + procRef.toString());
        }
    }
}
