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

import java.util.Arrays;

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.HelpTopic;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.Variable;

/**
 * Implements the 'help' command.
 */
public class Help
    extends Command {
    
    @Override
    public int execute (Session session, SqshOptions options)
        throws Exception {
        
        int nArgs = options.arguments.size();
        
        if (nArgs > 1) {
            
            session.err.println("use: help [command]");
            return 1;
        }
        
        if (nArgs == 0) {
            
            displayHelpCategories(session);
            return 0;
        }
        
        String word = options.arguments.get(0);
        int rc = 0;
        
        if (word.equals("topics")) {
            
            displayTopics(session);
        }
        else if (word.equals("vars")) {
            
            displayVariables(session);
        }
        else if (word.equals("commands")) {
            
            displayCommands(session);
        }
        else {
            
            rc = displayHelpText(session, word);
        }
        
        return rc;
    }
    
    /**
     * Looks up a specific help topic and displays it.
     * 
     * @param session The session to use for display.
     * @param word The topic to look for.
     * @return 0 if the topic was found and displayed, 1 otherwise.
     */
    private int displayHelpText(Session session, String word) {
        
        int rc = 0;
        
        HelpTopic topic = (HelpTopic) 
            session.getCommandManager().getCommand(word);
            
        /*
         * If we fail to find the command the user asked for it
         * *could* be because the shell interpreted the back slash
         * in front of the command name, so will will append one
         * and try again.
         */
        if (topic == null) {
                
            topic = (HelpTopic) 
                session.getCommandManager().getCommand('\\' + word);
        }
            
        /*
         * If we fail that, try to see if it is a variable that
         * we are talking about.
         */
        if (topic == null) {
                
            topic = (HelpTopic) 
                session.getVariableManager().getVariable(word);
                
            if (topic != null && topic.getDescription() == null) {
                    
                topic = null;
            }
        }
            
        /*
         * If we're still null, then we'll check to see if it is
         * a general topic that is being requested.
         */
        if (topic == null) {
                
            topic = session.getHelpManager().getTopic(word);
        }
            
        /*
         * At this point we're S.O.L.
         */
        if (topic == null) {
                
            session.err.println("No such command or topic '"
                + word + "'");
            rc = 1;
        }
        else {
                
            session.out.println(topic.getHelp());
        }
        
        return rc;
    }
    
    /**
     * Displays available help categories.
     * 
     * @param session The session to be used for displaying.
     */
    private void displayHelpCategories(Session session) {
        
        session.out.println("Available help categories. Use "
            + "\"\\help <category>\" to display topics within that category");
        
        ColumnDescription []columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Category", -1);
        columns[1] = new ColumnDescription("Description", -1);
            
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        renderer.header(columns);
        
        renderer.row(
            new String[] { "commands", "Help on all avaiable commands" });
        renderer.row(
            new String[] { "vars", "Help on all avaiable configuration variables" });
        renderer.row(
            new String[] { "topics", "General help topics for jsqsh" });
        
        renderer.flush();
    }
    
    /**
     * Displays available commands.
     * 
     * @param session The session to be used for displaying.
     */
    private void displayCommands(Session session) {
        
        session.out.println("Available commands. "
            + "Use \"\\help <command>\""
            + " to display detailed help for a given command");
        
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        
        Command []commands = session.getCommandManager().getCommands();
        Arrays.sort(commands);
            
        ColumnDescription []columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Command", -1);
        columns[1] = new ColumnDescription("Description", -1);
        renderer.header(columns);
        
        for (int i = 0; i < commands.length; i++) {
                
            String []row = new String[2];
            row[0] = commands[i].getName();
            row[1] = commands[i].getDescription();
                
            renderer.row(row);
        }
        
        renderer.flush();
    }
    
    /**
     * Displays available variables.
     * 
     * @param session The session to be used for displaying.
     */
    private void displayVariables(Session session) {
        
        session.out.println("Available configuration variables. "
            + "Use \"\\help <variable>\" to display detailed "
            + "help for a given variable");
        
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        
        Variable[] variables =
            session.getVariableManager().getVariables(true);
        Arrays.sort(variables);
        
        ColumnDescription []columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Variable", -1);
        columns[1] = new ColumnDescription("Description", -1);
        renderer.header(columns);
            
        for (int i = 0; i < variables.length; i++) {
                
            if (variables[i].getDescription() != null) {
                    
                String []row = new String[2];
            	row[0] = variables[i].getName();
            	row[1] = variables[i].getDescription();
                
            	renderer.row(row);
            }
        }
        
        renderer.flush();
    }
    
    /**
     * Displays available topics.
     * 
     * @param session The session to be used for displaying.
     */
    private void displayTopics(Session session) {
        
        session.out.println("Available help topics. "
            + "Use \"\\help <topic>\" to display detailed "
            + "help for a given variable");
        
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        
        ColumnDescription []columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Topic", -1);
        columns[1] = new ColumnDescription("Description", -1);
        renderer.header(columns);
        
        HelpTopic []topics = session.getHelpManager().getTopics();
        Arrays.sort(topics);
            
        for (int i = 0; i < topics.length; i++) {
                
            String []row = new String[2];
            row[0] = topics[i].getTopic();
            row[1] = topics[i].getDescription();
                
            renderer.row(row);
        }
        
        renderer.flush();
    }
}