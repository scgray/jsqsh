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
import static org.sqsh.options.ArgumentRequired.REQUIRED;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.Renderer;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

/**
 * Implements the \tables command.
 */
public class Tables
    extends Command
    implements DatabaseCommand {
    
    private static class Options
        extends SqshOptions {
        
        @Option(
            option='t', longOption="table-pattern", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against table names")
        public String tablePattern = "%";
        
        @Option(
            option='s', longOption="schema-pattern", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against schema names")
        public String schemaPattern = "%";
        
        @Argv(program="\\tables", min=0, max=1,
            usage="[-t table-pattern] [-s schema-pattern] [type]")
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
        String type = null;
        
        if (options.arguments.size() > 0) {
            
            type = options.arguments.get(0);
        }
        
        Connection con = session.getConnection();
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        ResultSet result = null;
        
        try {
            
            String []types = null;
            if (type != null) {
                
                types = new String[1];
                if ("TABLE".equals(type.toUpperCase())
                    || "USER".equals(type.toUpperCase())) {
                    
                    types[0] = "TABLE";
                }
                else if ("VIEW".equals(type.toUpperCase())) {
                    
                    types[0] = "VIEW";
                }
                else if ("SYSTEM".equals(type.toUpperCase())) {
                    
                    types[0] = "SYSTEM TABLE";
                }
                else if ("ALIAS".equals(type.toUpperCase())) {
                    
                    types[0] = "ALIAS";
                }
                else if ("SYNONYM".equals(type.toUpperCase())) {
                    
                    types[0] = "SYNONYM";
                }
                else {
                    
                    session.err.println("The object type '" 
                        + type + "' is not recognized. "
                        + "Valid types are: user, system, view, "
                        + "alias, synonym");
                    return 1;
                }
            }
            
            
            DatabaseMetaData meta = con.getMetaData();
            
            /*
             * We don't want to show all columns.
             */
            HashSet<Integer> cols = new HashSet<Integer>();
            cols.add(2); /* Owner */
            cols.add(3); /* Table name */
            cols.add(4); /* Table Type */
            
            result = meta.getTables(con.getCatalog(),
                options.schemaPattern, options.tablePattern, types);
            
            SQLRenderer sqlRenderer = session.getSQLRenderer();
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
