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

import org.sqsh.Command;
import org.sqsh.Session;


/**
 * Implements the \\unset command.
 */
public class Unset
    extends Command {

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        if (argv.length != 1) {
            
            session.err.println("Use: \\unset var_name");
            return 1;
        }
        
        String varName = argv[0];
        if (session.getVariableManager().remove(varName) == null) {
                
            session.getContext().getVariableManager().remove(varName);
        }
        
        return 0;
    }

}
