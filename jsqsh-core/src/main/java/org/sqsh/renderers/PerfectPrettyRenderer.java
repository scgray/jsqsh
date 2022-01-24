/*
 * Copyright 2007-2022 Scott C. Gray
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
    
    /**
     * For columns that allow resizing, this is the minimum size we will allow
     */
    private static final int MIN_RESIZE_SIZE = 10;
    
    /**
     * Minimum size we will shrink a column that resists resizing. For the most
     * part we expect numbers to be the ones that resist resizing, so we will
     * allow them to shrink a bit smaller because 10 would still be a big-ish 
     * number.
     */
    private static final int MIN_NORESIZE_SIZE = 4;
    
    /*
     * The data that has been collected thus far.
     */
    private List<String[]> rows = new ArrayList<String[]>();
    private int sampleSize = 0;
    private boolean hasHitSampleLimit = false;
    
    /**
     * Creates the renderer.
     *
     * @param session The owning session.
     * @param renderMan The owning manager.
     */
    public PerfectPrettyRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan, true);
        sampleSize = renderMan.getPerfectSampleSize();
    }

    protected PerfectPrettyRenderer(Session session, RendererManager renderMan,
           boolean hasOuterBorder) {

        super(session, renderMan, hasOuterBorder);
        sampleSize = renderMan.getPerfectSampleSize();
    }

    @Override
    public void header (ColumnDescription []columns) {
    
        super.header(columns);
        
        /*
         * We need to throw away the previous result set.
         */
        rows.clear();
        hasHitSampleLimit = false;
        
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
        
        if (hasHitSampleLimit) {
            
            printRow(row);
        }
        else {
            
            if (sampleSize > 0 && rows.size() >= sampleSize) {
                
                perfectWidth();
                printHeader();
                for (int i = 0; i < rows.size(); i++) {
            
                    if (session.out.checkError() || Thread.interrupted()) {
                
                        return false;
                    }
            
                    printRow(rows.get(i));
                }
                printRow(row);
                
                rows.clear();
                hasHitSampleLimit = true;
            }
            else {
        
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
            }
            
            rows.add(row);
        }
        
        return true;
    }
    
    /**
     * This method is called just prior to display and attempts to adjust
     * the width of specific columns so that they will try to stay within
     * the width of the screen overall.
     */
    private void perfectWidth() {

        /*
         * Attempt to grab the width of the console. If we can't get it,
         * then don't go any further.
         */
        int screenWidth = session.getScreenWidth();
        if (screenWidth <= 0) {
            
            return;
        }

        /*
         * If borders are enabled, then pre-add the space for the leading "| "
         * and the trailing " |".
         */
        int totalWidth = (hasOuterBorder ? 4 : 0);
        for (int i = 0; i < columns.length; i++) {

            /*
             * For all columns after the first, account for " | " before the
             * column value is printed.
             */
            if (i > 0) {

                totalWidth += 3;
            }

            /*
             * Add in the width of the column itself.
             */
            totalWidth += columns[i].getWidth();
        }

        /*
         * Pass #1: (this is not as efficient as it could be...)
         * 
         * Attempt to shrink all columns that are marked as resizeable, one
         * character at a time, but try not to shrink it smaller than the column
         * header or 10 characters. 
         */
        int nShrinks = 1;
        while (nShrinks > 0 && totalWidth > screenWidth) {
            
            nShrinks = 0;
            for (int i = 0; totalWidth > screenWidth
                && i < columns.length; i++) {
                
                ColumnDescription col = columns[i];
                
                // Can be resized
                if (col.isResizeable() 
                        // But not shorter than the column header
                        && (col.getName() == null 
                            || col.getWidth() > col.getName().length())
                        // And don't shrink below 10
                        && col.getWidth() > MIN_RESIZE_SIZE) {
                    
                    col.setWidth(col.getWidth() - 1);
                    --totalWidth;
                    ++nShrinks;
                }
            }
        }
        
        /*
         * Pass #2: 
         * 
         * Try again, but allow shrinking smaller than the column header length
         * if we have to.
         */
        nShrinks = 1;
        while (nShrinks > 0 && totalWidth > screenWidth) {
            
            nShrinks = 0;
            for (int i = 0; totalWidth > screenWidth
                && i < columns.length; i++) {
                
                ColumnDescription col = columns[i];
                
                if (col.isResizeable() && col.getWidth() > MIN_RESIZE_SIZE) {
                    
                    col.setWidth(col.getWidth() - 1);
                    --totalWidth;
                    ++nShrinks;
                }
            }
        }
        
        /*
         * Pass #3: 
         * 
         * Ok, we have shrunk all of the resizeable columns, now we have to
         * stop honoring the ones that requested no resizing.
         */
        nShrinks = 1;
        while (nShrinks > 0 && totalWidth > screenWidth) {
            
            nShrinks = 0;
            for (int i = 0; totalWidth > screenWidth
                && i < columns.length; i++) {
                
                ColumnDescription col = columns[i];
                
                if (!col.isResizeable() && col.getWidth() > MIN_NORESIZE_SIZE) {
                    
                    col.setWidth(col.getWidth() - 1);
                    --totalWidth;
                    ++nShrinks;
                }
            }
        }
        
        /*
         * There are no more passes now. If we didn't shrink enough...well,
         * we're just S.O.L.
         */
    }
    
    @Override
    public boolean flush () {
        
        if (!hasHitSampleLimit) {
            
            perfectWidth();
            
            printHeader();
            for (int i = 0; i < rows.size(); i++) {
                
                if (session.out.checkError() || Thread.interrupted()) {
                    
                    return false;
                }
                
                printRow(rows.get(i));
            }
        }
        printFooter();
        
        return true;
    }
}
