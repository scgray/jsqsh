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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
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
     * This is the ID of the next buffer that gets created. 
     */
    private int nextBufferId = 1;
    
    /**
     * The list of buffers that we currently have stored. Buffers
     * are stored from oldest to newest, with the last (most recent)
     * buffer in the list being the "current" buffer.
     */
    private List<Buffer> buffers = new LinkedList<Buffer> ();
    
    /**
     * Convenience pointer to the most recent buffer (the last one
     * on the list of buffers).
     */
    private Buffer current = null;
    
    /**
     * Creates a new, empty, buffer.
     * 
     * @return The newly created buffer.
     */
    public Buffer newBuffer() {
        
        Buffer buffer = new Buffer();
        addBuffer(buffer);
        return buffer;
    }
    
    /**
     * Creates a new buffer, pre-loaded with SQL.
     * 
     * @param sql The sql.
     */
    public Buffer newBuffer(String sql) {
        
        Buffer buffer = new Buffer(sql);
        addBuffer(buffer);
        return buffer;
    }
    
    /**
     * Adds a buffer to the manager. The buffer that is added is
     * consider the newest, "current", buffer.
     * 
     * @param buf The buffer to add.
     */
    public void addBuffer(Buffer buf) {
        
        /*
         * Assign an ID if the buffer doesn't already have one
         */
        if (buf.getId() == -1) {
                
            buf.setId(nextBufferId);
            ++nextBufferId;
        }
            
        /*
         * Add it to the list.
         */
        buffers.add(buf);
            
        /*
         * Mark it as the current buffer.
         */
        current = buf;
            
        /*
         * Check to see if we need to discard any old buffers.
         */
        checkMaxSize();
    }
    
    /**
     * Retrieves a buffer.
     * 
     * @param id The id of the buffer buffer to retrieve (as returned
     *   from {@link Buffer#getId()}. The special id "0" is the
     *   "current" active buffer.
     *   
     * @return The Buffer or null if the supplied index doesn't point
     *   to a valid buffer.
     */
    public Buffer getBuffer(int id) {
        
        /*
         * If we are being asked for the "current" buffer, then
         * we want the buffer on the end of the list.
         */
        if (id == 0 && current != null) {
            
            return current;
        }
        
        for (Buffer buf : buffers) {
            
            if (buf.getId() == id) {
                
                return buf;
            }
        }
        
        return null;
    }
    
    /**
     * Gets a buffer by name. There are two special syntaxes:
     * <ol>
     *   <li> <b>!.[.....]</b> Indicates a buffer based upon relative position
     *        in history. That is, !. means the current buffer, !.. the
     *        previous, !..., the one before that, etc.
     *   <li> <b>!N</b> Indicates a specific position in history with 0
     *        being the current buffer.
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
                
                int idx = buffers.size() - 1;
                for (int i = 2; 
                    i < bufferName.length() && bufferName.charAt(i) == '.';
                    ++i) {
                    
                    --idx;
                }
                
                if (idx >= 0) {
                    
                    return buffers.get(idx);
                }
                
                return null;
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
     * Returns the number of buffers currently allocated.
     * @return The number of buffers currently allocated.
     */
    public int getBufferCount() {
        
        return buffers.size();
    }
    
    /**
     * Returns the set of currently allocated buffers. This array
     * is sorted from oldest to newest, with the last entry in the
     * list being the "current" buffer.
     * 
     * @return the set of currently allocated buffers.
     */
    public Buffer[] getBuffers() {
        
        return buffers.toArray(new Buffer[0]);
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
        nextBufferId = 1;
        current = null;
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
         * Remove the first (oldest) entry in the list of buffers until
         * we are down to the maximum number of buffers we are allowed to
         * hold. Note that we leave one additional buffer slot for the
         * "current" buffer.
         */
        while ((maxBuffers+1) < buffers.size()) {
            
            buffers.remove(0);
        }
    }
    
    /**
     * Attempts to save the current set of buffers off to an external file.
     * @param file The file to save to.
     */
    public void save(File file) {
        
        PrintStream out = null;
        
        try {
            
            out = new PrintStream(new FileOutputStream(file));
            out.println("<Buffers>");
            
            /*
             * Write out the buffers in age order, newest to oldest.
             * Note that I am skipping the last buffer because it is
             * the "current" and thus hasn't been made part of the
             * history yet.
             */
            for (int i = buffers.size() - 1; i >= 0; i--) {

                Buffer b = buffers.get(i);
                
                if (!b.isEmpty(true)) {

                    out.println("   <Buffer><![CDATA[");
                    out.println(buffers.get(i));
                    out.println("   ]]></Buffer>");
                }
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
        clear();
        
        /*
         * Historically, the history.xml file was stored newest entry to
         * oldest yet the BufferManager stores them internally oldest to
         * newest. To deal with this, we read our file into a list, then
         * we'll go back and put them into the buffer manager in the
         * proper order.
         */
        List<Buffer> bufferList = new ArrayList<Buffer>();
        
        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        
        path = "Buffers/Buffer";
        digester.addObjectCreate(path,  "org.sqsh.Buffer");
        digester.addSetNext(path, "add", "java.lang.Object");
        digester.addCallMethod(path, 
            "add", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        digester.push(bufferList); 
        try {
            
            digester.parse(file);
        }
        catch (Exception e) {
            
            System.err.println("Failed to load buffer history file '"
                + file.toString() + "': " + e.getMessage());
        }
        
        /*
         * Now, blast back through our bufferList and put them into
         * the manager in the proper order (oldest to newest).
         */
        for (int i = bufferList.size() - 1; i >= 0; --i) {
            
            addBuffer(bufferList.get(i));
        }
        
        /*
         * Create an empty entry for "current".
         */
        newBuffer();
    }
    
}
