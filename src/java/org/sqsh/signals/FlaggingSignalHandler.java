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

public class FlaggingSignalHandler
    implements SigHandler {
    
    protected volatile boolean triggered = false;
    
    public FlaggingSignalHandler () {
        
    }
    
    /**
     * Tests whether or not the signal was triggered. If it was, then
     * the triggered flag is reset.
     * 
     * @return whether or not the handler was triggered.
     */
    public boolean isTriggered() {
        
        boolean b = triggered;
        triggered = false;
        
        return b;
    }
    
    public void clear() {
        
        triggered = false;
    }

    public void signal (Sig sig) {
        
        System.err.print("[Interrupted. Hit enter to continue...]");
        System.err.flush();
        
        triggered = true;
    }

}
