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

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshContextSwitchMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;

/**
 * Implements the \end command to end the current session and switch to
 * another session.
 */
public class End
    extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Argv(program="\\end", min=0, max=1, usage="[next-session]")
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
        
        Options options = (Options) opts;
        
        int targetSession = -1;
        Session target = null;
        
        if (options.arguments.size() > 1) {
            
            session.err.println("Use: \\end [next-session]");
            return 1;
        }
        
        if (options.arguments.size() == 1) {
            
            try {
                
                targetSession = Integer.parseInt(options.arguments.get(0));
            }
            catch (NumberFormatException e) {
                
                session.err.println("Invalid session id '" 
                    + options.arguments.get(0) + "'");
                return 1;
            }
            
            target = session.getContext().getSession(targetSession);
            if (target == null) {
                
                session.err.println("Specified next session id '"
                    + options.arguments.get(0) + "'" + " does not exist");
                return 1;
            }
        }
        
        throw new SqshContextSwitchMessage(session, target, true);
    }
}
