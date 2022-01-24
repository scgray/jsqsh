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

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;

import java.util.*;

/**
 * The PivotRenderer is a "special" renderer that takes an incoming result
 * set and pivots it based upon specific column in the original result set.
 * For example, given a result set of:
 * <pre>
 * +-------+-----------+----------+
 * | STATE | DAYOFWEEK |    SALES |
 * +-------+-----------+----------+
 * | NJ    | Mon       | 14.20000 |
 * | NJ    | Tue       | 11.40000 |
 * | NJ    | Wed       | 19.30000 |
 * | CA    | Mon       |  4.10000 |
 * | CA    | Tue       |  8.30000 |
 * | CA    | Wed       | 44.20000 |
 * | NJ    | Thu       | 17.10000 |
 * | AR    | Tue       |  4.30000 |
 * +-------+-----------+----------+
 * </pre>
 * You can indicate which column becomes the row header (called the vertical
 * header), which becomes the column header (the horizontal header) and which
 * provides the data. In this case, choosing:
 * <pre>
 *     Vertical   = DAYOFWEEK
 *     Horizontal = STATE
 *     Data       = SALES
 * </pre>
 * It would produce a result set like:
 * <pre>
 * +-----------+---------+----------+----------+
 * | DAYOFWEEK |      AR |       CA |       NJ |
 * +-----------+---------+----------+----------+
 * | Mon       |  [NULL] |  4.10000 | 14.20000 |
 * | Tue       | 4.30000 |  8.30000 | 11.40000 |
 * | Wed       |  [NULL] | 44.20000 | 19.30000 |
 * | Thu       |  [NULL] |   [NULL] | 17.10000 |
 * +-----------+---------+----------+----------+
 * </pre>
 * In order to do this, it is important to note that the PivotRenderer must buffer
 * all of the data for the final table in memory before display!
 */
public class PivotRenderer extends Renderer {

    private Renderer out;
    // Id of the column that drives the vertical column headers
    private int vertColNum;
    private String vertColName;
    // Id of the column that drives the horizontal column headers
    private int horizColNum;
    private String horizColName;
    // Id of the column that provides value within the table
    private int dataColNum;
    private String dataColName;
    private boolean isOk = true;

    // The actual underlying table of data in [row][column] order
    private String[][] table;
    // Maps a value in the row (vertical) column header to which row in the
    // final table contains the values
    private Map<String, Integer> valueToRowIdx = new LinkedHashMap<>();
    // Maps a value in the column (horizontal) header to which row in the
    // final table contains the values
    private Map<String, Integer> valueToColIdx = new HashMap<>();
    // Number of columns in the results
    private int nColumns;
    // Column descriptors for each column header in the final table
    private ColumnInfo []descriptions;
    // The original column descriptor for the column chosen as the
    // horizontal column header. A copy of this is made each time a new
    // column is added to the final table.
    private ColumnDescription valueColumnDescription;

    private static class ColumnInfo {

        // Index of the column in the table[row][column] array
        public int idx;
        // Description of the column
        public ColumnDescription desc;

        public ColumnInfo(ColumnDescription desc, int idx, String name) {

            this.desc = new ColumnDescription(
                    name,
                    desc.getWidth(),
                    desc.getAlignment(),
                    desc.getOverflowBehavior(),
                    desc.isResizeable());

            this.idx = idx;
        }
    }

    /**
     * Creates the pivot renderer
     * @param session The session running the query
     * @param renderMan The renderer manager
     * @param out A renderer that performs the actual final display of the results
     *            that the pivot renderer produces
     * @param vertColName The name or number (indexed from 1) of the column that
     *            will produce the row headers
     * @param horizColName The name or number of the column that will produce the
     *            horizontal column headers
     * @param dataColName The column that will provide the data for the final table.
     */
    public PivotRenderer(Session session, RendererManager renderMan,
        Renderer out, String vertColName, String horizColName, String dataColName) {

        super(session, renderMan);

        this.out = out;
        this.vertColName = vertColName;
        this.horizColName = horizColName;
        this.dataColName = dataColName;
    }

    @Override
    public void header(ColumnDescription[] columns) {

        // Assume all is right with the world
        vertColNum = findColumn(columns, vertColName);
        horizColNum = findColumn(columns, horizColName);
        dataColNum = findColumn(columns, dataColName);

        // If any of the column's were not found, then don't try to display anything
        // during this round of results.
        isOk = (vertColNum >= 0 && horizColNum >= 0 && dataColNum >= 0);
        valueToRowIdx.clear();
        valueToColIdx.clear();
        nColumns = 1;
        table = null;

        if (isOk) {

            valueColumnDescription = columns[dataColNum];
            descriptions = new ColumnInfo[1];
            descriptions[0] = new ColumnInfo(columns[vertColNum], 0, columns[vertColNum].getName());
        }
    }

    @Override
    public boolean row(String[] row) {

        // Ask the caller to abort if the columns we need didn't exist
        if (! isOk) {

            return false;
        }

        Integer rowId = valueToRowIdx.get(row[vertColNum]);
        if (rowId == null) {

            Integer colId = valueToColIdx.get(row[horizColNum]);
            if (colId == null) {

                addNewRowAndColumn(row[vertColNum], row[horizColNum], row[dataColNum]);
            }
            else {

                addNewRow(row[vertColNum], colId, row[dataColNum]);
            }
        }
        else {

            Integer colId = valueToColIdx.get(row[horizColNum]);
            if (colId == null) {

                addColumnToExistingRow(rowId, row[horizColNum], row[dataColNum]);
            }
            else {

                setValueForRowAndColumn(rowId, colId, row[dataColNum]);
            }
        }

        return true;
    }

    @Override
    public void footer(String footer) {

        out.footer(footer);
    }

    @Override
    public boolean flush() {

        if (! isOk) {

            return false;
        }

        if (table != null) {

            Arrays.sort(descriptions, 1, descriptions.length, new Comparator<ColumnInfo>() {

                @Override
                public int compare(ColumnInfo o1, ColumnInfo o2) {

                    return o1.desc.getName().compareTo(o2.desc.getName());
                }
            });

            ColumnDescription []cols = new ColumnDescription[descriptions.length];
            for (int i = 0; i < descriptions.length; i++) {

                cols[i] = descriptions[i].desc;
            }

            out.header(cols);


            for (int i = 0; i < table.length; i++) {

                String[] row = new String[nColumns];
                String[] currentRow = table[i];
                for (int j = 0; j < descriptions.length; j++) {
                    int idx = descriptions[j].idx;
                    if (idx < currentRow.length) {

                        row[j] = currentRow[idx];
                    }
                    else {

                        row[idx] = null;
                    }
                }

                out.row(row);
            }

            table = null;
            return out.flush();
        }

        return true;
    }

    private void setValueForRowAndColumn(int rowId, int colId, String value) {

        String[] row = table[rowId];
        if (colId >= row.length) {

            String[] newRow = new String[nColumns];
            System.arraycopy(row, 0, newRow, 0, row.length);
            row = newRow;
            table[rowId] = row;
        }

        row[colId] = value;
    }

    private void addNewRowAndColumn(String rowHeader, String columnHeader, String value) {

        int colNum = addColumn(columnHeader);
        String[] row = new String[nColumns];
        row[0] = rowHeader;
        row[colNum] = value;

        addRow(row);
    }

    private void addNewRow(String rowHeader, int colIdx, String value) {

        String[] row = new String[nColumns];
        row[0] = rowHeader;
        row[colIdx] = value;
        addRow(row);
    }

    private void addColumnToExistingRow(int rowIdx, String columnName, String value) {

        String[] existingRow = table[rowIdx];

        // New column number we have assigned
        int colNum = addColumn(columnName);

        // Add the column in the data
        String[] newRow = new String[nColumns];
        System.arraycopy(existingRow, 0, newRow, 0, existingRow.length);
        newRow[colNum] = value;
        table[rowIdx] = newRow;
    }

    private int addColumn(String columnName) {

        int newIdx = nColumns++;

        // Add the column definition
        ColumnInfo[] newDescs = new ColumnInfo[descriptions.length+1];
        System.arraycopy(descriptions, 0, newDescs, 0, descriptions.length);
        newDescs[descriptions.length] = new ColumnInfo(valueColumnDescription, newIdx, columnName);
        descriptions = newDescs;

        valueToColIdx.put(columnName, newIdx);
        return newIdx;
    }

    private void addRow(String[] newRow) {

        int idx = 0;

        if (table == null) {

            table = new String[1][];
            table[idx] = newRow;
        }
        else {

            idx = table.length;
            String newTable[][] = new String[table.length+1][];
            System.arraycopy(table, 0, newTable, 0, table.length);
            newTable[idx] = newRow;
            table = newTable;
        }

        valueToRowIdx.put(newRow[0], idx);
    }

    private int findColumn(ColumnDescription []columns, String name) {

        if (isNumber(name)) {

            int val = Integer.parseInt(name);
            if (val < 1 || val > columns.length) {

                session.err.println("Column #" + val + " does not exist in result set "
                    + "(which has " + columns.length + " columns)");
                return -1;
            }

            return val-1;
        }

        /*
         * TODO: When matching column names this should really happen according to the
         * same rules that are used by the database. For convenience I'll just to a case
         * insenstive match for now.
         */
        for (int i = 0; i < columns.length; i++) {

            if (name.equalsIgnoreCase(columns[i].getName())) {

                return i;
            }
        }

        session.err.println("Column \"" + name + "\" does not exist in result set");
        return -1;
    }

    private static boolean isNumber(String str) {

        final int len = str.length();
        for (int i = 0; i < len; i++) {

            if (! Character.isDigit(str.charAt(i))) {

                return false;
            }
        }

        return true;
    }

    private void dumpTable() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < descriptions.length; i++) {

            if (i > 0) {

                sb.append(",");
            }
            sb.append(descriptions[i].desc.getName());
        }
        System.out.println(sb.toString());

        for (int i = 0; i < table.length; i++) {

            sb.setLength(0);
            for (int j = 0; j < table[i].length; j++) {

                if (j > 0) {

                    sb.append(",");
                }
                sb.append(table[i][j]);
            }
            System.out.println(sb.toString());
        }
    }
}
