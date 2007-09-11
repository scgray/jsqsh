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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;


public class Go
    extends Command {
    
   private static class Options {
        
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
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
            
            bufferMan.clear();
        }
        
        try {
            
            sqlRenderer.execute(session, sql);
        }
        catch (SQLException e) {
            
            printException(session.err, e);
        }
        
        return 0;
    }
}
