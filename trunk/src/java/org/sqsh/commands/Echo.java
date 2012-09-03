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
package org.sqsh.commands;

import static org.sqsh.options.ArgumentRequired.NONE;

import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

/**
 * Implements the 'echo' command in sqsh.
 */
public class Echo
    extends Command {
    
     /*
      * Used to contain the command line options that were passed in by
      * the caller.
      */
    private static class Options
       extends SqshOptions {
        
       @OptionProperty(
            option='n', longOption="no-newline", arg=NONE,
            description="Do not append a trailing new-line")
       public boolean noNewline = false;
       
       @Argv(program="\\echo", usage="[text ...]")
       public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }
    
    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        for (int i = 0; i < options.arguments.size(); i++) {
            
            if (i > 0) {
                
                session.out.print(' ');
            }
            
            session.out.print(options.arguments.get(i));
        }
        
        if (options.noNewline == false) {
            
            session.out.println();
        }
        
        return 0;
    }
}
