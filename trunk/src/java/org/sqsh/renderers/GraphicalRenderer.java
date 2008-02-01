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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * The GraphicalRenderer displays row results using a swing graphical
 * interface.
 * 
 * <p>WARNING: I know NOTHING about developing GUI's or swing development
 * so this renderer is pretty simplistic and feature poor. Please feel
 * free to submit fancy enhancements...namely, I'd love to be able to have
 * it dynamically receive rows from the query so I can display the window
 * before all of the rows have been received. I know this can be done, I 
 * just don't know how.</p>
 */
public class GraphicalRenderer
    extends Renderer {
    
    private ColumnDescription []columns;
    private List<String[]> rows = new ArrayList<String[]>();
    
    /**
     * Creates the renderer.
     * 
     * @param session The session that needs renderin'
     * @param manager The manager that owns this renderer.
     */
    public GraphicalRenderer(Session session, RendererManager manager) {

        super(session, manager);
    }
    
    /**
     * This method is called at the beginning of a result set. It causes
     * the creation of a new window that will be fed data as each row()
     * is added.
     * 
     * @param columns The columns of the rows that are to be expected.
     */
    @Override
    public void header (ColumnDescription[] columns) {

        this.columns = columns;
        rows.clear();
    }
    
    /**
     * Adds a row to the panel.
     * 
     * @param row The row to add.
     */
    @Override
    public boolean row (String[] row) {

        rows.add(row);
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.sqsh.Renderer#footer(java.lang.String)
     */
    @Override
    public void footer (String footer) {
        
        if (columns == null) {
            
            return;
        }
        
        String [][]data = rows.toArray(new String[0][]);
        String []columnNames = new String[columns.length];
        
        for (int i = 0; i < columns.length; i++) {
            
            columnNames[i] = columns[i].getName();
        }
        
        JFrame frame = new JFrame();
        
        // Set the frame characteristics
        frame.setTitle("Query Results");
        frame.setSize(500, 250);
        frame.setLocationByPlatform(true);

        // Create a panel to hold all other components
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        frame.getContentPane().add(topPanel);

        // Create a new table instance
        JTable table = new JTable(data, columnNames);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Add the table to a scrolling pane
        JScrollPane scrollPane = new JScrollPane(table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        topPanel.add( scrollPane, BorderLayout.CENTER );
        
        JLabel footerText = new JLabel(footer);
        topPanel.add(footerText, BorderLayout.SOUTH);
        
        frame.setVisible(true);
        
        columns = null;
        rows.clear();
    }

    @Override
    public boolean flush () {

        // TODO Auto-generated method stub
        return true;
    }
}
