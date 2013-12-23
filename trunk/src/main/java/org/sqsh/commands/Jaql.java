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

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;
import org.sqsh.jaql.JaqlConnection;
import org.sqsh.jaql.JaqlFormatter;
import org.sqsh.jaql.JsonFormatter;
import org.sqsh.jaql.JsonFormatterFactory;
import org.sqsh.jaql.JsonLinesFormatter;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import com.ibm.jaql.io.converter.JsonToStream;
import com.ibm.jaql.io.stream.converter.DelTextOutputStream;
import com.ibm.jaql.io.stream.converter.JsonTextOutputStream;
import com.ibm.jaql.io.stream.converter.XmlTextOutputStream;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.lang.JaqlQuery;

/**
 * The Jaql command is a bit like "connect" in that it establishes the session
 * as a session to work with the Jaql query language. 
 */
public class Jaql 
    extends Command {
    
    public static class Options
        extends SqshOptions {
        
         @OptionProperty(
             option='o', longOption="outoptions", arg=REQUIRED, argName="style",
             description="Sets the display style for output (json, del, or xml)")
         public String style = null;
         
         @OptionProperty(
             option='j', longOption="jars", arg=REQUIRED, argName="jars",
             description="Comma separated list of jars to be used")
         public String jars = null;
         
         @OptionProperty(
             option='p', longOption="jaql-path", arg=REQUIRED, argName="path",
             description="Colon separated list of module search directories")
         public String jaqlPath = null;
         
         @OptionProperty(
             option='i', longOption="indent", arg=REQUIRED, argName="spaces",
             description="Number of spaces to indent when using json formatting")
         public int indent = -1;
         
         @OptionProperty(
             option='N', longOption="jobname", arg=REQUIRED, argName="name",
             description="Sets the $jaql_jobname variable")
         public String jobName = null;
         
         @OptionProperty(
             option='n', longOption="new-session", arg=NONE,
             description="Create a new session for the connection")
         public boolean newSession = false;
         
         @OptionProperty(
             option='r', longOption="no-rewrites", arg=NONE,
             description="Disables jaql rewrites")
         public boolean disableRewrites = false;
         
         /*
          * DISBALED FOR NOW -- THIS DOESN'T WORK PROPERTY (OR AT LEAST
          * THE WAY I THINK IT SHOULD
          * 
         @Option(
             option='s', longOption="progress", arg=NONE,
             description="Enable progress tracking of MR jobs")
         */
         public boolean progress = false;
        
         @Argv(program="\\jaql", min=0, max=0,
             usage="[-o style] [-j jars] [-p path]")
         public List<String> arguments = new ArrayList<String>();
     }

    @Override
    public SqshOptions getOptions() {

        return new Options();
    }
    
    /**
     * Set up configuration options based upon variables.
     * 
     * @param session The session
     * @param options The options that may be changed by variables.
     */
    private void configVariables (Session session, Options options) {
        
        if (options.style == null) {
            
            options.style = session.getVariable("jaql_style");
        }
        if (options.jars == null) {
            
            options.jars = session.getVariable("jaql_jars");
        }
        if (options.jaqlPath == null) {
            
            options.jaqlPath = session.getVariable("jaql_path");
        }
        if (options.progress == false) {
            
            String str = session.getVariable("jaql_progress");
            if (str != null 
                  && (str.equalsIgnoreCase("true")
                        || str.equalsIgnoreCase("on")
                        || str.equals("1"))) {
                
                options.progress = true;
            }
        }
        if (options.indent == -1) {
            
            String str = session.getVariable("jaql_indent");
            if (str != null) {
                
                options.indent = Integer.parseInt(str);
            }
        }
    }
    
    @Override
    public int execute(Session session, SqshOptions opts)
        throws Exception {

        Options   options = (Options) opts;
        configVariables(session, options);
        
        /*
         * Has to happen before we instantiate the engine.
         */
        if (options.progress) {
            
            System.setProperty("jaql.progress.monitor", "true");
        }
        
        if (options.jobName != null) {
            
            session.setVariable("jaql_jobname", options.jobName);
        }
        
        JaqlQuery engine  = new JaqlQuery();
        JaqlFormatter formatter = null;
        
        if (options.disableRewrites) {
            
            disableRewrites(session, engine);
        }
        
        if (options.indent == -1)
            options.indent = 3;
        
        /*
         * There may be some overlap in SQL display styles and Jaql
         * display styles. If no style was provided, then check to see
         * if the current display style will work.
         */
        if (options.style == null) {
            
            formatter = JsonFormatterFactory.getFormatter(
                session, session.getStyle());
        }
        
        if (formatter == null) {
            
            formatter = JsonFormatterFactory.getFormatter(
                session, options.style, options.indent);
        }
        
        if (formatter == null) {
            
            session.err.println("Display style '" + options.style 
                + "' is not a supported Jaql style. Using default style");
            formatter = JsonFormatterFactory.getFormatter(session, null);
        }
        
        if (options.jaqlPath != null) {
            
            engine.setModuleSeachPath(options.jaqlPath.split(":"));
        }
        
        if (options.jars != null) {
            
            engine.addJars(options.jars.split(","));
        }
        
        JaqlConnection conn = new JaqlConnection(session, engine, formatter);
        
        /*
         * If we are asked to create a new session, then we will do so.
         */
        if (options.newSession) {
            
            Session newSession = session.getContext().newSession(
                session.in, session.out, session.err);
            newSession.setInteractive(session.isInteractive());
            newSession.setConnectionContext(conn);
            
            /*
             * This magic exception is caught by the owning sqsh context
             * to tell it that we want to switch to the newly created
             * session.
             */
            throw new SqshContextSwitchMessage(session, newSession);
        }
        else {
            
            session.setConnectionContext(conn);
        }
        
        return 0;
    }
    
    private void disableRewrites(Session session, JaqlQuery query) {
        
        try {
            
            Method m = JaqlQuery.class.getMethod("enableRewrite", Boolean.TYPE);
            if (m != null) {
                
                m.invoke(query, false);
                session.out.println("Re-writes are disabled");
            }
        }
        catch (Throwable e) {
            
            session.err.println("Cannot disable rewrites: " + e.getMessage());
        }
    }
}
