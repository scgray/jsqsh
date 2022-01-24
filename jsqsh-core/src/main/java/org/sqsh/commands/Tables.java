/*
 * Copyright 2007-2022 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.sqsh.SQLConnectionContext;
import org.sqsh.SQLObjectName;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

/**
 * Implements the \tables command.
 */
public class Tables
    extends Command
    implements DatabaseCommand {
    
    private static class Options
        extends SqshOptions {
        
        @OptionProperty(
            option='a', longOption="all", arg=NONE,
            description="Show all available information")
        public boolean showAll = false;
        
        @OptionProperty(deprecated=true,
            option='t', longOption="table-pattern", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against table names")
        public String tablePattern = null;
        
        // Not documented any more
        @OptionProperty(deprecated=true,
            option='s', longOption="schema-pattern", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against schema names")
        public String schemaPattern = null;
        
        @OptionProperty(
            option='T', longOption="type", arg=REQUIRED, argName="type",
            description="Type of tables to return")
        public String type = null;
        
        @Argv(program="\\tables", min=0, max=1,
            usage="[[[catalog.]schema-pattern.]table-pattern]")
      public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }
    
    @Override
    public boolean keepDoubleQuotes() {
        
        return true;
    }    

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        
        SQLObjectName name;
        if (options.arguments.size() > 0) {
            
            name = new SQLObjectName(
                (SQLConnectionContext) session.getConnectionContext(), 
                options.arguments.get(0));
        }
        else {
            
            name = new SQLObjectName(
                (SQLConnectionContext) session.getConnectionContext(), 
                "%");
        }
        
        Connection con = session.getConnection();
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        ResultSet result = null;
        
        try {
            
            String []types = null;
            if (options.type != null) {
                
                types = new String[1];
                if ("TABLE".equals(options.type.toUpperCase())
                    || "USER".equals(options.type.toUpperCase())) {
                    
                    types[0] = "TABLE";
                }
                else if ("VIEW".equals(options.type.toUpperCase())) {
                    
                    types[0] = "VIEW";
                }
                else if ("SYSTEM".equals(options.type.toUpperCase())) {
                    
                    types[0] = "SYSTEM TABLE";
                }
                else if ("ALIAS".equals(options.type.toUpperCase())) {
                    
                    types[0] = "ALIAS";
                }
                else if ("SYNONYM".equals(options.type.toUpperCase())) {
                    
                    types[0] = "SYNONYM";
                }
                else {
                    
                    types[0] = options.type;
                }
            }
            
            
            DatabaseMetaData meta = con.getMetaData();
            
            /*
             * We don't want to show all columns.
             */
            HashSet<Integer> cols = null;
            
            if (options.showAll == false) {
                
                cols = new HashSet<Integer>();
                cols.add(2); /* Owner */
                cols.add(3); /* Table name */
                cols.add(4); /* Table Type */
            }
            
            result = meta.getTables(
                name.getCatalog(),
                options.schemaPattern != null ? options.schemaPattern : name.getSchema(), 
                options.tablePattern != null ? options.tablePattern : name.getName(),
                types);
            
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
