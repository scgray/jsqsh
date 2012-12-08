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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
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
 */
public class GraphicalRenderer
    extends Renderer {
    
    private SortableTableModel tableModel;
    private JPanel topPanel;
    
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

        int width = 600;
        int height = 400;

        if (columns == null) {
            
            return;
        }
        
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

        // Create a new table instance
        JTable table = new JTable();

        tableModel = new SortableTableModel(new String[0][], columns,
                    session.getDataFormatter().getNull());   
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
                
        frame.setVisible(true);
        
    }
    
    /**
     * Adds a row to the panel.
     * 
     * @param row The row to add.
     */
    @Override
    public boolean row (String[] row) {

        tableModel.addRow(row);
        
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
            
            topPanel.repaint();
            tableModel = null;
            topPanel = null;        
        }
                
    }

    @Override
    public boolean flush () {

        return true;
    }
    
    private static class SortableTableModel
        extends DefaultTableModel {
        
        private static final long serialVersionUID = 1L;
        private ColumnDescription []columns;
        private int sortedColumn = -1;
        private String nullRepresentation;
        private boolean isAscending = false;
        
        public SortableTableModel(String [][]data,
                ColumnDescription []columns,
                String nullRepresentation) {
            
            super(data, columns);
  
            this.columns = columns;
            
            this.nullRepresentation = nullRepresentation;
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

                for (int i = 0; i < getColumnCount(); i++) {

                    TableColumn column = colModel.getColumn(i);
                    column.setHeaderValue(getColumnName(
                        column.getModelIndex()));
                }

                table.getTableHeader().repaint();

                ColumnDescription des = columns[modelIndex];
                
                Collections.sort(getDataVector(), 
                        new ColumnComparator(modelIndex,
                    isAscending,  des, nullRepresentation));
                
                table.tableChanged(new TableModelEvent(
                    SortableTableModel.this));
                table.repaint();
            }
        }
    }
    
    private static class ColumnComparator
        implements Comparator<Vector<String>> {
        
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

        public int compare (Vector<String> o1, Vector<String> o2) {
            
            Vector<String> row1 = o1;
            Vector<String> row2 = o2;
            int r = 0;
            
            /*
             * First handle the case of nulls. We treat null as the
             * "lowest" sort.
             */
            if (nullRepresentation.equals(row1.get(idx))) {
                
                if (nullRepresentation.equals(row2.get(idx))) {
                    
                    r = 0;
                }
                
                r = -1;
            }
            else if (nullRepresentation.equals(row2.get(idx))) {
                
                r = 1;
            }
            else if (columnDescription.getType()
                    == ColumnDescription.Type.NUMBER) {
                
                double diff =  Double.parseDouble(row2.get(idx))
                    - Double.parseDouble(row1.get(idx));
                
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
                
                r = row1.get(idx).compareTo(row2.get(idx));
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
