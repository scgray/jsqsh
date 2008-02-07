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

import org.sqsh.Buffer;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;

/**
 * Implements the \macro command.
 */
public class Macro
    extends Command {

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Buffer buf = session.getBufferManager().getCurrent();
        if (buf.getLineNumber() == 1) {
            
            session.err.println("No macro definition has been supplied.");
            return 1;
        }
        
        String macro = buf.toString();
        try {
            
            session.expand(macro);
            session.out.println("Ok.");
            buf.clear();
        }
        catch (Exception e) {
            
            session.err.print("Failed to create macro: " + e.getMessage());
            return 1;
        }
        
        return 0;
    }

}
