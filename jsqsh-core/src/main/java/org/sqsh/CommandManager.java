/*
 * Copyright 2007-2016 Scott C. Gray
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
package org.sqsh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
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
    private static final String INTERNAL_FILE = 
        "org/sqsh/commands/Commands.xml";
    private static final String EXTENSION_FILE = 
        "Commands.xml";
    
    /**
     * The actual map of commands that are available.
     */
    private Map<String, Command>commandMap =
        new HashMap<String, Command>();
    
    /**
     * Creates a new command manager.
     */
    public CommandManager() {
        
        /*
         * Load the built-in commands
         */
        init(null, INTERNAL_FILE);
        
        /*
         * Location that people can provide their own commands for extending jsqsh
         */
        init(null, EXTENSION_FILE);
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
     * Commands may be imported from a directory. The directory must be
     * fully specified and can contain the following:
     * <ul>
     *   <li> It <b>must</b> contain the file Commands.xml, which defines
     *     the commands to be added to jsqsh</li>
     *   <li> It may contain on or more .jar files that implement the 
     *     commands described in the Commands.xml file.</li>
     *   <li> It may contain a program (typically shell script) called
     *     "classpath" or "classpath.*" that returns a classpath that 
     *     should also be used when loading the commands.
     * </ul>
     * 
     * @param directory The directory that contains the commands to be imported.
     * @return Returns the classloader that was used to load the commands
     * @throws CommandImportException if the command cannot be imported
     */
    public void importCommands (ClassLoader loader, String directory) 
        throws CommandImportException {
        
        File dir = new File(directory);
        if (! dir.exists()) {
            
            throw new CommandImportException("Extension directory '" + directory 
                + "' does not exist");
            
        }
        
        File xmlFile = new File(directory, EXTENSION_FILE);
        if (! xmlFile.exists()) {
            
            throw new CommandImportException(xmlFile + " does not exist");
        }
        
        try {
            
            load(loader, xmlFile.toURI().toURL());
        }
        catch (MalformedURLException e) {
            
            throw new CommandImportException("Bad URL: " + xmlFile + ": " 
                + e.getMessage());
        }
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
    private void init(ClassLoader loader, String location) {
        
        try {
            
            Enumeration<URL> inputs = getClass().getClassLoader().getResources(location);
            
            while (inputs.hasMoreElements()) {
                
               URL url = inputs.nextElement();
               load(loader, url);
            }
        }
        catch (Exception e) {
            
            LOG.severe("Unable to load \"" + location + "\": " + e.getMessage());
        }
    }

    /**
     * Load a specific command configuration file
     * 
     * @param loader The classloader to use to instantiate the commands. If null,
     *   then the system classloader is used.
     * @param url The url to the command XML file to load.
     */
    private void load (ClassLoader loader, URL url) 
        throws CommandImportException {

        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        
        if (loader != null) {
            
            digester.setClassLoader(loader);
        }
        
        path = "Commands/Command";
        digester.addObjectCreate(path,  "org.sqsh.Command", "class");
        digester.addSetNext(path, "addCommand", "org.sqsh.Command");
        digester.addCallMethod(path, 
            "setName", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "name");

        path = "Commands/Command/Internal";
        digester.addCallMethod(path, 
            "setHidden", 1, new Class[] { Boolean.TYPE });
            digester.addCallParam(path, 0);
            
        path = "Commands/Command/Description";
        digester.addCallMethod(path, 
            "setDescription", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        path = "Commands/Command/Help";
        digester.addCallMethod(path, 
            "setHelp", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        digester.push(this); 
        try {
            
           InputStream in = null;
           
           try {
                
                in =  url.openStream();
        
                /*
                 * This should never happen unless I manage to build the jar
                 * file incorrectly.
                 */
                if (in == null) {
            
                    LOG.severe("Cannot locate command definition resource " + url);
                    return;
                }
        
                digester.parse(in);
            }
            catch (Exception e) {
                
                LOG.severe("Failed to parse command file '" 
                    + url + "': " + e.getMessage());
            }
            finally {
                
                if (in != null) {
                    
                    try { in.close(); } catch (IOException e) { /* IGNORED */ }
                }
            }
        }
        catch (Exception e) {
            
            throw new CommandImportException("Unable to load \"" + url + "\": " 
                + e.getMessage(), e);
        }
    }
}
