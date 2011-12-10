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

import org.sqsh.Buffer;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.TruncatingLineIterator;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

/**
 * Implements the \history command.
 */
public class History
    extends Command {
    
    private static class Options
        extends SqshOptions {
        
        @Option(
            option='a', longOption="all", arg=NONE,
            description="Show the entire text of all statements")
            public boolean showAll = false;
        
        @Argv(program="\\history", min=0, max=0, usage="[-a]")
        public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }
    
    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options)opts;
        String lineSep = System.getProperty("line.separator");
        BufferManager bufMan = session.getBufferManager();
        
        /*
         * Note that we skip buffer 0 because it is the current
         * buffer.
         */
        Buffer []buffers = bufMan.getBuffers();
        for (int i = 0; i < (buffers.length - 1); i++) {
            
            Buffer buf = buffers[i];
            StringBuilder sb = new StringBuilder();
            sb.append('(').append(buf.getId()).append(") ");
            
            int indent = sb.length();
            int line = 0;
            
            TruncatingLineIterator iter = new TruncatingLineIterator(
                buf.toString(), -1);
            while (iter.hasNext()) {
                
                ++line;
                if (line > 1) {
                    
                    for (int j = 0; j < indent; j++) {
                        
                        sb.append(' ');
                    }
                }
                
                if (options.showAll == false && line > 10) {
                    
                    sb.append("...");
                    sb.append(lineSep);
                    break;
                }
                else {
                
                    sb.append(iter.next());
                    sb.append(lineSep);
                }
            }
            
            if (line == 0)
                sb.append(lineSep);
            
            session.out.print(sb.toString());
        }
        
        return 0;
    }
}
