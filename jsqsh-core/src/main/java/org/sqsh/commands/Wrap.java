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

import org.sqsh.Buffer;
import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.WordWrapLineIterator;
import org.sqsh.options.Argv;

/**
 * Command used to test the word-wrapping process.
 */
public class Wrap
    extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Argv(program="\\wrap", min=0, max=1, usage="[width]")
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
        
        int width = 40;
        if (options.arguments.size() > 0) {
            
            width = Integer.parseInt(options.arguments.get(0));
        }
        
        BufferManager bufMan = session.getBufferManager();
        
        /*
         * Fetch the current buffer and create a new one.
         */
        Buffer buf = bufMan.getCurrent();
        bufMan.newBuffer();
        
        WordWrapLineIterator iter = new WordWrapLineIterator(buf.toString(), width);
        while (iter.hasNext()) {
            
            String str = (String) iter.next();
            session.out.print("|");
            session.out.print(str);
            for (int i = str.length(); i < width; i++) {
                
                session.out.print(' ');
            }
            
            session.out.println("|");
        }
        
        return 0;
    }

}
