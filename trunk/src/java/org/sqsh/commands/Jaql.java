/*
 * Copyright (C) 2011 by Scott C. Gray
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;
import org.sqsh.jaql.JaqlConnection;
import org.sqsh.jaql.JaqlFormatter;
import org.sqsh.jaql.JsonFormatter;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;

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
        
         @Option(
             option='o', longOption="outoptions", arg=REQUIRED, argName="style",
             description="Sets the display style for output (json, del, or xml)")
         public String style = null;
         
         @Option(
             option='j', longOption="jars", arg=REQUIRED, argName="jars",
             description="Comma separated list of jars to be used")
         public String jars = null;
         
         @Option(
             option='p', longOption="jaql-path", arg=REQUIRED, argName="path",
             description="Colon separated list of module search directories")
         public String jaqlPath = null;
         
         @Option(
             option='i', longOption="indent", arg=REQUIRED, argName="spaces",
             description="Number of spaces to indent when using json formatting")
         public int indent = -1;
         
         @Option(
             option='n', longOption="new-session", arg=NONE,
             description="Create a new session for the connection")
         public boolean newSession = false;
        
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
        JaqlQuery engine  = new JaqlQuery();
        JaqlFormatter formatter = null;
        
        configVariables(session, options);
        
        if (options.indent == -1)
            options.indent = 3;
        
        if (options.style == null || options.style.equals("json")) {
            formatter = new JsonFormatter(session, options.indent);
        }
        else if (options.style.equals("xml") 
            || options.style.equals("del")) {
            
            session.err.println("Sorry format style '" + options.style + "' is"
              + " not yet implemented");
            return 1;
        }
        else {
            
            session.err.println("Invalid format style '" + options.style + "'");
            return 1;
        }
        
        if (options.jaqlPath != null) {
            
            engine.setModuleSeachPath(options.jaqlPath.split(":"));
        }
        
        if (options.jars != null) {
            
            com.ibm.jaql.lang.Jaql.addExtensionJars(options.jars.split(","));
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
}
