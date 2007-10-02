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

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


/**
 * Core definition of a sqsh command.
 */
public abstract class Command
    extends HelpTopic
    implements Comparable {
    
    private static final Logger LOG = 
        Logger.getLogger(Command.class.getName());
    
    private String name;
    
    /**
     * Handle back to the manager that owns this command.
     */
    private CommandManager manager = null;
    
    /**
     * This construtor should only be called by the CommandManager.
     */
    public Command () {
        
        name = null;
    }
    
    /**
     * Base constructor for a command.
     * @param name The name of the command.
     * @param description A brief description of what the command does.
     * @param helpText The full help text for the command.
     */
    public Command (String name, String description, String helpText) {
        
        super(name, description, helpText);
        this.name = name;
    }
    
    /**
     * Used by the CommandManager to record itself as being the "owner"
     * of this command.
     * 
     * @param manager The owner of the command.
     */
    protected void setManager(CommandManager manager) {
        
        this.manager = manager;
    }
    
    /**
     * This method must be implemented by the command and defines its
     * behavior.
     * @param session The session context in which the command is
     *   being executed.
     * @param argv The arguments to the command, this follows the same
     *   behavior as the main() method.
     * @return The "exit code" for the command. By convention, 0 indicates
     *   a successful execution.
     * @throws Exception Thrown if the command fails.
     */
    public abstract int execute(Session session, String argv[])
        throws Exception;
    
    /**
     * This is a helper method that a command can call to do their
     * command line processing. It takes care of basic error handling
     * and usage printing.
     * 
     * @param session The session in which the command is being executed.
     * @param argv The arguments being processed.
     * @param options The option descriptor.
     * @return 0 upon success, non-zero if there is an error.
     */
    public int parseOptions(Session session, String argv[],
            Object options) {
        
        CmdLineParser parser = new CmdLineParser(options);
        
        try {
            
            parser.parseArgument(argv);
        }
        catch (CmdLineException e) {
            
            session.err.println(e.getMessage());
            parser.printUsage(session.err);
            
            return 1;
        }
        
        return 0;
    }
    
    /**
     * Helper method to print the usage.
     * 
     * @param session
     * @param options
     */
    public void printUsage(Session session, Object options) {
        
        CmdLineParser parser = new CmdLineParser(options);
        parser.printUsage(session.err);
    }
    
    /**
     * Set the name of the command.
     * 
     * @param name The name of the command.
     */
    public void setName(String name) {
        
        setTopic(name);
        this.name = name;
    }
    
    /**
     * Returns the name of the variable.
     * 
     * @return The name of the variable.
     */
    public String getName() {
        
        return name;
    }
    
    /**
     * Compares the names of two commands. This method is provided primarily
     * to allow for easy sorting of commands on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(Object o) {
        
        if (o instanceof Command) {
            
            return name.compareTo(((Command) o).getName());
        }
        
        return -1;
    }
    
    /**
     * Compares two commands for equality. Commands are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is a Command that has the same name as this.
     */
    public boolean equals(Object o) {
        
        if (o instanceof Command) {
            
            return ((Command) o).getName().equals(name);
        }
        
        return false;
    }
    
    /**
     * Returns a hash value for the command. The hash code is nothing
     * more than the hash code for the command name itself.
     * 
     * @return The hash code of the command's name.
     */
    public int hashCode() {
        
        return name.hashCode();
    }
}
