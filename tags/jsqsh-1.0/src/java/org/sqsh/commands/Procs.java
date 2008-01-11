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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;

/**
 * Implements the \procs command.
 */
public class Procs
    extends Command {
    
    private static class Options {
        
        @Option(name="-p",
                usage="Provides a pattern to match against procedure names")
            public String procPattern = "%";
        
        @Option(name="-s",
                usage="Provides a pattern to match against schema (owner) names")
            public String schemaPattern = "%";
        
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        Options options = new Options();
        String type = null;
        int rc = parseOptions(session, argv, options);
        if (rc != 0) {
            
            return rc;
        }
        
        if (options.arguments.size() > 0) {
            
            type = options.arguments.get(0);
        }
        
        SQLContext context = session.getSQLContext();
        if (context == null) {
            
            session.err.println("No database connection has been established."
                + " Use the \\connect command to create a connection.");
            return 1;
        }
        
        Connection con = session.getConnection();
        ResultSet result = null;
        try {
            
            DatabaseMetaData meta = con.getMetaData();
            
            /*
             * We don't want to show all columns.
             */
            HashSet<Integer> cols = new HashSet<Integer>();
            cols.add(2); /* Owner */
            cols.add(3); /* Procedure name */
            
            result = meta.getProcedures(con.getCatalog(), 
                options.schemaPattern, options.procPattern);
            
            SQLRenderer sqlRenderer = session.getSQLRenderer();
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
            sqlRenderer.displayResults(renderer, session, result, cols);
        }
        catch (SQLException e) {
            
            session.err.println("Failed to retrieve database metadata: "
                + e.getMessage());
            
            return 1;
        }
        finally {
            
            if (result != null) {
                
                try {
                    
                    result.close();
                }
                catch (SQLException e) {
                    
                    /* IGNORED */
                }
            }
            
        }
        
        return 0;
    }
}
