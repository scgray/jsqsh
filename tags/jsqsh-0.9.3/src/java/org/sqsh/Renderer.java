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
package org.sqsh;

import org.sqsh.ColumnDescription.OverflowBehavior;


/**
 * A renderer is a class that is responsible for translating data (row and
 * columns) into a formatted output.
 */
public abstract class Renderer {
    
    /**
     * Session that owns the renderer.
     */
    protected Session session;
    
    /**
     * Formatting description for the columns that are to be displayed.
     */
    protected ColumnDescription []columns;
    
    /**
     * The parent manager that created this renderer. This provides
     * settings and hints as to how the renderer should display its output.
     */
    protected RendererManager manager;
    
    /**
     * Creates a renderer.
     * 
     * @param session The session for the renderer
     * @param manager The manager for this renderer
     * @param columns Description of the columns to be rendered.
     */
    public Renderer (Session session, RendererManager manager, 
            ColumnDescription []columns) {
        
        this.session = session;
        this.manager = manager;
        this.columns = columns;
    }
    
    /**
     * This is a helper method to display a string representing a 
     * single line of text to the session's output stream. This value
     * will be properly aligned as per the column's alignment rules 
     * and will be padded so that exactly {@link ColumnDescription#getWidth()}
     * characters are shown.
     * 
     * @param column The column that is being displayed.
     * @param str A single line of text (no new-lines!)
     */
    public void printColumnValue(ColumnDescription column,
            String str) {
        
        int padding = column.getWidth() - str.length();
        
        if (column.getAlignment() == ColumnDescription.Alignment.RIGHT) {
            
            for (int i = 0; i < padding; i++) {
                
                session.out.print(' ');
            }
        }
        
        session.out.print(str);
        
        if (column.getAlignment() == ColumnDescription.Alignment.LEFT) {
            
            for (int i = 0; i < padding; i++) {
                
                session.out.print(' ');
            }
        }
    }
    
    /**
     * This is a helper method for renderers to create a {@link LineIterator}
     * based upon the type of overflow behavior that is defined for the
     * column.
     * 
     * @param column The column that will need to be processed.
     * @param str The string to be iterated.
     * @return The newly created iterator.
     */
    public LineIterator getLineIterator(ColumnDescription column, String str) {
        
        /*
         * We'll use a truncating iterator if explicitly requested or 
         * unless the width doesn't specify a maximum size.
         */
        if (column.getOverflowBehavior() == OverflowBehavior.TRUNCATE
                || column.getWidth() <= 0) {
            
            return new TruncatingLineIterator(str, column.getWidth());
        }
        else {
            
            return new WordWrapLineIterator(str, column.getWidth());
        }
    }
    
    /**
     * This is another helper method for renderers to blast through a 
     * string that may contain multiple lines of text and to determine
     * the length of the longest line according to the overflow behavior
     * of the column.
     * 
     * @param str The string to test.
     * @return The longest line length.
     */
    public int getMaxLineWidth(ColumnDescription column, String str) {
        
        LineIterator iter =  new TruncatingLineIterator(str, -1);
        int maxWidth = 0;
        
        while (iter.hasNext()) {
            
            int len = iter.next().length();
            if (len > maxWidth) {
                
                maxWidth = len;
            }
        }
        
        return maxWidth;
    }
    
    /**
     * Processes a row of data. Note that it is not necessary for a rendered
     * to display the row at this point. Some renderers may choose to 
     * buffer their data until it is completed before display.
     * 
     * @param row The row of data to be displayed.
     * 
     * @return true if the row was successfully added to the rendered, false
     *   indicates that the renderer cannot accept any more rows. This is
     *   typically used in the case where the output stream for the renderer
     *   is attached to another process and that process has gone away. After
     *   receiving a false, it is expected that the caller should not call
     *   row() any more, and should jump straight to flush().
     */
    public abstract boolean row (String []row);
    
    /**
     * Called when all of the available data has been passed to the
     * renderer.
     * 
     * @return true if the flush was successful. A false indicates that the
     *   final destination for the data has gone away and the results 
     *   could not be flushed.
     */
    public abstract boolean flush();
}
