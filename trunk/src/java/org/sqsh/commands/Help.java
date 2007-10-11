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
import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.HelpTopic;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.Variable;

/**
 * Implements the 'help' command.
 */
public class Help
    extends Command {
    
    /*
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options {
        
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
        
        if (options.arguments.size() > 1) {
            
            session.err.println("use: help [command]");
            return 1;
        }
        else if (options.arguments.size() == 1) {
            
            HelpTopic topic = (HelpTopic) 
                session.getCommandManager().getCommand(
                    options.arguments.get(0));
            
            /*
             * If we fail to find the command the user asked for it
             * *could* be because the shell interpreted the back slash
             * in front of the command name, so will will append one
             * and try again.
             */
            if (topic == null) {
                
                topic = (HelpTopic) 
                    session.getCommandManager().getCommand(
                        '\\' + options.arguments.get(0));
            }
            
            /*
             * If we fail that, try to see if it is a variable that
             * we are talking about.
             */
            if (topic == null) {
                
                topic = (HelpTopic) 
                    session.getVariableManager().getVariable(
                        options.arguments.get(0));
                
                if (topic != null && topic.getDescription() == null) {
                    
                    topic = null;
                }
            }
            
            /*
             * If we're still null, then we'll check to see if it is
             * a general topic that is being requested.
             */
            if (topic == null) {
                
                topic = session.getHelpManager().getTopic(
                    options.arguments.get(0));
            }
            
            /*
             * At this point we're S.O.L.
             */
            if (topic == null) {
                
                session.err.println("No such command or topic '"
                    + options.arguments.get(0) + "'");
                return 1;
            }
            else {
                
                session.out.println(topic.getHelp());
            }
        }
        else {
            
            Command []commands = session.getCommandManager().getCommands();
            Arrays.sort(commands);
            
            ColumnDescription []columns = new ColumnDescription[3];
            columns[0] = new ColumnDescription("Type", -1);
            columns[1] = new ColumnDescription("Name", -1);
            columns[2] = new ColumnDescription("Description", -1);
            
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
            renderer.header(columns);
            
            for (int i = 0; i < commands.length; i++) {
                
                String []row = new String[3];
                row[0] = "Command";
                row[1] = commands[i].getName();
                row[2] = commands[i].getDescription();
                
                renderer.row(row);
            }
            
            Variable[] variables =
                session.getVariableManager().getVariables(true);
            Arrays.sort(variables);
            
            for (int i = 0; i < variables.length; i++) {
                
                if (variables[i].getDescription() != null) {
                    
                    String []row = new String[3];
                	row[0] = "Variable";
                	row[1] = variables[i].getName();
                	row[2] = variables[i].getDescription();
                
                	renderer.row(row);
                }
            }
            
            HelpTopic []topics = session.getHelpManager().getTopics();
            Arrays.sort(topics);
            
            for (int i = 0; i < topics.length; i++) {
                
                String []row = new String[3];
                row[0] = "Topic";
                row[1] = topics[i].getTopic();
                row[2] = topics[i].getDescription();
                
                renderer.row(row);
            }
            
            renderer.flush();
        }
        
        return 0;
    }
}
