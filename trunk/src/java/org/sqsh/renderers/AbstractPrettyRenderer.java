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
import org.sqsh.LineIterator;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;

public abstract class AbstractPrettyRenderer
    extends Renderer {
    
    /**
     * To save a bajillion object creations we create a set of line iterators
     * that we will re-use throughout the rendering process.
     */
    private LineIterator []iterators = null;
    
    public AbstractPrettyRenderer(Session session, RendererManager renderMan,
            ColumnDescription []columns) {
        
        super(session, renderMan, columns);
    }
    
    /**
     * Helper method to print the header row.
     */
    public void printHeader() {
        
        /*
         * Just abort if we aren't supposed to be showing headers.
         */
        if (manager.isShowHeaders() == false) {
            
            return;
        }
        
        printHorizontalLine();
        
        session.out.print("| ");
        for (int i = 0; i < columns.length; i++) {
            
            if (i > 0) {
                
                session.out.print(" | ");
            }
            
            String columnName = columns[i].getName();
            if (columnName == null) {
                
                columnName = "";
            }
            
            printColumnValue(columns[i], columnName);
        }
        
        session.out.println(" |");
        
        printHorizontalLine();
    }
    
    /**
     * Displays a row of data.
     * @param row The row to display.
     */
    public void printRow(String []row) {
        
        if (iterators == null) {
            
            iterators = new LineIterator[columns.length];
            for (int i = 0; i < columns.length; i++) {
                
                iterators[i] = getLineIterator(columns[i], row[i]);
            }
        }
        else {
            
            for (int i = 0; i < columns.length; i++) {
                
                iterators[i].reset(row[i]);
            }
        }
        
        boolean done = false;
        while (!done) {
            
            session.out.print("| ");
            
            done = true;
            for (int i = 0; i < columns.length; i++) {
                
                if (i > 0) {
                    
                    session.out.print(" | ");
                }
                
                LineIterator iter = iterators[i];
                if (iter.hasNext()) {
                    
                    
                    printColumnValue(columns[i], iter.next());
                    if (iter.hasNext()) {
                        
                        done = false;
                    }
                }
                else {
                    
                    printColumnValue(columns[i], " ");
                }
            }
            
            session.out.println(" |");
        }
    }
    
    public void printFooter() {
        
        printHorizontalLine();
    }
    
    /**
     * Displays a horizontal line 
     */
    public void printHorizontalLine() {
        
        session.out.print("+-");
        for (int i = 0; i < columns.length; i++) {
            
            if (i > 0) {
                
                session.out.print("-+-");
            }
            
            ColumnDescription col = columns[i];
            int width = col.getWidth();
            for (int j = 0; j < width; j++) {
                
                session.out.print('-');
            }
        }
        session.out.println("-+");
    }
}
