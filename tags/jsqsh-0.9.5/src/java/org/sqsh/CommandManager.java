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
package org.sqsh;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.digester.Digester;

/**
 * This class is used to manage and look up commands. Upon instantiation
 * the command processes the XML file org/sqsh/commands/Commands.xml and
 * automatically defines the set of commands that are available to the
 * shell.
 */
public class CommandManager {
    
    private static final Logger LOG = 
        Logger.getLogger(CommandManager.class.getName());
    
    /**
     * File containing the command definitions.
     */
    private static final String DEFINITION_FILE = 
        "org/sqsh/commands/Commands.xml";
    
    /**
     * The actual map of commands that are available.
     */
    private Map<String, Command>commandMap =
        new HashMap<String, Command>();
    
    /**
     * Creates a new command manager.
     */
    public CommandManager() {
        
        init();
    }
    
    /**
     * Returns the set of commands defined for this manager.
     * @return The set of commands defined for this manager.
     */
    public Command[] getCommands() {
        
        return commandMap.values().toArray(new Command[0]);
    }
    
    /**
     * Adds a command to the the manager.
     * 
     * @param command The command to add.
     */
    public void addCommand(Command command) {
        
        commandMap.put(command.getName(), command);
        command.setManager(this);
    }
    
    /**
     * Looks up a command.
     * 
     * @param name The name of the command.
     * @return The command or null if the command is not defined.
     */
    public Command getCommand(String name) {
        
        return commandMap.get(name);
    }
    
    /**
     * Performs initialization of the commandMap by processing the XML
     * document in org/sqsh/commands/Commands.xml
     */
    private void init() {
        
        
        
        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        
        path = "Commands/Command";
        digester.addObjectCreate(path,  "org.sqsh.Command", "class");
        digester.addSetNext(path, "addCommand", "org.sqsh.Command");
        digester.addCallMethod(path, 
            "setName", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "name");
            
        path = "Commands/Command/Description";
        digester.addCallMethod(path, 
            "setDescription", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        path = "Commands/Command/Help";
        digester.addCallMethod(path, 
            "setHelp", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        digester.push(this); 
        InputStream in = null;
        try {
            
            in =  getClass().getClassLoader().getResourceAsStream(
                DEFINITION_FILE);
            
            /*
             * This should never happen unless I manage to build the jar
             * file incorrectly.
             */
            if (in == null) {
                
                LOG.severe("Cannot locate command definition resource "
                    + DEFINITION_FILE);
                
                return;
            }
            
            digester.parse(in);
        }
        catch (Exception e) {
            
            LOG.severe("Failed to parse internal command file '"
                + DEFINITION_FILE + "': " + e.getMessage());
        }
        finally {
            
            try {
                
                in.close();
            }
            catch (IOException e) {
                
                /* IGNORED */
            }
        }
    }
}
