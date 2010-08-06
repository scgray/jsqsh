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
import java.util.Arrays;
import java.util.List;

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.SQLDriver;
import org.sqsh.SQLDriverManager;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the \drivers command.
 */
public class Drivers
    extends Command {
    
    private static class Options
        extends SqshOptions {
    
        @Argv(program="\\drivers", min=0, max=0)
        public List<String> arguments = new ArrayList<String>();
    }
    
    /**
     * Return our overridden options.
     */
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        SQLDriverManager driverMan = session.getDriverManager();
        SQLDriver []drivers = driverMan.getDrivers();
        
        Arrays.sort(drivers);
        
        ColumnDescription []columns = new ColumnDescription[4];
        columns[0] = new ColumnDescription("Target", -1);
        columns[1] = new ColumnDescription("Name", -1);
        columns[2] = new ColumnDescription("URL", -1);
        columns[3] = new ColumnDescription("Class", -1);
        
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        renderer.header(columns);
        
        for (int i = 0; i < drivers.length; i++) {
            
            String target = drivers[i].getTarget();
            if (drivers[i].isAvailable()) {
                
                target = "* " + target;
            }
            else {
                
                target = "  " + target;
            }
            
            String []row = new String[4];
            row[0] = target;
            row[1] = drivers[i].getName();
            row[2] = drivers[i].getUrl();
            row[3] = drivers[i].getDriverClass();
            
            renderer.row(row);
        }
        
        renderer.flush();
        
        return 0;
    }
}
