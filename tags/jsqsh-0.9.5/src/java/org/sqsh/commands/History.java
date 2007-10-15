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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.TruncatingLineIterator;

/**
 * Implements the \history command.
 */
public class History
    extends Command {
    
   private static class Options {
        
        @Option(name="-a",usage="Show all of every SQL statement")
            public boolean showAll = false;
        
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        Options options = new Options();
        String lineSep = System.getProperty("line.separator");
        
        int rc = parseOptions(session, argv, options);
        if (rc != 0) {
            
            return rc;
        } 
        
        BufferManager bufMan = session.getBufferManager();
        
        /*
         * Note that we skip buffer 0 because it is the current
         * buffer.
         */
        for (int i = bufMan.getBufferCount() - 1; i > 0; i--) {
            
            StringBuilder sb = new StringBuilder();
            sb.append('(').append(i).append(") ");
            
            int indent = sb.length();
            int line = 0;
            
            TruncatingLineIterator iter = new TruncatingLineIterator(
                bufMan.getBuffer(i).toString(), -1);
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
            
            session.out.print(sb.toString());
        }
        
        return 0;
    }
}
