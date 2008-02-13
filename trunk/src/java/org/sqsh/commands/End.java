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

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the \end command to end the current session and switch to
 * another session.
 */
public class End
    extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Argv(program="\\end", min=0, max=1, usage="[next-session]")
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
        
        Options options = (Options) opts;
        
        int targetSession = -1;
        Session target = null;
        
        if (options.arguments.size() > 1) {
            
            session.err.println("Use: \\end [next-session]");
            return 1;
        }
        
        if (options.arguments.size() == 1) {
            
            try {
                
                targetSession = Integer.parseInt(options.arguments.get(0));
            }
            catch (NumberFormatException e) {
                
                session.err.println("Invalid session id '" 
                    + options.arguments.get(0) + "'");
                return 1;
            }
            
            target = session.getContext().getSession(targetSession);
            if (target == null) {
                
                session.err.println("Specified next session id '"
                    + options.arguments.get(0) + "'" + " does not exist");
                return 1;
            }
        }
        
        throw new SqshContextSwitchMessage(session, target, true);
    }
}
