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
package org.sqsh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.digester.Digester;


/**
 * The ConnectionDescriptorManager is responsible for loading and saving
 * the $HOME/.jsqsh/connections.xml file. This file contains description of
 * connection properties are utilized to establish connections to various
 * database servers.
 */
public class ConnectionDescriptorManager {
    
    private static final Logger LOG = 
        Logger.getLogger(ConnectionDescriptorManager.class.getName());
    
    private Map<String, ConnectionDescriptor> descriptors =
        new HashMap<String, ConnectionDescriptor>();
    
    private String filename = null;

    /**
     * Creates a connection descriptor manager that will manage connections. When
     * created in this fashion, with no connection file specified, then the save()
     * method will be unable to perform the save as it will not know what file to
     * save to.  Once the load() method has been called, the file that is loaded will
     * be remembered and used for the next save.
     */
    public ConnectionDescriptorManager() {

    }
    
    /**
     * Creates a connection descriptor manager that will manage connections
     * defined in a specific file.
     * 
     * @param filename The name of the file that describes the connections.
     *   If the save() method is called, this same file will be used to save
     *   the new connection descriptions.
     */
    public ConnectionDescriptorManager(String filename) {
        
        load(filename);
    }
    
    /**
     * Fetch a descriptor by name.
     * 
     * @param name The name of the descriptor.
     * @return A ConnectionDescriptor with the provided name or null 
     *   if there is no descriptor matching that name.
     */
    public ConnectionDescriptor get(String name) {
        
        return descriptors.get(name);
    }
    
    /**
     * Given two connection descriptors, creates a third that is a merged
     * copy of the two.
     * 
     * @param c1 The primary source for the merge.
     * @param c2 The source for overriding values for the merge.
     * @return A copy of c1 that incorporates any new values that may
     *   be provided by c2.
     */
    public ConnectionDescriptor merge(ConnectionDescriptor c1,
            ConnectionDescriptor c2) {
        
        ConnectionDescriptor n = (ConnectionDescriptor) c1.clone();
        if (c2.getCatalog() != null) {
            
            n.setCatalog(c2.getCatalog());
        }
        if (c2.getDomain() != null) {
            
            n.setDomain(c2.getDomain());
        }
        if (c2.getDriver() != null) {
            
            n.setDriver(c2.getDriver());
        }
        if (c2.getJdbcClass() != null) {
            
            n.setJdbcClass(c2.getJdbcClass());
        }
        if (c2.getPassword() != null) {
            
            n.setPassword(c2.getPassword());
        }
        if (c2.getPort() >= 0) {
            
            n.setPort(c2.getPort());
        }
        if (c2.getServer() != null) {
            
            n.setServer(c2.getServer());
        }
        if (c2.getUrl() != null) {
            
            n.setUrl(c2.getUrl());
        }
        if (c2.getUsername() != null) {
            
            n.setUsername(c2.getUsername());
        }
        if (c2.getProperties().size() > 0) {
        
            n.addProperties(c2.getProperties());
        }
        if (c2.getUrlVariables().size() > 0) {

            n.addUrlVariables(c2.getUrlVariables());
        }
        if (c2.isAutoconnect() != false) {
            
            n.setAutoconnect(true);
        }
        
        return n;
    }
    
    /**
     * Adds a descriptor.
     * 
     * @param connDesc The descriptor to add.
     */
    public void put(ConnectionDescriptor connDesc) {
        
        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("Adding " + connDesc.getName());
        }
        
        /*
         * There can be only one autoconnecting descriptor!
         */
        if (connDesc.isAutoconnect()) {
            
            for (ConnectionDescriptor desc : descriptors.values()) {
                
                if (desc != connDesc) {
                    
                    desc.setAutoconnect(false);
                }
            }
        }
        
        descriptors.put(connDesc.getName(), connDesc);
    }
    
    
    /**
     * @return The descriptor that is current set for autoconnect or null
     *   if no descriptors have the attribute set.
     */
    public ConnectionDescriptor getAutoconnectDescriptor() {
        
        for (ConnectionDescriptor desc : descriptors.values()) {
                
            if (desc.isAutoconnect()) {
                
                return desc;
            }
        }
        
        return null;
    }
    
    
    /**
     * Removes a connection descriptor.
     * @param name The name of the descriptor to remove.
     * @return The removed descriptor or null if none matched the name.
     */
    public ConnectionDescriptor remove(String name) {
        
        return descriptors.remove(name);
    }
    
    /**
     * Returns the complete list of descriptors.
     * 
     * @return The complete list of descriptors.
     */
    public ConnectionDescriptor[] getAll() {
        
        return descriptors.values().toArray(new ConnectionDescriptor[0]);
    }
    
    /**
     * Saves connection information.  The connection information will be saved to 
     * the file provided in the constructor for the ConnectionDescriptorManager (if
     * provided) or provided during the most recent call to load().
     */
    public void save() {

        if (filename == null) {

            if (descriptors.size() > 0) {

                LOG.warning("Attempt to save connection descriptions failed. "
                    + "No filename was provided to save to.");
            }

            return;
        }
        
        try {
            
            PrintWriter out = new PrintWriter(
                new FileOutputStream(filename));
            
            out.println("<connections>");
            for (ConnectionDescriptor connDesc : descriptors.values()) {
                
                out.print("   <connection name=\"");
                out.print(connDesc.getName());
                out.println("\"");
                
                if (connDesc.getDriver() != null) {
                    
                    out.print("               driver=\"");
                    out.print(connDesc.getDriver());
                    out.println("\"");
                }
                
                if (connDesc.getServer() != null) {
                    
                    out.print("               server=\"");
                    out.print(connDesc.getServer());
                    out.println("\"");
                }
                
                if (connDesc.getPort() >= 0) {
                    
                    out.print("               port=\"");
                    out.print(connDesc.getPort());
                    out.println("\"");
                }
                
                if (connDesc.isAutoconnect()) {
                    
                    out.println("               autoconnect=\"true\"");
                }
                
                if (connDesc.getDomain() != null) {
                    
                    out.print("               domain=\"");
                    out.print(connDesc.getDomain());
                    out.println("\"");
                }
                
                out.println("   >");
                
                if (connDesc.getUsername() != null) {
                    
                    out.print("      <username><![CDATA[");
                    out.print(connDesc.getUsername());
                    out.println("]]></username>");
                }
                
                if (connDesc.getPassword() != null) {
                    
                    out.print("      <password encrypted=\"true\"><![CDATA[");
                    out.print(connDesc.getEncryptedPassword());
                    out.println("]]></password>");
                }
                
                if (connDesc.getCatalog() != null) {
                    
                    out.print("      <catalog><![CDATA[");
                    out.print(connDesc.getCatalog());
                    out.println("]]></catalog>");
                }
                
                if (connDesc.getUrl() != null) {
                    
                    out.print("      <jdbc-url");
                    if (connDesc.getJdbcClass() != null) {
                        
                        out.print(" class=\"");
                        out.print(connDesc.getClass());
                        out.print("\"");
                    }
                    out.print("><![CDATA[");
                    out.print(connDesc.getUrl());
                    out.println("]]></jdbc-url>");
                }
                
                Map<String,String> props = connDesc.getPropertiesMap();
                if (props.size() > 0) {
                    
                    out.println("      <properties>");
                    for (Entry<String, String> e : props.entrySet()) {

                        out.print("          <property name=\"");
                        out.print(e.getKey());
                        out.print("\"><![CDATA[");
                        out.print(e.getValue());
                        out.println("]]></property>");
                    }
                    out.println("      </properties>");
                }

                Map<String,String> vars = connDesc.getUrlVariablesMap();
                if (vars.size() > 0) {

                    out.println("      <url-variables>");
                    for (Entry<String, String> e : vars.entrySet()) {

                        out.print("          <variable name=\"");
                        out.print(e.getKey());
                        out.print("\"><![CDATA[");
                        out.print(e.getValue());
                        out.println("]]></variable>");
                    }
                    out.println("      </url-variables>");
                }

                out.println("   </connection>");
            }
            
            
            out.println("</connections>");
            out.flush();
            
            out.close();
        }
        catch (IOException e) {
            
            LOG.severe("WARNING: Unable to write to "
                + filename + ": " + e.getMessage());
        }
    }
    
    /**
     * Execute a program that generates a valid connection XML file.
     * @param prog the program to execute
     */
    public void loadFromProgram (String prog) {
        
        Process process;
        try {
        
            process = Runtime.getRuntime().exec(prog);
        }
        catch (IOException e) {
            
            LOG.warning("Failed to execute \"" + prog + "\": " + e.getMessage());
            return;
        }
        
        final BufferedReader err = new BufferedReader(
            new InputStreamReader(process.getErrorStream()));
        final StringBuilder errBuffer = new StringBuilder();
        
        Thread errorConsumer = new Thread() {
            
            public void run() {
                
                try {
                    
                    LOG.fine("Error stream reader running");
                    String line;
                    while ((line = err.readLine()) != null) {
                        
                        errBuffer.append(line).append("\n");
                    }
                }
                catch (IOException e) {
                    
                    // Ignored
                }
                LOG.fine("Error stream reader shut down");
            }
        };
        
        errorConsumer.start();
        InputStream in = process.getInputStream();
        boolean ok = true;
        if (! load(in, prog)) {
            
            ok = false;
            
            // Consume left over input so the program can finish
            try {
                
                byte buffer[] = new byte[1024];
                while ((in.read(buffer)) >= 0) {
                
                    // Nothing to see here. Move along.
                }
            }
            catch (IOException e) {
                
                // Ignored
            }
        }
        
        process.destroy();
        if (!ok && errBuffer.length() > 0) {
            
            LOG.warning(prog + " error output:");
            LOG.warning(errBuffer.toString());
        }
    }
    
    /**
     * Attempts to load the contents of the descriptor file.
     *
     * @param filename Specifies the name of the file to load. The next time save()
     *    is called, this filename will be used as the name of the file to save to.
     */
    public void load(String filename) {

        this.filename = filename;
        
        File file = new File(filename);
        if (file.exists() == false) {
            
            LOG.fine("   Connections file " + filename 
                + " does not exist. Skipping load");
            return;
        }

        if (LOG.isLoggable(Level.FINE)) {

            LOG.fine("   Loading connections file '" + filename + "'");
        }
        
        InputStream in = null;
        try {
            
            in = new FileInputStream(filename);
            load(in, filename);
        }
        catch (IOException e) {
            
            LOG.severe("Failed to load connection descriptor file '"
                + filename + ": " + e.getMessage());
        }
        finally {
            
            if (in != null) {
                
                try { in.close(); } catch (IOException e2) { /* IGNORED */ }
            }
        }
    }
    
    private boolean load(InputStream in, String filename) {

        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        
        path = "connections/connection";
        digester.addObjectCreate(path, ConnectionDescriptor.class.getName());
        digester.addSetNext(path, "put",
            ConnectionDescriptor.class.getName());
        digester.addSetProperties(path);
        
        path = "connections/connection/username";
        digester.addCallMethod(path, 
            "setUsername", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        path = "connections/connection/password";
        digester.addCallMethod(path, 
            "setPassword", 2, new Class[] {
                java.lang.String.class, java.lang.Boolean.class });
            digester.addCallParam(path, 0);
            digester.addCallParam(path, 1, "encrypted");
            
        path = "connections/connection/catalog";
        digester.addCallMethod(path, 
            "setCatalog", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        path = "connections/connection/jdbc-url";
        digester.addCallMethod(path, 
            "setUrl", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
        digester.addCallMethod(path, 
            "setJdbcClass", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "class");
            
        path = "connections/connection/properties/property";
        digester.addCallMethod(path, 
            "setProperty", 2, new Class[] {
                java.lang.String.class, java.lang.String.class });
            digester.addCallParam(path, 0, "name");
            digester.addCallParam(path, 1);

        path = "connections/connection/url-variables/variable";
        digester.addCallMethod(path,
                "setUrlVariable", 2, new Class[] {
                        java.lang.String.class, java.lang.String.class });
        digester.addCallParam(path, 0, "name");
        digester.addCallParam(path, 1);

        digester.push(this); 
        try {
                
            digester.parse(in);
        }
        catch (Exception e) {
                
            LOG.severe("Failed to load connection descriptor from '"
                + filename + ": " + e.getMessage());
            return false;
        }
        
        return true;
    }
}
