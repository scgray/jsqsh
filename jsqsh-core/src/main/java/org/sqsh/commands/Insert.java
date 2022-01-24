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

import static org.sqsh.options.ArgumentRequired.REQUIRED;
import static org.sqsh.options.ArgumentRequired.NONE;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.RendererManager;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;
import org.sqsh.renderers.InsertRenderer;


public class Insert 
    extends Command
    implements DatabaseCommand {
    
    private static class Options
        extends SqshOptions {
        
        @OptionProperty(
            option='s', longOption="target-session", arg=REQUIRED, argName="id",
            description="Target session in which inserts are executed")
        public int sessionId = -1;
        
        @OptionProperty(
            option='b', longOption="batch-size", arg=REQUIRED, argName="rows",
            description="Number of rows per batch")
         public int batchSize = 50;

        @OptionProperty(
            option='t', longOption="terminator", arg=REQUIRED, 
            argName="terminator", 
            description="Command used to terminator a batch")
         public String batchTerminator = "go";

        @OptionProperty(
            option='m', longOption="multi-row", arg=NONE, 
            argName="multi-row", 
            description="Allow multiple rows per insert")
         public boolean multiRowInsert = false;
        
        @Argv(program="\\insert", min=1, max=1,
            usage="[-s target-session] [-b batch-size] [-t terminator] "
                  + "table_name")
        public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }
    
    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        RendererManager renderMan = session.getRendererManager();
        Options options = (Options) opts;
        
        /*
         * Make sure the user provided at least a table name.
         */
        if (options.arguments.size() != 1) {
            
            session.err.println("use: \\insert [options] table_name");
            return 1;
        }
        
        String table = options.arguments.get(0);
        Connection targetConnection = null;
        
        /*
         * If a session Id was provided, then try to get its connection.
         */
        if (options.sessionId > 0) {
            
            Session targetSession = session.getContext().getSession(
                options.sessionId);
            if (targetSession == null) {
                
                session.err.println("The provided session id '" 
                    + options.sessionId + "' is not valid. Use \\session "
                    + "to view valid session ids");
                return 1;
            }
            
            targetConnection = targetSession.getConnection();
            if (targetConnection == null) {
                
                session.err.println("Session " + options.sessionId + 
                    " is not currently connected to a server.");
                return 1;
            }
        }
        
        
        /*
         * Set up the insert renderer based upon the provided input
         * parameters.
         */
        InsertRenderer renderer = (InsertRenderer) 
            renderMan.getRenderer(session, "insert");
        
        renderer.setTable(table);
        renderer.setBatchSize(options.batchSize);
        renderer.setConnection(targetConnection);
        renderer.setBatchTerminator(options.batchTerminator);
        renderer.setMultiRowInsert(options.multiRowInsert);
        
        /*
         * Get the current SQL statement.
         */
        BufferManager bufferMan = session.getBufferManager();
        SQLRenderer sqlRenderer = session.getSQLRenderer();
        String sql = bufferMan.getCurrent().toString();
        
        /*
         * If this is an interactive session, then we create a new
         * buffer to work with, otherwise we just re-use the current
         * buffer.
         */
        if (session.isInteractive()) {
            
            bufferMan.newBuffer();
        }
        else {
            
            bufferMan.getCurrent().clear();
        }
        
        try {
            
            sqlRenderer.execute(renderer, session, sql);
        }
        catch (SQLException e) {
            
            SQLTools.printException(session, e);
        }
        
        return 0;
    }

}
