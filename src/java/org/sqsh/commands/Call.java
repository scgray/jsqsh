package org.sqsh.commands;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

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
                
                if (!setParameter(session, statement, i+1, argv[i])) {
                    
                    return 1;
                }
            }
            
            sqlRenderer.execute(session, statement);
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.err, e);
            return 1;
        }
        
        return 0;
    }
    
    private boolean setParameter(Session session,
            CallableStatement statement, int idx, String value)
        throws SQLException {
        
        char type = 'S';
        
        if (value.length() > 2
                && value.charAt(1) == ':') {
            
            type = Character.toUpperCase(value.charAt(0));
            value = value.substring(2);
        }
        
        try {
            
            switch (type) {
                
                case 'S':
                case 'C':
                    statement.setString(idx, value);
                    break;
                
                case 'Z':
                    statement.setBoolean(idx, Boolean.valueOf(value));
                    break;
                    
                case 'D':
                    statement.setDouble(idx, Double.valueOf(value));
                    break;
                    
                case 'F':
                    statement.setFloat(idx, Float.valueOf(value));
                    break;
                    
                case 'I':
                    statement.setInt(idx, Integer.valueOf(value));
                    break;
                    
                case 'J':
                    statement.setLong(idx, Long.valueOf(value));
                    break;
                    
                default:
                    session.err.println("Invalid type specifier '"
                        + type + "'. Valid specifiers are SCZDFIJ");
                    return false;
            }
        }
        catch (NumberFormatException e) {
            
            session.err.println("Invalid number format '"
                + value + "' provided for type '" + type + "'");
            return false;
        }
        
        return true;
    }
}
