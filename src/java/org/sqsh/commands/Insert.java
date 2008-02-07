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

import java.sql.Connection;
import java.sql.SQLException;

import org.kohsuke.args4j.Option;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.RendererManager;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.renderers.InsertRenderer;


public class Insert 
    extends Command
    implements DatabaseCommand {
    
    private static class Options
        extends SqshOptions {
        
        @Option(name="-s",usage="Session used to execute inserts")
            public int sessionId = -1;
        @Option(name="-b",usage="Batch size for inserts")
            public int batchSize = 50;
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }
    
    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        RendererManager renderMan = session.getRendererManager();
        Options options = (Options) opts;
        
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
