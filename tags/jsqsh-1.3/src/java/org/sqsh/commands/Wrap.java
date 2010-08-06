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

import org.sqsh.Buffer;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.WordWrapLineIterator;
import org.sqsh.options.Argv;

/**
 * Command used to test the word-wrapping process.
 */
public class Wrap
    extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Argv(program="\\wrap", min=0, max=1, usage="[width]")
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
        
        int width = 40;
        if (options.arguments.size() > 0) {
            
            width = Integer.parseInt(options.arguments.get(0));
        }
        
        BufferManager bufMan = session.getBufferManager();
        
        /*
         * Fetch the current buffer and create a new one.
         */
        Buffer buf = bufMan.getCurrent();
        bufMan.newBuffer();
        
        WordWrapLineIterator iter = new WordWrapLineIterator(buf.toString(), width);
        while (iter.hasNext()) {
            
            String str = (String) iter.next();
            session.out.print("|");
            session.out.print(str);
            for (int i = str.length(); i < width; i++) {
                
                session.out.print(' ');
            }
            
            session.out.println("|");
        }
        
        return 0;
    }

}
