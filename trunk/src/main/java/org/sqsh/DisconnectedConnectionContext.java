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

import java.sql.SQLException;

/**
 * This class implements the "connection" that is available when a session
 * is not connected to anything. It's main purpose is to allow the user
 * to set "connection" level settings which this class then passes back
 * down to the session or the main context. For example, when the
 * "queryTimeout" setting is set and there is no connection, then this
 * transfers the setting down to the main SqshContext class.  When new
 * connections (ConnectionContext) objects are created, they automatically
 * transfer these global settings into themselves.
 */
public class DisconnectedConnectionContext extends ConnectionContext {
    
    public DisconnectedConnectionContext(Session session) {
        
        super(session);
    }
    
    @Override
    public int getQueryTimeout() {

        /*
         * Return only the main context's setting.
         */
        return session.getContext().getQueryTimeout();
    }


    @Override
    public void setQueryTimeout(int timeout) {

        /*
         * Transfer this setting to the main context
         */
        session.getContext().setQueryTimeout(timeout);
    }

    @Override
    public boolean supportsQueryTimeout() {

        return false;
    }

    @Override
    public void evalImpl(String batch, Session session, SQLRenderer renderer)
        throws Exception {

        throw new SQLException("Session is not connected. "
            + "Run \"\\help connect\" for details");
    }

    @Override
    public void cancel() throws Exception {

        /* EMPT */
    }

    @Override
    public Style getStyle() {

        return new Style(session.getRendererManager().getDefaultRenderer());
    }

    @Override
    public void setStyle(Style style) {

        session.getRendererManager().setDefaultRenderer(style.getName());
    }

    @Override
    public void setStyle(String name) {

        session.getRendererManager().setDefaultRenderer(name);
    }

    @Override
    public boolean isTerminated(String batch, char terminator) {

        return false;
    }
}
