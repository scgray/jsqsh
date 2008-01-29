package org.sqsh.commands;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.RendererManager;
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.renderers.InsertRenderer;


public class Insert 
    extends Command {
    
    private static class Options {
        
        @Option(name="-s",usage="Session used to execute inserts")
            public int sessionId = -1;
        @Option(name="-b",usage="Batch size for inserts")
            public int batchSize = 50;
         
         @Argument
             public List<String> arguments = new ArrayList<String>();
     }
    
    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        RendererManager renderMan = session.getRendererManager();
        Options options = new Options();
        int rc = parseOptions(session, argv, options);
        if (rc != 0) {
            
            return rc;
        } 
        
        SQLContext context = session.getSQLContext();
        if (context == null) {
            
            session.err.println("You are not currently connect to a server. "
                + "Use the \\connect command to create a new connection.");
            return 1;
        }
        
        /*
         * Make sure the user provided at least a table name.
         */
        if (options.arguments.size() != 1) {
            
            session.err.println("use: \\insert [options] table_name");
            return 1;
        }
        
        String table = options.arguments.get(0);
        Connection targetConnection = null;
        
        /*
         * If a session Id was provided, then try to get its connection.
         */
        if (options.sessionId > 0) {
            
            Session targetSession = session.getContext().getSession(
                options.sessionId);
            if (targetSession == null) {
                
                session.err.println("The provided session id '" 
                    + options.sessionId + "' is not valid. Use \\session "
                    + "to view valid session ids");
                return 1;
            }
            
            targetConnection = targetSession.getConnection();
            if (targetConnection == null) {
                
                session.err.println("Session " + options.sessionId + 
                    " is not currently connected to a server.");
                return 1;
            }
        }
        
        
        /*
         * Set up the insert renderer based upon the provided input
         * parameters.
         */
        InsertRenderer renderer = (InsertRenderer) 
            renderMan.getRenderer(session, "insert");
        
        renderer.setTable(table);
        renderer.setBatchSize(options.batchSize);
        renderer.setConnection(targetConnection);
        
        /*
         * Get the current SQL statement.
         */
        BufferManager bufferMan = session.getBufferManager();
        SQLRenderer sqlRenderer = session.getSQLRenderer();
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
            
            sqlRenderer.execute(renderer, session, sql);
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.err, e);
        }
        
        return 0;
    }

}
