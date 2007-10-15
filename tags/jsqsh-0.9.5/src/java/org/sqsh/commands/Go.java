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
import org.kohsuke.args4j.Option;
import org.sqsh.BufferManager;
import org.sqsh.CannotSetValueError;
import org.sqsh.Command;
import org.sqsh.RendererManager;
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;


public class Go
    extends Command {
    
   private static class Options {
       
       @Option(name="-m",usage="Sets the display style for output")
           public String style = null;
       
       @Option(name="-h",usage="Toggles display of result header information")
           public boolean toggleHeaders = false;
       
       @Option(name="-f",usage="Toggles display of result footer information")
           public boolean toggleFooters = false;
        
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        RendererManager renderMan = session.getRendererManager();
        Options options = new Options();
        String origStyle = null;
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
        
        if (options.style != null) {
            
            origStyle = renderMan.getDefaultRenderer();
            
            try {
                
                session.getRendererManager().setDefaultRenderer(options.style);
            }
            catch (CannotSetValueError e) {
                
                session.err.println("Display style '" + options.style 
                    + "' is not a valid display style. See '\\help style' "
                    + "for list of available styles");
                return 1;
            }
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
            
            bufferMan.getCurrent().clear();
        }
        
        boolean origHeaders = renderMan.isShowFooters();
        boolean origFooters = renderMan.isShowFooters();
        
        if (options.toggleFooters) {
            
            renderMan.setShowFooters(!origFooters);
        }
        if (options.toggleHeaders) {
            
            renderMan.setShowHeaders(!origHeaders);
        }
        
        try {
            
            sqlRenderer.execute(session, sql);
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.err, e);
        }
        finally {
            
            if (origStyle != null) {
                
                renderMan.setDefaultRenderer(origStyle);
            }
            
            renderMan.setShowHeaders(origHeaders);
            renderMan.setShowFooters(origFooters);
        }
        
        return 0;
    }
}
