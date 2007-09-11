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
 * Thrown from a session that wishes to switch to another session.
 */
public class SqshContextSwitchMessage
    extends SqshContextMessage {
    
    private Session targetSession;
    private boolean endCurrent = false;
    
    /**
     * Create a message to request that the SqshContext switch its session
     * from the current session to a specified target session.
     * 
     * @param session The current session.
     * @param targetSession The target session. If targetSession is null, then
     *   the next available session will be utilized.
     */
    public SqshContextSwitchMessage (Session session, Session targetSession) {
        
        super(session, "Please switch to session #" + targetSession.getId());
        this.targetSession = targetSession;
    } 
    
    /**
     * Create a message to request that the SqshContext switch its session
     * from the current session to a specified target session.
     * 
     * @param session The current session.
     * @param targetSession The target session. If targetSession is null, then
     *   the next available session will be utilized.
     * @param endCurrent Indicates that the current session should be ended
     *   during the switch.
     */
    public SqshContextSwitchMessage (Session session, Session targetSession,
            boolean endCurrent) {
        
        super(session, "Please switch to " + 
            (targetSession == null ? " next available session"
                    :  ("session #" + targetSession.getId()) + ")")
            + (endCurrent ? " (and end the current session)" : ""));
        
        this.targetSession = targetSession;
        this.endCurrent = endCurrent;
    } 
    
    public Session getTargetSession() {
        
        return targetSession;
    }
    
    public boolean isEndingCurrent() {
        
        return endCurrent;
    }
}
