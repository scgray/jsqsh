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

import java.util.ArrayList;
import java.util.List;

import org.sqsh.ColumnDescription;
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * Implements a renderer that is similar to mysql's or henplus's in that it
 * will attempt to analyze all of the available data to perfectly display
 * all columns without wasting too much space.
 */
public class PerfectPrettyRenderer
    extends AbstractPrettyRenderer {
    
    /*
     * The data that has been collected thus far.
     */
    private List<String[]> rows = new ArrayList<String[]>();
    
    /**
     * Creates the renderer.
     * 
     * @param renderMan The owning manager.
     * @param columns The columns. The width specified by the column's
     *    {@link ColumnDescription#getWidth()} will be ignored and calculated
     *    prior to displaying the results.
     */
    public PerfectPrettyRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan);
    }
    
    @Override
    public void header (ColumnDescription []columns) {
    
        super.header(columns);
        
        /*
         * We need to throw away the previous result set.
         */
        rows.clear();
        
        for (int i = 0; i < columns.length; i++) {
            
            ColumnDescription col = columns[i];
            if (col.getName() != null) {
                
                col.setWidth(col.getName().length());
            }
            else {
                
                col.setWidth(1);
            }
        }
    }
    
    @Override
    public boolean row (String[] row) {
        
        for (int colIdx = 0; colIdx < columns.length; ++colIdx) {
            
            ColumnDescription col = columns[colIdx];
            int width = col.getWidth();
            
            if (row[colIdx] == null) {
                
                row[colIdx] = session.getDataFormatter().getNull();
                if (width < row[colIdx].length()) {
                    
                    width = row[colIdx].length();
                }
            }
            else {
            
                int lineWidth = getMaxLineWidth(col, row[colIdx]);
                if (lineWidth > width) {
                    
                    width = lineWidth;
                }
            }
            
            col.setWidth(width);
        }
        
        rows.add(row);
        return true;
    }
    
    @Override
    public boolean flush () {
        
        printHeader();
        for (int i = 0; i < rows.size(); i++) {
            
            if (session.out.checkError() || Thread.interrupted()) {
                
                return false;
            }
            
            printRow(rows.get(i));
        }
        printFooter();
        
        return true;
    }
}
