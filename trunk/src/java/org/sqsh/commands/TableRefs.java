package org.sqsh.commands;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.SQLTools;
import org.sqsh.Session;

/**
 * Implements the \table-refs command.
 */
public class TableRefs
    extends Command {

    @Override
    public int execute (Session session, String[] argv) throws Exception {
        
        BufferManager bufferMan = session.getBufferManager();
        String sql = bufferMan.getCurrent().toString();
        
        SQLTools.TableReference []refs =
            SQLTools.getTableReferences(sql, false);
        
        if (refs.length == 0) {
            
            session.out.println("No table references identified");
            return 0;
        }
        
        session.out.println("Table references identified:");
        for (int i = 0; i < refs.length; i++) {
            
            if (refs[i].getAlias() != null) {
                
                session.out.println("   Table ["
                    + refs[i].getTable() + "] as ["
                    + refs[i].getAlias() + "]");
                
            }
            else {
                
                session.out.println("   Table ["
                    + refs[i].getTable() + "]");
            }
        }
        
        if (session.isInteractive()) {
            
            bufferMan.newBuffer();
        }
        else {
            
            bufferMan.getCurrent().clear();
        }
        
        return 0;
    }
}
