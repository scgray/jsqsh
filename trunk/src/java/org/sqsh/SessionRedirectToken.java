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

/**
 * This token is returned in response to a >+[n] token on the command
 * line and is used to indicate that the user wishes the output of the
 * current session to be processed in the context of another session.
 */
public class SessionRedirectToken
    extends Token {
    
    private int sessionId = -1;
    private boolean append = false;

    /**
     * Creates a session redirect token.
     * 
     * @param sessionId The sessionId of the target session. An id <= 0
     *    indicates that the current session is to be used (a loopback).
     * @param line 
     * @param position
     */
    public SessionRedirectToken(String line, int position, int sessionId,
            boolean append) {

        super(line, position);
        this.sessionId = sessionId;
        this.append = append;
    }
    
    
    public int getSessionId () {
    
        return sessionId;
    }

    public boolean isAppend () {
    
        return append;
    }


    @Override
    public String toString () {
        
        StringBuilder sb = new StringBuilder();
        sb.append('>');
        if (append) {
            
            sb.append('>');
        }
        sb.append('+');
        
        if (sessionId > 0) {
            
            sb.append(sessionId);
        }
        
        return sb.toString();
    }
}
