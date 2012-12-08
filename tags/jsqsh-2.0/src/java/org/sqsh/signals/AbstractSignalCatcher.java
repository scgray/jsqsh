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

/**
 * All signal handlers must extend this class.
 */
public class AbstractSignalCatcher {
    
    private SignalManager manager;
    
    /**
     * Creates a signal handler for handling Sun JVM signals.
     * 
     * @param manager The manager that created us.
     */
    public AbstractSignalCatcher(SignalManager manager) {
        
        this.manager = manager;
    }
    
    /**
     * Returns the manager of this handler.
     * 
     * @return The manager of this handler.
     */
    public SignalManager getManager() {
        
        return manager;
    }
}
