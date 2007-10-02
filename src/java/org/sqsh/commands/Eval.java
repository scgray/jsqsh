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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.sqsh.Command;
import org.sqsh.Session;

/**
 * Implements the \eval command.
 */
public class Eval
    extends Command {

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        if (argv.length != 1) {
            
            session.err.println("use: \\eval filename");
            return 1;
        }
        
        File filename = new File(argv[0]);
        if (!filename.canRead()) {
            
            session.err.println("Cannot open '" + filename.toString()
                + "' for read'");
            return 1;
        }
        
        InputStream origIn = session.in;
        boolean wasInteractive = session.isInteractive();
        InputStream in = 
            new BufferedInputStream(new FileInputStream(filename));
        session.setIn(in);
        
        try {
            
            session.setInteractive(false);
            session.readEvalPrint();
        }
        catch (Exception e) {
            
            return 1;
        }
        finally {
            
            session.setIn(origIn);
            session.setInteractive(wasInteractive);
        }
        
        return 0;
    }

}
