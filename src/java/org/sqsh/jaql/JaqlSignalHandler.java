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
