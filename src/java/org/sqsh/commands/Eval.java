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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SessionEvaluateMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the \eval command.
 */
public class Eval
    extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Argv(program="\\eval", min=1, max=1, usage="filename")
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
        
        if (options.arguments.size() != 1) {
            
            session.err.println("use: \\eval filename");
            return 1;
        }
        
        File filename = new File(options.arguments.get(0));
        if (!filename.canRead()) {
            
            session.err.println("Cannot open '" + filename.toString()
                + "' for read'");
            return 1;
        }
        
        InputStream in =  new FileInputStream(filename);
        
        /*
         * This exception is a "message" to the session that we wish it
         * to temporarily take its input from the provided input stream.
         */
        throw new SessionEvaluateMessage(in, true, false);
    }

}
