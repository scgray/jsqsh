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
        
        super(session,  "Please switch to session #" 
            + (targetSession != null ? targetSession.getId() : "<prev>"));
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
