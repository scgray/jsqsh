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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.ConnectionContext;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the "\globals" command to display all global variables (or
 * anything else the driver says is "global").
 */
public class Globals
    extends Command {
    
    private static class Options
        extends SqshOptions {

        @Argv(program="\\globals", min=0, max=0)
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {

        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions options) throws Exception {
        
        ConnectionContext conn = session.getConnectionContext();
        
        if (conn == null) {
            
            session.err.println("No connection established");
            return 1;
        }
        
        List<String> vars = conn.getGlobals();
        Collections.sort(vars);
        
        ColumnDescription []columns = new ColumnDescription[1];
        columns[0] = new ColumnDescription("Global", -1);
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        renderer.header(columns);
        
        for (String var : vars) {
            
            String row[] = new String[1];
            
            row[0] = var;
            renderer.row(row);
        }
        
        renderer.flush();
        return 0;
    }
}
