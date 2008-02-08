package org.sqsh.commands;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;

/**
 * Implements the \call command.
 */
public class Call
    extends Command
    implements DatabaseCommand {

    @Override
    public int execute (Session session, SqshOptions options)
        throws Exception {
        
        String argv[] = options.arguments.toArray(new String[0]);
        
        SQLRenderer sqlRenderer = session.getSQLRenderer();
        BufferManager bufferMan = session.getBufferManager();
        Connection conn = session.getConnection();
        String sql = bufferMan.getCurrent().toString();
        
        /*
         * If this is an interactive session, then we create a new
         * buffer to work with, otherwise we just re-use the current
         * buffer.
         */
        if (session.isInteractive()) {
            
            bufferMan.newBuffer();
        }
        else {
            
            bufferMan.getCurrent().clear();
        }
        
        try {
            
            CallableStatement statement = conn.prepareCall(sql);
            for (int i = 0; i < argv.length; i++) {
                
                statement.setString(i+1, argv[i]);
            }
            
            sqlRenderer.execute(session, statement);
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.err, e);
            return 1;
        }
        
        return 0;
    }

}
