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
    
    private static SignalManager instance = null;
    private static Object lock = new Object();
    
    private Stack<SigHandler> handlers = new Stack<SigHandler>();
    private int signalCount = 0;
    private AbstractSignalCatcher handlerImpl = null;
    
    private SignalManager() {
        
    }
    
    /**
     * Grabs a hold of the one-true signal handler.
     * 
     * @return A handle to the manager.
     */
    public static SignalManager getInstance() {
        
        if (instance == null) {
            
            synchronized (lock) {
                
                if (instance == null) {
            
                    for (int i = 0; i < SIGNAL_IMPLEMENTATIONS.length; i++) {
                        
                        try {
                            
                            Class<? extends AbstractSignalCatcher> sigClass = 
                                Class.forName(SIGNAL_IMPLEMENTATIONS[i]).asSubclass(
                                    AbstractSignalCatcher.class);
                            
                            Constructor<? extends AbstractSignalCatcher> constructor = 
                                 sigClass.getConstructor(SignalManager.class);
                            
                            instance = new SignalManager();
                            instance.handlerImpl = constructor.newInstance(instance);
                            break;
                        }
                        catch (Exception e) {
                            
                            /* IGNORED */
                        }
                    }
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
    public synchronized void push(SigHandler handler) {
        
        signalCount = 0;
        handlers.push(handler);
    }
    
    /**
     * Removes the current signal handler, setting the previous handler
     * as the active one and returning the current handler.
     * 
     * @return The current handler.
     */
    public synchronized SigHandler pop() {
        
        signalCount = 0;
        return handlers.pop();
    }
    
    /**
     * Used to acknowledge the receipt of the current signal.
     */
    public synchronized void acknowledge() {
        
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
