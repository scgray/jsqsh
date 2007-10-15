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
 * This exception is throw from a Session when it wishes to to tell its
 * containing context somthing (such as, "please exit").
 */
public abstract class SqshContextMessage
    extends Error  {
    
    private Session session;
    
    /**
     * Creates a new context message.
     * 
     * @param session The session sending the message.
     * @param msg The message being sent.
     */
    public SqshContextMessage (Session session, String msg) {
        
        super("Session #" + session.getId() + ": " + msg);
        this.session = session;
    }
    
    /**
     * Returns the session that issued the message.
     * @return the session that issued the message.
     */
    public Session getSession() {
        
        return session;
    }
}
