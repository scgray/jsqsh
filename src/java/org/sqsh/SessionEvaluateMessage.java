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

import java.io.InputStream;

/**
 * This object should be thrown by any jsqsh command wishing the input
 * for the session to be temporarily redirected from the supplied 
 * input stream.
 */
public class SessionEvaluateMessage
    extends SessionMessage {
    
    private InputStream in;
    private boolean autoClose;
    private boolean isInteractive;
    
    public SessionEvaluateMessage(InputStream in, boolean autoClose,
            boolean isInteractive) {
        
        super("Eval");
        this.in = in;
        this.autoClose = autoClose;
        this.isInteractive = isInteractive;
    }
    
    public boolean isAutoClose() {
        
        return autoClose;
    }
    
    public boolean isInteractive() {
        
        return isInteractive;
    }
    
    public InputStream getInput() {
        
        return in;
    }
}
