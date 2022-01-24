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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.ConnectionContext;
import org.sqsh.Renderer;
import org.sqsh.RendererFactory;
import org.sqsh.RendererManager;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.VariableManager;
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
                option='v', longOption="var", arg=REQUIRED, argName="name=value",
                description="Sets a jsqsh variable for the duration of the command")
        public List<String> vars = new ArrayList<>();

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
        int returnCode = 0;
        ConnectionContext conn = session.getConnectionContext();
        Map<String, String> origVariableValues = new HashMap<String, String>();

        if (conn == null) {
            
            session.err.println("You are not currently connected to a database "
              + "or another queryable data source. Type 'help \\connect' for "
              + "details");
            return 1;
        }

        VariableManager varMan = session.getVariableManager();
        BufferManager bufferMan = session.getBufferManager();
        SQLRenderer sqlRenderer = session.getSQLRenderer();
        String sql = bufferMan.getCurrent().toString();
        RendererFactory rendererFactory = null;

        int origTimeout = conn.getQueryTimeout();

        if (options.style != null) {

            if (options.style.equals("insert")) {

                set(varMan, origVariableValues, "null", "NULL");
            }
            set(varMan, origVariableValues, "style", options.style);
        }

        /*
         * If we are being asked to generate INSERT statements then we need to
         * switch the NULL display to be a form of NULL that works in SQL.
         */
        if (options.insertTable != null) {

            set(varMan, origVariableValues, "style", "insert");
            set(varMan, origVariableValues, "insert_table", options.insertTable);
        }

        if (options.toggleFooters) {

            set(varMan, origVariableValues, "footers", Boolean.toString(!renderMan.isShowFooters()));
        }
        if (options.toggleHeaders) {

            set(varMan, origVariableValues, "headers", Boolean.toString(!renderMan.isShowHeaders()));
        }

        if (options.vars.size() > 0) {

            for (String nameValue : options.vars) {

                String name = nameValue;
                int idx = nameValue.indexOf('=');
                if (idx > 0) {

                    name = nameValue.substring(0, idx);
                }

                origVariableValues.put(name, varMan.get(name));
                varMan.put(nameValue);
            }
        }

        long startTime = 0;
        if (options.repeat > 1) {

            startTime = System.currentTimeMillis();
        }
        
        if (options.queryTimeout > 0) {
            
            conn.setQueryTimeout(options.queryTimeout);
        }

        try {

            /*
             * If the user has asked for a crosstab, I go through some wacky chicanery here.
             * To do the crosstab there is a "special" renderer called the PivotRenderer. This
             * one isn't registered or created like a normal renderer. Instead we create it,
             * and temporarily register it with the renderer manager under a "random" name,
             * then we set this name as the display style to use.  At the end, the renderer is
             * unregistered.
             */
            if (options.crosstab != null) {

                String[] parts = options.crosstab.split(",");
                if (parts.length != 3) {

                    session.err.println("--crosstab (-c) requires three values vcol,hcol,dcol "
                            + "(vertical column, horizontal column, data column");
                    return 1;
                }

                Renderer currentRenderer = renderMan.getRenderer(session);

                // Create the renderer and give it a random name
                final PivotRenderer pivot = new PivotRenderer(session, renderMan, currentRenderer,
                        parts[0], parts[1], parts[2]);
                final String rendererName = UUID.randomUUID().toString();

                // Register a factory with the renderer manager that will serve up our renderer
                // whenever it is asked for
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
             * Restore variables to their original state
             */
            if (origVariableValues.size() > 0) {

                for (Map.Entry<String, String> e : origVariableValues.entrySet()) {

                    varMan.put(e.getKey(), e.getValue());
                }
            }

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

    private static void set(VariableManager varMan, Map<String, String> priorValues, String name, String value) {

        if (! priorValues.containsKey(name)) {

            priorValues.put(name, varMan.get(name));
        }

        varMan.put(name, value);
    }

    private static void set(VariableManager varMan, Map<String, String> priorValues, String nameValue) {

        String name = nameValue;
        String value = null;
        int idx = nameValue.indexOf('=');
        if (idx > 0) {

            name = nameValue.substring(0, idx);
            value = nameValue.substring(idx+1);
        }

        set(varMan, priorValues, name, value);
    }
}
