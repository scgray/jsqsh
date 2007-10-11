package org.sqsh.commands;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;

/**
 * Implements the \databases command.
 */
public class Databases
    extends Command {

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        SQLContext context = session.getSQLContext();
        if (context == null) {
            
            session.err.println("No database connection has been established."
                + " Use the \\connect command to create a connection.");
            return 1;
        }
        
        Connection con = session.getConnection();
        ResultSet result = null;
        try {
            
            DatabaseMetaData meta = con.getMetaData();
            
            result = meta.getCatalogs();
            
            SQLRenderer sqlRenderer = session.getSQLRenderer();
            Renderer renderer = session.getRendererManager()
                .getCommandRenderer(session);
            
            sqlRenderer.displayResults(renderer, session, result, null);
        }
        catch (SQLException e) {
            
            session.err.println("Failed to retrieve database metadata: "
                + e.getMessage());
            
            return 1;
        }
        finally {
            
            if (result != null) {
                
                try {
                    
                    result.close();
                }
                catch (SQLException e) {
                    
                    /* IGNORED */
                }
            }
            
        }
        
        return 0;
    }
}
