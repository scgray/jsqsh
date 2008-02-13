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

import static org.sqsh.options.ArgumentRequired.NONE;

import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

/**
 * Implements the 'echo' command in sqsh.
 */
public class Echo
    extends Command {
    
     /*
      * Used to contain the command line options that were passed in by
      * the caller.
      */
    private static class Options
       extends SqshOptions {
        
       @Option(
            option='n', longOption="no-newline", arg=NONE,
            description="Do not append a trailing new-line")
       public boolean noNewline = false;
       
       @Argv(program="\\echo", usage="[text ...]")
       public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }
    
    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        for (int i = 0; i < options.arguments.size(); i++) {
            
            if (i > 0) {
                
                session.out.print(' ');
            }
            
            session.out.print(options.arguments.get(i));
        }
        
        if (options.noNewline == false) {
            
            session.out.println();
        }
        
        return 0;
    }
}
