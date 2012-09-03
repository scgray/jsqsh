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

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * An implementation of a signal handler that utilizes the 
 * sun.misc.SignalHandler interface.
 */
public class SunSignalCatcher
    extends AbstractSignalCatcher
    implements SignalHandler {
    
    /**
     * Creates a signal handler for handling Sun JVM signals.
     * 
     * @param manager The manager that created us.
     */
    public SunSignalCatcher(SignalManager manager) {
        
        super(manager);
        Signal sig = new Signal("INT");
        Signal.handle(sig, this);
    }
    
    /**
     * Called by the VM when a signal is encountered.
     */
    public void handle (Signal sig) {
        
        Sig signal = new Sig(sig.getName());
        getManager().signal(signal);
    }
}
