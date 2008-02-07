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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;
import org.sqsh.variables.DimensionVariable;
import org.sqsh.variables.FontVariable;

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
        
        int width = 600;
        int height = 400;
        
        DimensionVariable v = (DimensionVariable) session.getVariableManager()
            .getVariable("window_size");
        if (v != null) {
            
            width = v.getWidth();
            height = v.getHeight();
        }
        
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
        frame.setSize(width, height);
        frame.setLocationByPlatform(true);

        // Create a panel to hold all other components
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        frame.getContentPane().add(topPanel);
        
        SortableTableModel tableModel = new SortableTableModel(data, columns,
            session.getDataFormatter().getNull());

        // Create a new table instance
        JTable table = new JTable();
        table.setModel(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        FontVariable fontVar =
            (FontVariable) session.getVariableManager()
                .getVariable("font");
        
        if (fontVar != null) {
            
            table.setFont(new Font(fontVar.getFontName(), Font.PLAIN,
                fontVar.getFontSize() ));
        }
        
        JTableHeader header = table.getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.addMouseListener(tableModel.new ColumnListener(table));
        header.setReorderingAllowed(true);

        // Add the table to a scrolling pane
        JScrollPane scrollPane = new JScrollPane(table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
    
    private static class SortableTableModel
        extends AbstractTableModel {
        
        private ColumnDescription []columns;
        private String [][]data;
        private int sortedColumn = -1;
        private String nullRepresentation;
        private boolean isAscending = false;
        
        public SortableTableModel (String [][]data,
                ColumnDescription []columns,
                String nullRepresentation) {
            
            this.data = data;
            this.columns = columns;
            this.nullRepresentation = nullRepresentation;
        }

        public int getColumnCount () {

            return columns.length;
        }

        public int getRowCount () {

            return data.length;
        }

        public Object getValueAt (int row, int col) {

            return data[row][col];
        }

        public boolean isCellEditable (int row, int col) {

            return false;
        }
        
        public String getColumnName(int col) {
            
            String name = columns[col].getName();
            if (name == null) {
                
                name = "";
            }
            
            if (col == sortedColumn) {
                
                name = name + (isAscending ? " <<" : " >>");
            }
            
            return name;
        }
    
        private class ColumnListener
            extends MouseAdapter {

            protected JTable table;

            public ColumnListener(JTable t) {

                table = t;
            }

            public void mouseClicked (MouseEvent e) {

                TableColumnModel colModel = table.getColumnModel();
                int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
                int modelIndex = colModel.getColumn(columnModelIndex)
                        .getModelIndex();

                if (modelIndex < 0) {

                    return;
                }

                if (sortedColumn == modelIndex) {

                    isAscending = !isAscending;
                }
                else {

                    sortedColumn = modelIndex;
                    isAscending = true;
                }

                for (int i = 0; i < columns.length; i++) {

                    TableColumn column = colModel.getColumn(i);
                    column.setHeaderValue(getColumnName(
                        column.getModelIndex()));
                }

                table.getTableHeader().repaint();

                Arrays.sort(data, new ColumnComparator(modelIndex,
                    isAscending,  columns[modelIndex], nullRepresentation));

                table.tableChanged(new TableModelEvent(
                    SortableTableModel.this));
                table.repaint();
            }
        }
    }
    
    private static class ColumnComparator
        implements Comparator {
        
        private int idx;
        private boolean isAscending;
        private ColumnDescription columnDescription;
        String nullRepresentation;
        
        public ColumnComparator (int idx,
                boolean isAscending,
                ColumnDescription columnDescription,
                String nullRepresentation) {
            
            this.idx = idx;
            this.isAscending = isAscending;
            this.columnDescription = columnDescription;
            this.nullRepresentation = nullRepresentation;
        }

        public int compare (Object o1, Object o2) {
            
            String []row1 = (String[])o1;
            String []row2 = (String[])o2;
            int r = 0;
            
            /*
             * First handle the case of nulls. We treat null as the
             * "lowest" sort.
             */
            if (nullRepresentation.equals(row1[idx])) {
                
                if (nullRepresentation.equals(row2[idx])) {
                    
                    r = 0;
                }
                
                r = -1;
            }
            else if (nullRepresentation.equals(row2[idx])) {
                
                r = 1;
            }
            else if (columnDescription.getType()
                    == ColumnDescription.Type.NUMBER) {
                
                double diff =  Double.parseDouble(row2[idx])
                    - Double.parseDouble(row1[idx]);
                
                if (diff < 0) {
                    
                    r = -1;
                }
                else if (diff > 0) {
                    
                    r = 1;
                }
                else {
                
                    r = 0;
                }
            }
            else {
                
                r = row1[idx].compareTo(row2[idx]);
            }
            
            if (r != 0) {
                
                if (!isAscending) {
                    
                    r = -(r);
                }
            }
            
            return r;
        }
    }
}
