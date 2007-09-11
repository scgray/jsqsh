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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.CannotSetValueError;
import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.Variable;
import org.sqsh.VariableManager;

/**
 * Implements the 'set' command in sqsh.
 */
public class Set
    extends Command {
    
    /*
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options {
        
        @Option(name="-x",usage="Export the variable")
            public boolean doExport = false;
        
        @Option(name="-l",usage="Define the variable locally to this session")
            public boolean isLocal = false;
        
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        Options options = new Options();
        int rc = parseOptions(session, argv, options);
        if (rc != 0) {
            
            return rc;
        }
        
        /*
         * Gotta have at least one option (case of "x=y");
         */
        if (options.arguments.size() == 0) {
            
            VariableManager varMan = session.getVariableManager();
            Variable []vars = varMan.getVariables(true);
            Arrays.sort(vars);
            
            ColumnDescription []columns = new ColumnDescription[2];
            columns[0] = new ColumnDescription("Variable", -1);
            columns[1] = new ColumnDescription("Value", -1);
            
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(
                    session, columns);
            
            for (int i = 0; i < vars.length; i++) {
                
                String []row = new String[2];
                row[0] = vars[i].getName();
                row[1] = vars[i].toString();
                
                renderer.row(row);
            }
            
            renderer.flush();
            return 0;
        }
        
        /*
         * We will allow the user to be a little lazy and specity the
         * set as any one of the following:
         * 
         *    x=y
         *    x = y
         *    x =y
         */
        String name = null;;
        StringBuilder value = new StringBuilder();
        int argIdx = 1;
        
        String s = options.arguments.get(0);
        int equalIdx = s.indexOf('=');
        
        /*
         * If we've got an equals contained in argv[0]...
         */
        if (equalIdx > 0) {
            
            /*
             * Everything left of the equals is the variable name.
             */
            name = s.substring(0, equalIdx);
            
            /*
             * If there is anything following the equals, then use it.
             */
            if (equalIdx < s.length() - 1) {
                
                value.append(s.substring(equalIdx + 1));
            }
            
            argIdx = 1;
        }
        else {
            
            name = options.arguments.get(0);
            
            /*
             * No equals in argv[0], it better be in argv[1];
             */
            if (options.arguments.size() < 2) {
                
                printUsage(session, options);
                return 1;
            }
            
            /*
             * Get the next argument, and make sure it either is
             * or begins with an equals.
             */
            s = options.arguments.get(1);
            if (s.startsWith("=") == false) {
                
                printUsage(session, options);
                return 1;
            }
            
            if (s.length() > 0) {
                
                value.append(s.substring(1));
            }
            
            argIdx = 2;
        }
        
        for (; argIdx < options.arguments.size(); argIdx++) {
            
            if (value.length() > 0) {
                
                value.append(' ');
            }
            value.append(options.arguments.get(argIdx));
        }
        
        try {
            
            if (options.isLocal 
                    || session.getVariableManager().containsLocal(name)) {
                
                session.getVariableManager().put(
                    name, value.toString(), options.doExport);
            }
            else {
                
                session.getContext().getVariableManager().put(
                    name, value.toString(), options.doExport);
            }
        }
        catch (CannotSetValueError e) {
            
            session.err.println(e.getMessage());
            return 1;
            
        }
        
        return 0;
    }
}