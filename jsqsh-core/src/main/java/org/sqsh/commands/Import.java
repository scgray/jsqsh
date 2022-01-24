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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.Extension;
import org.sqsh.ExtensionException;
import org.sqsh.Renderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.ColumnDescription.Alignment;
import org.sqsh.ColumnDescription.OverflowBehavior;
import org.sqsh.options.Argv;

public class Import extends Command {
    
    /**
     * Used to contain the command line options that were passed in by
     * the caller.
     */
    private static class Options
        extends SqshOptions {
        
        @Argv(program="\\import", min=0, max=1,
            usage="package")
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
        
        int nArgs = options.arguments.size();
        
        if (nArgs > 1) {
            
            session.err.println("use: \\import [package]");
            return 1;
        }
        
        if (nArgs == 0) {
            
            doDisplayImports(session);
            return 0;
        }
        
        String packageName = options.arguments.get(0);
        try {
            
            session.getContext().getExtensionManager().load(packageName);
        }
        catch (ExtensionException e) {
            
            session.err.println("Failed to load extension \"" + packageName + "\": "
                + e.getMessage());
            session.setException(e);
            return 1;
        }
        
        return 0;
    }
    
    private void doDisplayImports (Session session) {
        
        Extension extensions[] = session.getContext().getExtensionManager().getExtensions();
        
        Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
        
        ColumnDescription []columns = new ColumnDescription[5];
        columns[0] = new ColumnDescription("Name", -1, 
                Alignment.LEFT, OverflowBehavior.WRAP, false);
        columns[1] = new ColumnDescription("Directory", -1, 
                Alignment.LEFT, OverflowBehavior.WRAP, false);
        columns[2] = new ColumnDescription("Loaded", -1, 
                Alignment.LEFT, OverflowBehavior.WRAP, false);
        columns[3] = new ColumnDescription("Disabled", -1, 
                Alignment.LEFT, OverflowBehavior.WRAP, false);
        columns[4] = new ColumnDescription("Classpath", -1);
        renderer.header(columns);
        
        String row[] = new String[5];
        StringBuilder sb = new StringBuilder();
        for (Extension e : extensions) {
            
            row[0] = e.getName();
            row[1] = e.getDirectory();
            row[2] = Boolean.toString(e.isLoaded());
            row[3] = Boolean.toString(e.isDisabled());
            
            sb.setLength(0);
            if (e.getClasspath() != null) {

                for (String str : e.getClasspath()) {
                    
                    if (sb.length() > 0) 
                        sb.append(File.pathSeparator);
                    sb.append(str);
                }
            }
            
            row[4] = sb.toString();
            renderer.row(row);
        }
        
        renderer.flush();
    }
}
