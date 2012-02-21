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

import org.sqsh.signals.Sig;
import org.sqsh.signals.SigHandler;

import com.ibm.jaql.lang.core.Context;

/**
 * Called when a signal is intercepted during the processing of a Jaql
 * statement. This signal handler should only be installed while a Jaql
 * statement is in-flight and should be immediately installed when the
 * statement is complete
 */
public class JaqlSignalHandler
    implements SigHandler {
    
    private Context context;
    private volatile boolean triggered = false;
    
    public JaqlSignalHandler (Context context) {
        
        this.context = context;
    }
    
    public final boolean isTriggered() {
        
        return triggered;
    }

    @Override
    public void signal(Sig signal) {

        triggered = true;
        context.interrupt();
    }
}
