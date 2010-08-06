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
package org.sqsh.commands;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.parser.SQLParser;
import org.sqsh.parser.SQLParserListener;
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
        SQLParser parser = new SQLParser(new ParserListener(session.out));
        
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
        implements SQLParserListener {
        
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
