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
    protected ColumnDescription []columns = null;
    
    /**
     * The parent manager that created this renderer. This provides
     * settings and hints as to how the renderer should display its output.
     */
    protected RendererManager manager;
    
    /**
     * This is a little bit evil--some of the display styles need to 
     * know when a column contains an actual NULL value. Right now
     * there is no elegant way for a renderer to know this. To get
     * around this, the renderer can ask itself {@link #isNull(String)},
     * and the value  provided will be compared to the current 
     * representation of NULL according to the {@link DataFormatter}.
     * 
     * <p>At some point I may need to pass a lot of meta-data with each
     * row, but for now I have this hack.
     */
    private String nullRepresentation;
    
    /**
     * Creates a renderer.
     * 
     * @param session The session for the renderer
     * @param manager The manager for this renderer
     */
    public Renderer (Session session, RendererManager manager) {
        
        this.session = session;
        this.manager = manager;
        
        this.nullRepresentation = session.getDataFormatter().getNull();
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
    protected void printColumnValue(ColumnDescription column,
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
     * This is a helper method to display the name of a column (or a portion
     * of the name). This is the same as printColumnValue() except that
     * the alignment will always be left.
     * 
     * @param column The column that is being displayed.
     * @param str A single line of text (no new-lines!)
     */
    protected void printColumnName(ColumnDescription column, String str) {
        
        int padding = column.getWidth() - str.length();
        session.out.print(str);
        for (int i = 0; i < padding; i++) {
                
            session.out.print(' ');
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
    protected LineIterator getLineIterator(ColumnDescription column, String str) {
        
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
    protected int getMaxLineWidth(ColumnDescription column, String str) {
        
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
     * This is a convenience method that a renderer can call with a 
     * row value to determine if it contains a NULL value.
     * 
     * @param value The value in the row.
     * @return true if it appears to be a null.
     */
    protected boolean isNull(String value) {
        
        return (value == null || nullRepresentation.equals(value));
        
    }
    
    /**
     * This method is called before {@link #row(String[])} to describe
     * the result set that is about to come. 
     * 
     * @param columns Description of the columns to be displayed
     */
    public void header (ColumnDescription []columns) {
        
        this.columns = columns;
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
     * Called when all of the available data for the current result set
     * has been passed to the renderer.
     * 
     * @return true if the flush was successful. A false indicates that the
     *   final destination for the data has gone away and the results 
     *   could not be flushed.
     */
    public abstract boolean flush();
    
    /**
     * Called to display the footer string. The default implementation
     * will only display the footer string if 
     * {@link RendererManager#isShowFooters()} is true.
     * 
     * <p>As a note, this current implementation is very silly. The "right"
     * way to do this is to pass an object that represents metrics about the
     * results and allow the renderer to format it however it feels fit. I
     * may do this later if the mood strikes me.
     * 
     * @param footer The footer string.
     */
    public void footer (String footer) {
        
        if (manager.isShowFooters()) {
            
            session.out.println(footer);
        }
    }
}
