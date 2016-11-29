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
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * This renderer is very similar to the PerfectPrettyRenderer except that
 * it makes no attempt to create columns of perfect width. Instead the columns
 * are made wide enough to hold the entire value encountered.
 */
public class PrettyRenderer
    extends AbstractPrettyRenderer {
    
    /**
     * Creates the renderer.
     *
     * @param session The ownning session.
     * @param renderMan The owning manager.
     */
    public PrettyRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan, true);
    }

    protected PrettyRenderer(Session session, RendererManager renderMan,
            boolean hasOuterBorder) {

        super(session, renderMan, hasOuterBorder);
    }

    public void header (ColumnDescription []columns) {
        
        super.header(columns);
        
        /*
         * The columns as provided by the caller should already be wide 
         * enough for the values that could be coming from them, however
         * we can have the case where a column is, more or less, unbounded.
         * In that case, the width will be a negative value. In this case
         * we will force the width to be the renderers maximum column width.
         */
        for (int i = 0; i < columns.length; i++) {
            
            ColumnDescription col = columns[i];
            if (col.getWidth() <= 0 
                    || col.getWidth() > manager.getMaxColumnWidth()) {
                
                col.setWidth(manager.getMaxColumnWidth());
            }
            
            /*
             * If the column isn't wide enough for the column
             * name, then bump it out a bit wider.
             */
            if (col.getName() != null) {
                
                if (col.getName().length() > col.getWidth()) {
                    
                    col.setWidth(col.getName().length());
                }
            }
        }
        
        printHeader();
    }
    
    /**
     * Called when a row needs to be displayed. This renderer will 
     * immediately display the row.
     * 
     * @param row The row to be displayed
     * 
     * @return false if the row cannot be rendered because the output
     *    stream has been closed.
     */
    public boolean row (String[] row) {

        if (session.out.checkError())  {
            
            return false;
        }
        
        printRow(row);
        return true;
    }

    @Override
    public boolean flush () {

        if (session.out.checkError())  {
            
            return false;
        }
        
        printFooter();
        return true;
    }
}
