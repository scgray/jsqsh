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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the debug command.
 */
public class Debug
    extends Command {
    
    private static class Options
        extends SqshOptions {
    
        @Argv(program="\\debug", min=1, max=1)
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
        
        Options options = (Options)opts;
        boolean ok = true;

        for (int i = 0; i < options.arguments.size(); i++) {
            
            String name = options.arguments.get(i);
            Logger log = Logger.getLogger(name);
            
            if (log != null) {
                
                log.setLevel(Level.FINE);
            }
            else {
                
                session.err.println("Unable to find logger '"
                    + name + "'");
                ok = false;
            }
        }
        
        return (ok ? 0 : 1);
    }
}
