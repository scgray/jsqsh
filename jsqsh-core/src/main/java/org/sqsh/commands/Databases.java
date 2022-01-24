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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.ConnectionContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the \databases command.
 */
public class Databases
    extends Command {
    
    private static class Options
    extends SqshOptions {
    
        @Argv(program="\\databases", min=0, max=0)
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
    public int execute (Session session, SqshOptions options)
        throws Exception {
        
        Connection con = session.getConnection();
        if (con == null) {
            
            session.err.println("No database connection has been established."
                + " Use the \\connect command to create a connection.");
            return 1;
        }
        
        ResultSet result = null;
        try {
            
            DatabaseMetaData meta = con.getMetaData();
            
            result = meta.getCatalogs();
            
            SQLRenderer sqlRenderer = session.getSQLRenderer();
            Renderer renderer = session.getRendererManager()
                .getCommandRenderer(session);
            
            sqlRenderer.displayResults(renderer, session, result, null);
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
