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

import static org.sqsh.options.ArgumentRequired.NONE;

import java.util.ArrayList;
import java.util.List;

import org.sqsh.Buffer;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.TruncatingLineIterator;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

/**
 * Implements the \history command.
 */
public class History
    extends Command {
    
    private static class Options
        extends SqshOptions {
        
        @OptionProperty(
            option='a', longOption="all", arg=NONE,
            description="Show the entire text of all statements")
            public boolean showAll = false;
        
        @Argv(program="\\history", min=0, max=0, usage="[-a]")
        public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }
    
    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options)opts;
        String lineSep = System.getProperty("line.separator");
        BufferManager bufMan = session.getBufferManager();
        
        /*
         * Note that we skip buffer 0 because it is the current
         * buffer.
         */
        Buffer []buffers = bufMan.getBuffers();
        for (int i = 0; i < (buffers.length - 1); i++) {
            
            Buffer buf = buffers[i];
            StringBuilder sb = new StringBuilder();
            sb.append('(').append(buf.getId()).append(") ");
            
            int indent = sb.length();
            int line = 0;
            
            TruncatingLineIterator iter = new TruncatingLineIterator(
                buf.toString(), -1);
            while (iter.hasNext()) {
                
                ++line;
                if (line > 1) {
                    
                    for (int j = 0; j < indent; j++) {
                        
                        sb.append(' ');
                    }
                }
                
                if (options.showAll == false && line > 10) {
                    
                    sb.append("...");
                    sb.append(lineSep);
                    break;
                }
                else {
                
                    sb.append(iter.next());
                    sb.append(lineSep);
                }
            }
            
            if (line == 0)
                sb.append(lineSep);
            
            session.out.print(sb.toString());
        }
        
        return 0;
    }
}
