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

import java.io.File;
import java.io.IOException;
import org.sqsh.Buffer;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SessionRedrawBufferMessage;
import org.sqsh.SqshOptions;
import org.sqsh.jni.Shell;

public class Edit
    extends Command {

    @Override
    public int execute (Session session, SqshOptions options) 
        throws Exception {
        
        String readBuffer = "!.";
        String writeBuffer = "!.";
        
        if (options.arguments.size() > 2) {
            
            session.err.println("Use: \buf-edit [read-buf [write-buf]]");
            return 1;
        }
        
        BufferManager bufMan = session.getBufferManager();
        
        /*
         * If no arguments are provided then we check the current buffer,
         * if it is empty we try to edit the previous buffer.
         */
        if (options.arguments.size() == 0) {
            
            Buffer current = bufMan.getCurrent();
            if (current.getLineNumber() == 1 
                    && bufMan.getBuffer("!..") != null) {
                
                readBuffer = "!..";
            }
        }
        else {
            
            readBuffer = options.arguments.get(0);
            if (options.arguments.size() == 2) {
                
                writeBuffer = options.arguments.get(1);
            }
        }
        
        Buffer readBuf = bufMan.getBuffer(readBuffer);
        if (readBuf == null) {
            
            session.err.println("Buffer '" + readBuffer
                + "' does not exist");
            return 1;
        }
        
        Buffer writeBuf = bufMan.getBuffer(writeBuffer);
        if (writeBuf == null) {
            
            session.err.println("Buffer '" + writeBuffer
                + "' does not exist");
            return 1;
        }
        
        String editor = getEditor(session);
        File tmpFile = null;
        Shell s = null;
        
        try {
            
            tmpFile = File.createTempFile("jsqsh", ".tmp");
            readBuf.save(tmpFile);
            
            s = session.getShellManager().detachShell(
                editor + " " + tmpFile.toString());
            
            try {
                
                s.waitFor();
            }
            catch (InterruptedException e) {
                
                /* IGNORED */
            }
            
            writeBuf.load(tmpFile);
        }
        catch (IOException e) {
            
            session.err.println(e.toString());
        }
        finally {
            
            if (tmpFile != null) {
                
                tmpFile.delete();
            }
        }
        
        if (writeBuf == bufMan.getCurrent()) {
            
            throw new SessionRedrawBufferMessage();
        }
        
        return 0;
    }
    
    /**
     * Determines which editor to use.
     * @param session The session used to retrieve variables
     * @return The editor.
     */
    public String getEditor (Session session) {
        
        String editor = session.getVariable("EDITOR");
        if (editor == null) {
            
            editor = session.getVariable("VISUAL");
        }
        
        if (editor == null) {
            
            String os = System.getProperty("os.name");
            if (os.indexOf("Wind") > 0) {
                
                editor = "notepad.exe";
            }
            else {
                
                editor = "vi";
            }
        }
        
        return editor;
    }
}
