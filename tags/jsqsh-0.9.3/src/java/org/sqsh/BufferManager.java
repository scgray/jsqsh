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
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.digester.Digester;

/**
 * The buffer manager manages SQL buffers for sqsh. This object is a
 * part of the session.
 */
public class BufferManager
    implements Cloneable {
    
    /**
     * The maximum command history that will be retained by this buffer
     * manager.
     */
    private int maxBuffers = 50;
    
    /**
     * The list of buffers that we currently have stored. The head of the
     * list is the newest (current) and the tail is the oldest.
     */
    private List<Buffer> buffers = new LinkedList<Buffer> ();
    
    /**
     * Creates a new, empty, buffer.
     * 
     * @return The newly created buffer.
     */
    public Buffer newBuffer() {
        
        Buffer buffer = new Buffer();
        
        buffers.add(0, buffer);
        checkMaxSize();
        return buffer;
    }
    
    /**
     * Creates a new buffer, pre-loaded with SQL.
     * 
     * @param sql The sql.
     */
    public Buffer newBuffer(String sql) {
        
        Buffer buffer = new Buffer(sql);
        
        buffers.add(0, buffer);
        checkMaxSize();
        return buffer;
    }
    
    /**
     * Retrieves a buffer.
     * 
     * @param idx The index of the buffer. The 0'th buffer is the
     *   "current" active buffer, each subsequent buffer is older
     *   than the last.
     *   
     * @return The Buffer or null if the supplied index doesn't point
     *   to a valid buffer.
     */
    public Buffer getBuffer(int idx) {
        
        if (idx >= buffers.size()) {
            
            return null;
        }
        
        return buffers.get(idx);
    }
    
    /**
     * Gets a buffer by name. There are two special syntaxes:
     * <ol>
     *   <li> <b>!.[.....]</b> Indicates a buffer based upon relative position
     *        in history. That is, !. means the current buffer, !.. the
     *        previous, !..., the one before that, etc.
     *   <li> <b>!N</b> Indicates a specific position in history with 0
     *        being the current buffer, 1 the previous buffer, etc.
     * </ol>
     * 
     * @param  The name  of the buffer to fetch.
     * @return The buffer indicated or null.
     */
    public Buffer getBuffer(String bufferName) {
        
        if ("!!".equals(bufferName)) {
            
            bufferName = "!..";
        }
        
        if (bufferName.charAt(0) == '!'
                && bufferName.length() > 1) {
            
            if (bufferName.charAt(1) == '.') {
                
                int idx = 0;
                for (int i = 2; 
                    i < bufferName.length() && bufferName.charAt(i) == '.';
                    ++i) {
                    
                    ++idx;
                }
                
                return getBuffer(idx);
            }
            else if (Character.isDigit(bufferName.charAt(1))){
                
                StringBuilder sb = new StringBuilder(10);
                for (int i = 1; i < bufferName.length()
                    && Character.isDigit(bufferName.charAt(i)); i++) {
                    
                    sb.append(bufferName.charAt(i));
                }
                
                return getBuffer(Integer.valueOf(sb.toString()));
            }
        }
        
        return null;
    }
    
    /**
     * Adds a buffer.
     * 
     * @param buf The buffer to add.
     */
    public void addBuffer(Buffer buf) {
        
        buffers.add(buf);
        checkMaxSize();
    }
    
    /**
     * Returns the number of buffers currently allocated.
     * @return The number of buffers currently allocated.
     */
    public int getBufferCount() {
        
        return buffers.size();
    }
    
    /**
     * Returns the "current" buffer. The current buffer is considered
     * the buffer that the user is currently typing into and is, by 
     * convention, the 0'th buffer.
     * 
     * @return The current buffer.
     */
    public Buffer getCurrent() {
        
        return getBuffer(0);
    }
    
    /**
     * Creates all buffers.
     */
    public void clear() {
        
        buffers.clear();
    }
    
    /**
     * Changes the maximum number of buffers that are going to be
     * stored by this manager, discarding if necessary.
     * 
     * @param maxBuffers The new max buffer setting.
     */
    public void setMaxBuffers(int maxBuffers) {
        
        this.maxBuffers = maxBuffers;
        checkMaxSize();
    }
    
    /**
     * Retrieves the current setting for the maximum number of buffers.
     * 
     * @return The new max buffers.
     */
    public int getMaxBuffers() {
        
        return maxBuffers;
    }
    
    /**
     * Make sure that we haven't overfilled ourselves.
     */
    private void checkMaxSize() {
        
        /*
         * If we have  more than the new max, the start removing
         * from the history.
         */
        while (maxBuffers < buffers.size()) {
            
            buffers.remove(buffers.size() - 1);
        }
    }
    
    /**
     * Attempts to save the current set of buffers off to an external file.
     * @param file The file to save to.
     */
    public void save(File file) {
        
        PrintStream out = null;
        
        try {
            
            out = new PrintStream(file);
            out.println("<Buffers>");
            
            for (int i = 1; i < buffers.size(); i++) {
                
                out.println("   <Buffer><![CDATA[");
                out.println(buffers.get(i));
                out.println("   ]]></Buffer>");
            }
            
            out.println("</Buffers>");
        }
        catch (IOException e) {
            
            System.err.println("WARNING: Unable to write SQL history to "
                + file.toString() + ": " + e.getMessage());
        }
        finally {
            
            if (out != null) {
                
                out.close();
            }
        }
    }
    
    /**
     * Attempts to load a buffer history.
     * @param file The file to read.
     */
    public void load(File file) {
        
        /*
         * Clear out the current history.
         */
        buffers.clear();
        
        /*
         * Create a place holder for the "current" entry.
         */
        buffers.add(new Buffer(""));
        
        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        
        path = "Buffers/Buffer";
        digester.addObjectCreate(path,  "org.sqsh.Buffer");
        digester.addSetNext(path, "addBuffer", "org.sqsh.Buffer");
        digester.addCallMethod(path, 
            "add", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        digester.push(this); 
        try {
            
            digester.parse(file);
        }
        catch (Exception e) {
            
            System.err.println("Failed to load buffer history file '"
                + file.toString() + "': " + e.getMessage());
        }
    }
}
