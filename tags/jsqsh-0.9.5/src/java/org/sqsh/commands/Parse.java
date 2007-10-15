package org.sqsh.commands;

import java.io.PrintStream;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.parser.SQLParser;
import org.sqsh.parser.SQLParserListener;
import org.sqsh.parser.TableReference;

/**
 * Implements the \parse command.
 */
public class Parse
    extends Command {

    @Override
    public int execute (Session session, String[] argv) throws Exception {
        
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
         * @see org.sqsh.parser.SQLParserListener#foundTableReference(org.sqsh.parser.SQLParser, org.sqsh.parser.TableReference)
         */
        public void foundTableReference (SQLParser parser,
                TableReference tableRef) {
            
            StringBuilder sb = new StringBuilder();
            if (tableRef.getDatabase() != null) {
                
                sb.append('[')
                    .append(tableRef.getDatabase())
                    .append(']')
                    .append('.');
            }
            if (tableRef.getOwner() != null) {
                
                sb.append('[')
                    .append(tableRef.getOwner())
                    .append(']')
                    .append('.');
            }
            sb.append('[')
                .append(tableRef.getTable())
                .append(']');
            
            if (tableRef.getAlias() != null) {
                
                sb.append(" [as ");
                sb.append(tableRef.getAlias());
                sb.append("]");
            }
            
            out.println("   TABLE-REF: " + sb.toString());
        }
    }
}
