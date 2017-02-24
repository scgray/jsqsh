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
 * This token is returned in response to a &gt;+[n] token on the command
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
     * @param sessionId The sessionId of the target session. An id &lt;= 0
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
