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

import static org.sqsh.options.ArgumentRequired.REQUIRED;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

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

        @Option(
            option='n', longOption="repeat", arg=REQUIRED, argName="count",
            description="Repeates the query execution count times")
        public int repeat = 1;
        
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
        
        SqshContext context = session.getContext();

        for (int i = 0; i < options.repeat; i++) {

            /*
             * Just like the \go command we set a variable that the
             * SQL in the session can refer to to know what iteration
             * it is in.
             */
            if (options.repeat > 1) {

                session.setVariable("iteration", Integer.toString(i));
            }
        
            /*
             * Pay close attention, kiddies...first, we open our input file.
             */
            InputStream in = new FileInputStream(filename);
            
            /*
             * Next, make the input of our session, this file. Note that we
             * mark it as auto-close (the "true"). This will cause the session
             * to close this descriptor after the command is finished executing
             * and we mark it as non-interactive (the "false").
             */
            session.setIn(in, true, false);
            
            /*
             * Now we will recurse back into the context and ask it to execute
             * the contents of this session. This will return when the EOF
             * is reached on the input stream.   Now, this sessin's input
             * stream will still be set to be the file we opened, but we
             * don't really need to worry about that as the default behavior
             * of the session is to restore all input/output streams to the
             * original state after the execution of every command.
             */
            context.run(session);
        }
        
        return 0;
    }
}
