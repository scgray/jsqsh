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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.Buffer;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SessionRedrawBufferMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

/**
 * Implements the \buf-load command.
 */
public class BufLoad
    extends Command {
    
    private static class Options
       extends SqshOptions {
        
        @Option(
            option='a', longOption="append", arg=NONE,
            description="Appends file contents to specified buffer")
        public boolean doAppend = false;
        
        @Argv(program="\\buf-load", min=1, max=2,
            usage="[-a] filename [dest-buf]")
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
        
        String filename = options.arguments.get(0);
        String destBuf = "!.";
        if (options.arguments.size() == 2) {
            
            destBuf = options.arguments.get(1);
        }
        
        Buffer buf = session.getBufferManager().getBuffer(destBuf);
        if (buf == null) {
            
            session.err.println("Specified destination buffer '" + destBuf
                + "' does not exist");
            return 1;
        }
        
        File file = new File(filename);
        if (file.exists() == false) {
            
            session.err.println("File '" + filename + "' does not exist");
            return 1;
        }
        
        if (options.doAppend == false) {
            
            buf.clear();
        }
        
        buf.load(file);
        
        if (buf == session.getBufferManager().getCurrent()) {
            
            throw new SessionRedrawBufferMessage();
        }
        
        return 0;
    }

}
