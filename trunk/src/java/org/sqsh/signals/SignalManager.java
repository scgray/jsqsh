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
package org.sqsh.signals;

import java.lang.reflect.Constructor;
import java.util.Stack;

/**
 * The SignalManager is a singleton responsible for catching a user's
 * request to stop processing CTRL-C, it then dispatches it to the 
 * appropriate registered listener for handling.
 */
public class SignalManager
    implements SigHandler {
    
    private static final String[] SIGNAL_IMPLEMENTATIONS = {
        
        "org.sqsh.signals.SunSignalCatcher",
        "org.sqsh.signals.NullSignalCatcher",
    };
    
    private static Stack<SigHandler> handlers = new Stack<SigHandler>();
    private static SignalManager instance = null;
    private static AbstractSignalCatcher handlerImpl = null;
    private static int signalCount = 0;
    
    private SignalManager() {
        
    }
    
    /**
     * Grabs ahold of the one-true signal handler.
     * 
     * @return A handle to the manager.
     */
    public static synchronized SignalManager getInstance() {
        
        if (instance == null) {
            
            instance = new SignalManager();
            
            boolean done = false;
            for (int i = 0; !done && i < SIGNAL_IMPLEMENTATIONS.length; i++) {
                
                try {
                    
                    Class sigClass = Class.forName(SIGNAL_IMPLEMENTATIONS[i]);
                    Constructor constructor = 
                         sigClass.getConstructor(SignalManager.class);
                    handlerImpl = (AbstractSignalCatcher)
                        constructor.newInstance(instance);
                    done = true;
                }
                catch (Exception e) {
                    
                    /* IGNORED */
                    e.printStackTrace();
                }
            }
        }
        
        return instance;
    }
    
    /**
     * Sets the signal handler. This handler will be the only one
     * to receive signals until it is popped off the stack, at 
     * which time the previous handler will become the active one.
     * 
     * @param handler The new handler.
     */
    public static synchronized void push(SigHandler handler) {
        
        signalCount = 0;
        handlers.push(handler);
    }
    
    /**
     * Removes the current signal handler, setting the previous handler
     * as the active one and returning the current handler.
     * 
     * @return The current handler.
     */
    public static synchronized SigHandler pop() {
        
        signalCount = 0;
        return handlers.pop();
    }
    
    /**
     * Used to acknowledge the receipt of the current signal.
     */
    public static synchronized void acknowledge() {
        
        signalCount = 0;
    }
    
    /**
     * Called by the VM when a signal is encountered.
     */
    public void signal (Sig sig) {
        
        ++signalCount;
        if (signalCount == 1) {
            
            SigHandler handler = handlers.peek();
            if (handler != null) {
                
                handler.signal(sig);
            }
            else {
                
                System.exit(1);
            }
        }
    }
}
