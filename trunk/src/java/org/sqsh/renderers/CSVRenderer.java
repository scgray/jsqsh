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
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * Outputs results as CSV (comma separated values). This class attempts to
 * adhere to <a href="http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm">
 * http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm</a>.
 */
public class CSVRenderer
    extends Renderer {
    
    private StringBuilder line = new StringBuilder(); 
    
    public CSVRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan);
    }
    
    public void header (ColumnDescription []columns) {
        
        super.header(columns);
        
        if (manager.isShowHeaders()) {
            
            String []row = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                
                row[i] = columns[i].getName();
                if (row[i] == null) {
                    
                    row[i] = "";
                }
            }
            
            row(row);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean flush () {

        return true;
    }
    
    private boolean needsQuoting(String str) {
        
        final int sz = str.length();
        
        if (sz == 0) {
            
            return true;
        }
        
        if (Character.isWhitespace(str.charAt(0))
            || Character.isWhitespace(str.charAt(sz -1))) {
            
            return true;
        }
        
        for (int i = 0; i < sz; i++) {
            
            char ch = str.charAt(i);
            if (ch == '"' || ch == '\n' || ch == ',') {
                
                return true;
            }
        }
        
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean row (String[] row) {

        line.setLength(0);
        for (int i = 0; i < row.length; i++) {
            
            if (i > 0) {
                
                line.append(',');
            }
            
            String field = row[i];
            
            if (!isNull(field)) {
                
                if (!needsQuoting(field)) {
                    
                    line.append(field);
                }
                else {
                    
                    line.append('"');
                    for (int j = 0; j < field.length(); j++) {
                        
                        char ch = field.charAt(j);
                        if (ch == '"') {
                            
                            line.append('"');
                        }
                        
                        line.append(ch);
                    }
                    
                    line.append('"');
                }
            }
        }
        
        session.out.println(line);
        return (session.out.checkError() == false);
    }
    
    @Override
    public void footer (String footer) {
        
        /*
         * This style will never display footer information.
         */
    }
}
