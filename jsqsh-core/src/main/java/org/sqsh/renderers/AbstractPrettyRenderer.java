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

public abstract class AbstractPrettyRenderer
    extends Renderer {
    
    /**
     * To save a bajillion object creations we create a set of line iterators
     * that we will re-use throughout the rendering process.
     */
    private LineIterator []iterators = null;
    protected boolean hasOuterBorder;
    
    public AbstractPrettyRenderer(Session session, RendererManager renderMan,
            boolean hasOuterBorder) {
        
        super(session, renderMan);
        this.hasOuterBorder = hasOuterBorder;
    }
    
    /** {@inheritDoc} */
    public void header (ColumnDescription[] columns) {
        
        super.header(columns);
        
        /*
         * Throw away our column iterators since we have a new result set
         * comine.
         */
        iterators = null;
    }
    
    /**
     * Helper method to print the header row.
     */
    public void printHeader() {
        
        if (hasOuterBorder) {

            printHorizontalLine();
        }
        else {

            session.out.println();
        }

        if (!manager.isShowHeaders()) {

            return;
        }

        String []names = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            
            names[i] = columns[i].getName();
            if (names[i] == null) {
                
                names[i] = "";
            }
        }
        printRow(names);
        
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

            if (hasOuterBorder) {

                session.out.print("| ");
            }

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

            if (hasOuterBorder) {

                session.out.println(" |");
            }
            else {

                session.out.println();
            }
        }
    }
    
    public void printFooter() {

        if (hasOuterBorder) {

            printHorizontalLine();
        }
        else {

            session.out.println();
        }
    }
    
    /**
     * Displays a horizontal line 
     */
    public void printHorizontalLine() {

        if (hasOuterBorder) {

            session.out.print("+-");
        }

        for (int i = 0; i < columns.length; i++) {
            
            if (i > 0) {
                
                session.out.print("-+-");
            }
            
            ColumnDescription col = columns[i];
            int width = col.getWidth();
            
            dashes(width);
        }

        if (hasOuterBorder) {

            session.out.println("-+");
        }
        else {

            session.out.println();
        }
    }
}
