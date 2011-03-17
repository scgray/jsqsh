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

public class ISQLRenderer
    extends Renderer {
    
    private int screenWidth;
    
    public ISQLRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan);
        
        /*
         * We use this a lot so fetch it once.
         */
        screenWidth = session.getShellManager().getConsoleWidth();
    }
    
    /** {@inheritDoc} */
    public void header (ColumnDescription[] columns) {
        
        super.header(columns);
        int totalWidth = 0;
        
        /*
         * Print the column names.
         */
        for (int i = 0; i < columns.length; i++) {
            
            /* 
             * isql caps the display width of the column header at 512.
             */
            if (columns[i].getWidth() > 512)
                columns[i].setWidth(512);
            
            if (i > 0 
                &&  (totalWidth + columns[i].getWidth() + 1) > screenWidth) {
                
                session.out.println();
                session.out.print(" \t");
                totalWidth = 9;
            }
            else {
                
                session.out.print(' ');
                ++totalWidth;
            }
            
            printColumnName(columns[i], columns[i].getName());
            totalWidth += columns[i].getWidth();
        }
        session.out.println();
        
        /*
         * Now our dashes.
         */
        for (int i = 0; i < columns.length; i++) {
            
            if (i > 0 
                &&  (totalWidth + columns[i].getWidth() + 1) > screenWidth) {
                
                session.out.println();
                session.out.print(" \t");
                totalWidth = 9;
            }
            else {
                
                session.out.print(' ');
                ++totalWidth;
            }
            
            for (int j = 0; j < columns[i].getWidth(); j++) {
                
                session.out.print('-');
            }
            
            totalWidth += columns[i].getWidth();
        }
        
        session.out.println();
    }
    
    /** {@inheritDoc} */
    public boolean row (String[] row) {
        
        int totalWidth = 0;
        
        for (int i = 0; i < columns.length; i++) {
            
            if (i > 0 
                &&  (totalWidth + columns[i].getWidth() + 1) > screenWidth) {
                
                session.out.println();
                session.out.print(" \t");
                totalWidth = 9;
            }
            else {
                
                session.out.print(' ');
                ++totalWidth;
            }
            
            printColumnValue(columns[i], row[i]);
            totalWidth += columns[i].getWidth();
        }
        session.out.println();
        
        return true;
    }
    
    @Override
    public boolean flush () {
        
        session.out.println();
        return true;
    }
}
