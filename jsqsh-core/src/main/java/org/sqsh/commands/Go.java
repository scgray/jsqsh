/*
 * Copyright 2007-2012 Scott C. Gray
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sqsh.*;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;
import org.sqsh.renderers.PivotRenderer;
import org.sqsh.util.TimeUtils;

/**
 * Implements the \go command.
 */
public class Go
    extends Command {
    
    protected static class Options
       extends SqshOptions {
       
        @OptionProperty(
            option='m', longOption="display-style", arg=REQUIRED, argName="style",
            description="Sets the display style for output")
        public String style = null;
       
        @OptionProperty(
            option='i', longOption="insert", arg=REQUIRED, argName="table",
            description="Generates INSERT statements for specified table")
        public String insertTable = null;

        @OptionProperty(
            option='n', longOption="repeat", arg=REQUIRED, argName="count",
            description="Repeates the query execution count times")
        public int repeat = 1;
       
        @OptionProperty(
            option='H', longOption="no-headers", arg=NONE,
            description="Toggles display of result header information")
        public boolean toggleHeaders = false;
       
        @OptionProperty(
            option='F', longOption="no-footers", arg=NONE,
            description="Toggles display of result footer information")
        public boolean toggleFooters = false;
        
        @OptionProperty(
            option='t', longOption="timeout", arg=REQUIRED, argName="sec",
            description="Specifies number of seconds before the query should timeout")
        public int queryTimeout = 0;

        @OptionProperty(
                option='c', longOption="crosstab", arg=REQUIRED, argName="vcol,hval,dcol",
                description="Produces a crosstab of the final results")
        public String crosstab = null;

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
        RendererFactory rendererFactory = null;


        boolean origHeaders = renderMan.isShowHeaders();
        boolean origFooters = renderMan.isShowFooters();
        int origTimeout = conn.getQueryTimeout();
        
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
        
        if (options.queryTimeout > 0) {
            
            conn.setQueryTimeout(options.queryTimeout);
        }

        try {

            if (options.crosstab != null) {

                String[] parts = options.crosstab.split(",");
                if (parts.length != 3) {

                    session.err.println("--crosstab (-c) requires three values vcol,hcol,dcol "
                            + "(vertical column, horizontal column, data column");
                    return 1;
                }

                Renderer currentRenderer = renderMan.getRenderer(session);
                final PivotRenderer pivot = new PivotRenderer(session, renderMan, currentRenderer,
                        parts[0], parts[1], parts[2]);
                final String rendererName = UUID.randomUUID().toString();

                rendererFactory = new RendererFactory() {

                    @Override
                    public Renderer get(String name) {

                        if (name.equals(rendererName)) {

                            return pivot;
                        }

                        return null;
                    }
                };

                renderMan.addFactory(rendererFactory);
                if (origStyle == null) {

                    origStyle = conn.getStyle();
                }

                conn.setStyle(rendererName);
            }

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
             * If we set the connection's query timeout property, then reset 
             * it to zero when we are finished.
             */
            if (options.queryTimeout > 0) {
                
                conn.setQueryTimeout(origTimeout);
            }
            
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

            if (rendererFactory != null) {

                renderMan.removeFactory(rendererFactory);
            }
        }

        if (options.repeat > 1) {

            long endTime = System.currentTimeMillis();
            System.out.println(
                options.repeat + " iterations (total "
                + TimeUtils.millisToDurationString(endTime - startTime) + ", "
                + TimeUtils.millisToDurationString((endTime - startTime) / options.repeat) + " avg)");
        }
        
        return returnCode;
    }
}
