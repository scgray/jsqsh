/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the debug command.
 */
public class Debug
    extends Command {
    
    private static class Options
        extends SqshOptions {
    
        @Argv(program="\\debug", min=1, max=1)
        public List<String> arguments = new ArrayList<String>();
    }

    /**
     * Return our overridden options.
     */
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options)opts;
        boolean ok = true;

        for (int i = 0; i < options.arguments.size(); i++) {
            
            String name = options.arguments.get(i);
            Logger log = Logger.getLogger(name);
            
            if (log != null) {
                
                log.setLevel(Level.FINE);
            }
            else {
                
                session.err.println("Unable to find logger '"
                    + name + "'");
                ok = false;
            }
        }
        
        return (ok ? 0 : 1);
    }
}
