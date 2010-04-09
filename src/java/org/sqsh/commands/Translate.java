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

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.BufferManager;
import org.sqsh.Buffer;
import org.sqsh.CannotSetValueError;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.RendererManager;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

/**
 * This command is use only by me (Scott) for testing some application
 * code that is specific to the company that I work for.  This command
 * takes the SQL that is about to be executed and wraps it into a 
 * stored procedure call called ACS_REPOS.TRANSLATE_AND_EXECUTE.
 */
public class Translate
    extends Go
    implements DatabaseCommand {

    private static class Options
       extends Go.Options {

        @Option(
            option='s', longOption="spid", arg=REQUIRED, argName="spid",
            description="Sets the sysprocesses spid number of the client")
        public int spid = 0;
    }
    
    public SqshOptions getOptions() {
        
        return new Options();
    }

    /**
     * Used internally to "login" to the Sybase world.
     *
     * @param session Jsqsh session handle.
     * @return spid The spid assigned to the connection.  -1 is returned 
     *   if a spid could not be established.
     */
    private int login(Session session) {

        Connection conn = session.getSQLContext().getConnection();
        CallableStatement stmt = null;
        ResultSet results = null;
        boolean done = false;
        int spid = -1;

        try {

            stmt = conn.prepareCall("{call acs_repos.acs_login(?)}");
            stmt.registerOutParameter(1, java.sql.Types.SMALLINT);

            stmt.execute();
            while (!done) {

                results = stmt.getResultSet();
                if (results != null)
                {
                    while (results.next()) {

                        /* Discard results */
                    }
                }
                else {

                    if (stmt.getUpdateCount() < 0) {

                        done = true;
                    }
                    else {

                        stmt.getMoreResults();
                    }
                }
            }

            spid = stmt.getShort(1);
        }
        catch (SQLException e) {

            SQLTools.printException(session.err, e);
        }
        finally {

            if (stmt != null) {

                SQLTools.close(stmt);
            }
        }

        return spid;
    }
    
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        BufferManager bufferMan = session.getBufferManager();
        SQLRenderer sqlRenderer = session.getSQLRenderer();
        Buffer buffer = bufferMan.getCurrent();
        String sql = buffer.toString();
        StringBuilder call = new StringBuilder();
        int spid = ((Options)opts).spid;

        /*
         * If we weren't force-fed a spid, then see if we have one
         * established.
         */
        if (spid == 0) {

            String str = session.getVariable("acs_spid");
            if (str != null) {

                spid = Integer.valueOf(str);
            }
            else {

                spid = login(session);
                if (spid < 0) {

                    return 1;
                }

                session.setVariable("acs_spid", Integer.toString(spid));
            }
        }

        /*
         * We need to protect any quotes that are in the original SQL.
         */
        int start = 0;
        int end = sql.indexOf('\'');
        if (end >= 0) {

            StringBuilder sb = new StringBuilder();

            while (end >= 0) {

                sb.append(sql, start, end + 1);
                sb.append('\'');
                start = end + 1;
                end   = sql.indexOf('\'', start);
            }

            if (start < sql.length()) {

                sb.append(sql, start, sql.length());
            }

            sql = sb.toString();
        }

        call.append("CALL ACS_REPOS.TRANSLATE_AND_EXECUTE(")
            .append(((Options)opts).spid);

        start = 0;
        boolean done = false;
        for (int i = 0; i < 10; i++) {

            if (done) {

                call.append(",NULL");
            }
            else {

                end = sql.length();
                if ((end - start) > 30000) {

                    end = start + 30000;
                }

                call.append(", '");
                if (start == 0
                    && end == sql.length()) {

                    call.append(sql);
                }
                else {

                    call.append(sql, start, end);
                }
                call.append('\'');

                if (end == sql.length()) {
                    done = true;
                }
            }
        }
        call.append(")");

        buffer.clear();
        buffer.add(call.toString());

        /*
        System.out.println("=======================");
        System.out.println(call.toString());
        System.out.println("=======================");
        */

        return super.execute(session, opts);
    }
}
