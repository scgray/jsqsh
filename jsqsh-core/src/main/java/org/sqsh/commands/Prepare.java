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

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Prepare extends Command implements DatabaseCommand {

    private static class Options extends SqshOptions {

        @Argv(program = "\\prepare", min = 0, max = 0, usage = "")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions options) throws Exception {
        Connection conn = session.getConnection();
        BufferManager bufferMan = session.getBufferManager();
        SQLRenderer sqlRenderer = session.getSQLRenderer();

        String sql = bufferMan.getCurrent().toString();

        int returnCode = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)){
            ResultSetMetaData columns = stmt.getMetaData();

            // Display result columns
            if (columns != null) {
                session.out.println("Result columns");
                sqlRenderer.displayMetadata(session, columns);
                session.out.println();
            }

            ParameterMetaData params = stmt.getParameterMetaData();
            if (params != null) {
                session.out.println("Parameters");
                sqlRenderer.displayParameterMetadata(session, params);
            }
        } catch (SQLException e) {
            SQLTools.printException(session, e);
            returnCode = 1;
        } catch (Throwable e) {
            session.printException(e);
            returnCode = 1;
        } finally {
            // If this is an interactive session, then we create a new buffer to work with, otherwise we just
            // re-use the current buffer.
            if (session.isInteractive()) {
                bufferMan.newBuffer();
            } else {
                bufferMan.getCurrent().clear();
            }
        }

        return returnCode;
    }

}
