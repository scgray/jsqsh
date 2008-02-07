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
import org.sqsh.Alias;
import org.sqsh.AliasManager;
import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;

public class AliasCmd
    extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Option(name="-G",usage="Make the alias global (apply to whole line)")
            public boolean isGlobal = false;
    }
    
    
    /**
     * Return our overridden options.
     */
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        if (options.arguments.size() == 0) {
            
            AliasManager aliasMan = session.getAliasManager();
            Alias []aliases = aliasMan.getAliases();
            Arrays.sort(aliases);
            
            ColumnDescription []columns = new ColumnDescription[3];
            columns[0] = new ColumnDescription("Alias", -1);
            columns[1] = new ColumnDescription("Global?", -1);
            columns[2] = new ColumnDescription("Text", -1);
            
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
            
            renderer.header(columns);
            
            for (Alias alias : aliases) {
                    
                String []row = new String[3];
                    
                row[0] = alias.getName();
                row[1] = alias.isGlobal() ? "Y" : "N";
                row[2] = alias.getText();
                    
                renderer.row(row);
            }
            
            renderer.flush();
            return 0;
        }
        
        /*
         * We will allow the user to be a little lazy and specity the
         * alias as any one of the following:
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
        
        session.getAliasManager().addAlias(
            new Alias(name, value.toString(), options.isGlobal));
        
        return 0;
    }
}
