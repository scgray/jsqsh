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
import java.util.ArrayList;
import java.util.List;

import org.sqsh.BufferManager;
import org.sqsh.CannotSetValueError;
import org.sqsh.Command;
import org.sqsh.ConnectionContext;
import org.sqsh.DatabaseCommand;
import org.sqsh.RendererManager;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.Style;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

/**
 * Implements the \go command.
 */
public class Go
    extends Command {
    
    protected static class Options
       extends SqshOptions {
       
        @Option(
            option='m', longOption="display-style", arg=REQUIRED, argName="style",
            description="Sets the display style for output")
        public String style = null;
       
        @Option(
            option='i', longOption="insert", arg=REQUIRED, argName="table",
            description="Generates INSERT statements for specified table")
        public String insertTable = null;

        @Option(
            option='n', longOption="repeat", arg=REQUIRED, argName="count",
            description="Repeates the query execution count times")
        public int repeat = 1;
       
        @Option(
            option='H', longOption="no-headers", arg=NONE,
            description="Toggles display of result header information")
           public boolean toggleHeaders = false;
       
        @Option(
            option='F', longOption="no-footers", arg=NONE,
            description="Toggles display of result footer information")
           public boolean toggleFooters = false;
        
        @Argv(program="\\go", min=0, max=0,
            usage="[-m style] [-i table] [-H] [-F]")
        public List<String> arguments = new ArrayList<String>();
    }
    
    public SqshOptions getOptions() {
        
        return new Options();
    }
   
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        RendererManager renderMan = session.getRendererManager();
        Style origStyle = null;
        String origNull = null;
        int returnCode = 0;
        ConnectionContext conn = session.getConnectionContext();
        
        if (conn == null) {
            
            session.err.println("You are not currently connected to a database "
              + "or another queryable data source. Type 'help \\connect' for "
              + "details");
            return 1;
        }
        
        /*
         * If we are being asked to generate INSERT statements then we need to
         * switch the NULL display to be a form of NULL that works in SQL.
         */
        if (options.insertTable != null) {
            
            options.style = "insert";
            session.setVariable("insert_table", options.insertTable);
        }
        
        if (options.style != null) {
            
            if (options.style.equals("insert")) {
                
                origNull = session.getDataFormatter().getNull();
            	session.getDataFormatter().setNull("NULL");
            }
            
            origStyle = conn.getStyle();
            
            try {
                
                conn.setStyle(options.style);
            }
            catch (CannotSetValueError e) {
                
                session.err.println(e.getMessage());
                return 1;
            }
        }
        
        BufferManager bufferMan = session.getBufferManager();
        SQLRenderer sqlRenderer = session.getSQLRenderer();
        String sql = bufferMan.getCurrent().toString();
        
        boolean origHeaders = renderMan.isShowHeaders();
        boolean origFooters = renderMan.isShowFooters();
        
        if (options.toggleFooters) {
            
            renderMan.setShowFooters(!origFooters);
        }
        if (options.toggleHeaders) {
            
            renderMan.setShowHeaders(!origHeaders);
        }

        long startTime = 0;
        if (options.repeat > 1) {

            startTime = System.currentTimeMillis();
        }

        try {

            for (int i = 0; i < options.repeat; i++) {

                if (options.repeat > 1) {

                    session.setVariable("iteration", Integer.toString(i));
                }

                try {
                    
                    conn.eval(sql, session, sqlRenderer);
                }
                catch (SQLException e) {
                    
                    SQLTools.printException(session, e);
                    returnCode = 1;
                }
                catch (Throwable e) {
                    
                    session.printException(e);
                    returnCode = 1;
                }
            }
        }
        finally {
            
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
            
            if (origNull != null) {
                
                session.getDataFormatter().setNull(origNull);
            }
            
            if (origStyle != null) {
                
                conn.setStyle(origStyle);
            }
            
            renderMan.setShowHeaders(origHeaders);
            renderMan.setShowFooters(origFooters);
        }

        if (options.repeat > 1) {

            long endTime = System.currentTimeMillis();
            System.out.println(
                options.repeat + " iterations (total "
                + (endTime - startTime) + "ms., "
                + ((endTime - startTime) / options.repeat) + "ms. ag)");
        }
        
        return returnCode;
    }
}
