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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
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
    
    private String filename;
    
    /**
     * Creates a connection descriptor manager that will manage connections
     * defined in a specific file.
     * 
     * @param filename The name of the file that describes the connections.
     */
    public ConnectionDescriptorManager(String filename) {
        
        this.filename = filename;
        load();
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
        if (c2.getSid() != null) {
            
            n.setSid(c2.getSid());
        }
        if (c2.getUrl() != null) {
            
            n.setUrl(c2.getUrl());
        }
        if (c2.getUsername() != null) {
            
            n.setUsername(c2.getUsername());
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
        
        descriptors.put(connDesc.getName(), connDesc);
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
    
    public void save() {
        
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
                
                if (connDesc.getSid() != null) {
                    
                    out.print("               sid=\"");
                    out.print(connDesc.getSid());
                    out.println("\"");
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
                    out.println("]]></jdbc-url");
                }
                
                out.println("   </connection>");
            }
            
            
            out.println("</connections>");
            out.flush();
            
            out.close();
        }
        catch (IOException e) {
            
            LOG.warning("WARNING: Unable to write to "
                + filename + ": " + e.getMessage());
        }
    }
    
    /**
     * Attempts to load the contents of the descriptor file.
     */
    private void load() {
        
        File file = new File(filename);
        if (file.exists() == false) {
            
            if (LOG.isLoggable(Level.FINE)) {

                LOG.fine("File " + filename + " does not exist. Skipping load");
            }
            
            return;
        }
        
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
            
        digester.push(this); 
        try {
                
            digester.parse(file);
        }
        catch (Exception e) {
                
            LOG.severe("Failed to load connection descriptor file '"
                + filename + ": " + e.getMessage());
        }
    }
}
