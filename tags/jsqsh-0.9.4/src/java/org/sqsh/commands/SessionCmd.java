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

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.SQLContext;
import org.sqsh.Session;
import org.sqsh.SqshContext;
import org.sqsh.SqshContextSwitchMessage;


public class SessionCmd
    extends Command {

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        if (argv.length > 1) {
            
            session.err.println("Use: \\session [session_id]");
            return 1;
        }
        
        SqshContext ctx = session.getContext();
        
        /*
         * If a context id is supplied, then try to switch to it.
         */
        if (argv.length == 1) {
            
            boolean ok = true;
            Session newSession = null;
            int id;
            
            try {
                
                id = Integer.parseInt(argv[0]);
                newSession = ctx.getSession(id);
                if (newSession == null) {
                    
                    ok = false;
                }
            }
            catch (Exception e) {
                
                ok = false;
            }
            
            if (ok == false) {
                
                session.err.println("Invalid session number '" 
                    + argv[0] + "'. Run \\session with no arguments to "
                    + "see a list of available sessions.");
                return 1;
            }
            
            throw new SqshContextSwitchMessage(session, newSession);
        }
        
        Session []sessions = ctx.getSessions();
        
        ColumnDescription []columns = new ColumnDescription[3];
        columns[0] = new ColumnDescription("Id", -1);
        columns[1] = new ColumnDescription("Username", -1);
        columns[2] = new ColumnDescription("URL", -1);
        
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(
                session, columns);
        
        for (int i = 0; i < sessions.length; i++) {
            
            String []row = new String[3];
            if (sessions[i] == ctx.getCurrentSession()) {
                
                row[0] = "* " + Integer.toString(sessions[i].getId());
            }
            else {
                
                row[0] = "  " + Integer.toString(sessions[i].getId());
            }
            
            SQLContext sqlContext = sessions[i].getSQLContext();
            if (sqlContext == null) {
                
                row[1] = "-";
                row[2] = "-";
            }
            else {
                
                row[1] = sessions[i].getVariable("user");
                row[2] = sqlContext.getUrl();
            }
            
            renderer.row(row);
        }
        
        renderer.flush();
        return 0;
    }

}
