/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
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
     * @param renderMan The owning manager.
     * @param columns The columns. The width specified by the column's
     *    {@link ColumnDescription#getWidth()} will be ignored and calculated
     *    prior to displaying the results.
     */
    public PrettyRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan);
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
