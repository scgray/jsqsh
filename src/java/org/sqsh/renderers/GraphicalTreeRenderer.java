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
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;

import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;
import org.sqsh.variables.DimensionVariable;
import org.sqsh.variables.FontVariable;

/**
 * The GraphicalTreeRenderer displays row results using a swing graphical
 * tree interface.
 * 
 * @author Ryan O. Stouffer
 */
public class GraphicalTreeRenderer
    extends Renderer {
    
    private DefaultMutableTreeNode rootTreeNode;
    private JPanel topPanel;
    private JTree tree;
    private int[] colOrder;
    private TreeWillExpandListener twel;    

    
    /**
     * Creates the renderer.
     * 
     * @param session The session that needs renderin'
     * @param manager The manager that owns this renderer.
     */
    public GraphicalTreeRenderer(Session session, RendererManager manager) {

        super(session, manager);
        
    }
    
    /**
     * Returns the Columns to order for a specific run
     * @return the colOrder
     */
    public int[] getColOrder() {
        
        return colOrder;
    }
    
    /**
     * A way to set a will expand listener, this was added as a hack to 
     * interface with the tree command.  This too will be nullified after
     * the footer has been called.
     * @param twel
     */
    public void setTreeWillExpandListener(TreeWillExpandListener twel) {
        
        this.twel = twel;
    }
    
    /**
     * Sets the Columns to order for a specific run.  The col order is set
     * to null after footer is called.
     * @return the colOrder
     */
    public void setColOrder(int[] colOrder) {
        
        this.colOrder = colOrder;
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

        int width = 600;
        int height = 400;
        
        if (columns == null) {
            
            return;
        }
        
        StringBuilder label = new StringBuilder();
        
        
        /* col order is workaround to supply a column id and order to show 
           otherwise the natural order is shown*/
        if (colOrder == null) {

            colOrder = new int[columns.length];
            
            for (int i = 0 ; i < columns.length; i++) {
            
                if (i > 0) {
                    
                    label.append(" >> ");
                }
                
                colOrder[i] = i;
                label.append(columns[i]);
                
            }
            
        }
        else {

            for (int i = 0; i < colOrder.length; i++) {

                if (i > 0) {
                    
                    label.append(" >> ");
                }
                
                label.append(columns[colOrder[i]]);
                
            }
            
        }
        
        JLabel headerLabel = new JLabel(label.toString());
        
        DimensionVariable v = (DimensionVariable) session.getVariableManager()
            .getVariable("window_size");
        
        if (v != null) {
            
            width = v.getWidth();
            height = v.getHeight();
        }

        JFrame frame = new JFrame();

        // Set the frame characteristics
        frame.setTitle("Query Results");
        frame.setSize(width, height);
        frame.setLocationByPlatform(true);

        // Create a panel to hold all other components
        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        
        frame.getContentPane().add(topPanel);

        rootTreeNode = new DefaultMutableTreeNode();
        
        // Create a new table instance
        tree = new JTree(rootTreeNode); 
        
        FontVariable fontVar =
            (FontVariable) session.getVariableManager()
                .getVariable("font");
        
        if (fontVar != null) {
            
            tree.setFont(new Font(fontVar.getFontName(), Font.PLAIN,
                fontVar.getFontSize()));
        }
        
        // Add the table to a scrolling pane
        JScrollPane scrollPane = new JScrollPane(tree,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        topPanel.add( headerLabel, BorderLayout.NORTH);
        topPanel.add( scrollPane, BorderLayout.CENTER );

        if (twel != null) {
            
            tree.addTreeWillExpandListener(twel);
        }
        
        frame.setVisible(true);
        
    }
    
    /**
     * Finds a node if it isn't there one is created and added to the parent.
     * @param parent
     * @param label
     * @return
     */
    private DefaultMutableTreeNode 
        findChild(DefaultMutableTreeNode parent, String label) {
        
        int size = parent.getChildCount();

        for (int i = 0; i < size; i++) {
            
            DefaultMutableTreeNode kid = 
                (DefaultMutableTreeNode)parent.getChildAt(i); 
            
            if (kid.getUserObject().equals(label)) {
                
                return kid;              
                
            }
        }
        
        DefaultMutableTreeNode newKid = new DefaultMutableTreeNode(label);
        
        parent.add(newKid);
       
        return newKid;
    }
    
    /**
     * Adds a row to the tree grouping by the natural order of columns
     * or if set if it is the colOrder.
     * 
     * @param row The row to add.
     */
    @Override
    public boolean row (String[] row) {

        DefaultMutableTreeNode parent = rootTreeNode;
        
        for(int col:colOrder) {

            String label = row[col];
           
            DefaultMutableTreeNode node = findChild(parent, label);
            
            parent = node;                    
        }
        
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.sqsh.Renderer#footer(java.lang.String)
     */
    @Override
    public void footer (String footer) {
        
        if (topPanel != null) {
            
            JLabel footerText = new JLabel(footer);
            
            topPanel.add(footerText, BorderLayout.SOUTH);

        }        
                
    }

    @Override
    public boolean flush () {

        /**
         * set all the tree defaults and reset.
         */
        
        if (topPanel != null) {

            tree.expandPath(tree.getPathForRow(0));
            
            tree.setShowsRootHandles(true);
            
            tree.setRootVisible(false);
            
            tree = null;            
            rootTreeNode = null;
            topPanel = null;
            colOrder = null; 
        }
        
        return true;
    }
    
}
