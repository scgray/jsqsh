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
package org.sqsh.renderers;

import org.sqsh.ColumnDescription;
import org.sqsh.LineIterator;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;
import org.sqsh.WordWrapLineIterator;

public class VerticalRenderer
    extends Renderer {
    
    private int screenWidth;
    private int maxColumnNameWidth = 0;
    private WordWrapLineIterator lineIter = null;
    private boolean valueOnNewLine = false;
    
    public VerticalRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan);
        
        /*
         * We use this a lot so fetch it once.
         */
        screenWidth = session.getShellManager().getConsoleWidth();
    }
    
    /** {@inheritDoc} */
    public void header (ColumnDescription[] columns) {
        
        super.header(columns);
        
        /*
         * Print the column names.
         */
        for (int i = 0; i < columns.length; i++) {
            
            int nameLength = columns[i].getName().length();
            if (nameLength > maxColumnNameWidth)
                maxColumnNameWidth = nameLength;
        }
        
        int valueWidth = screenWidth - (maxColumnNameWidth + 2);
        if (valueWidth <= 0) {
            
            valueOnNewLine = true;
            lineIter = new WordWrapLineIterator("", screenWidth - 4);
        }
        else {
            
            valueOnNewLine = false;
            lineIter = new WordWrapLineIterator("", valueWidth);
        }
    }
    
    /** {@inheritDoc} */
    public boolean row (String[] row) {
        
        for (int i = 0; i < columns.length; i++) {
            
            String colName = columns[i].getName();
            int padding    = maxColumnNameWidth - colName.length();
            
            session.out.print(colName);
            session.out.print(": ");
            
            for (int j = 0; j < padding; j++) {
                
                session.out.print(' ');
            }
            
            if (valueOnNewLine) {
                
                session.out.println();
                session.out.print("    ");
            }
            
            lineIter.reset(row[i]);
            int segment = 0;
            while (lineIter.hasNext()) {
                
                String line = lineIter.next();
                if (segment > 0) {
                    
                    if (valueOnNewLine)
                        session.out.print("    ");
                    else {
                        
                        for (int j = 0; j < maxColumnNameWidth + 2; j++) {
                            
                            session.out.print(' ');
                        }
                    }
                }
                
                session.out.println(line);
                ++segment;
            }
        }
        session.out.println();
        
        return true;
    }
    
    @Override
    public boolean flush () {
        
        return true;
    }
}
