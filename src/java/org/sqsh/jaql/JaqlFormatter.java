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
package org.sqsh.jaql;

import org.sqsh.Session;

import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;

/**
 * Interface for a class capable of print JSON.
 */
public abstract class JaqlFormatter {
    
    protected Session session;
    protected JaqlSignalHandler sigHandler = null;
    
    public JaqlFormatter (Session session) {
        
        this.session = session;
    }
    
    /**
     * @return the name of the formatter.
     */
    public abstract String getName();
    
    /**
     * Installs a flagging signal handler that will be (well, SHOULD be)
     * polled by the formatter while it is doing its work to determine
     * if it should abort its work.
     * 
     * @param sigHandler The signal handler to install, or null if you
     *   wish to totally uninstall the handler.
     */
    public void setSignalHandler (JaqlSignalHandler sigHandler) {
        
        this.sigHandler = sigHandler;
    }
    
    /**
     * This should be called by the formatter to determine if the current
     * query was canceled. If it was, then the formatter should attempt
     * to abort its work as soon as possible.
     * 
     * @return true if the query was canceled.
     */
    public final boolean isCanceled() {
        
        if (sigHandler == null)
            return false;
        
        return sigHandler.isTriggered();
    }
    
    
    /**
     * Write the results from a {@link JsonIterator}
     * @param iter The iterator
     * @return The number of "rows" returned, which typically corresponds
     *   to the number of elements in the outer most array.
     * @throws Exception
     */
    public abstract int write (JsonIterator iter)
        throws Exception;
    
    /**
     * Write the results from a {@link JsonValue}
     * @param v The value to write
     * @return The number of "rows" returned, which typically corresponds
     *   to the number of elements in the outer most array.
     * @throws Exception
     */
    public abstract int write (JsonValue v) 
        throws Exception;
}
