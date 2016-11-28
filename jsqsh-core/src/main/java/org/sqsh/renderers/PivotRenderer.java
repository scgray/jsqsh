/*
 * Copyright 2007-2016 Scott C. Gray
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private String[][] table;
    private Map<String, Integer> valueToRowIdx = new LinkedHashMap<String, Integer>();
    private Map<String, Integer> valueToColIdx = new HashMap<String, Integer>();
    private int nColumns;
    private ColumnDescription []descriptions;
    private ColumnDescription valueColumnDescription;

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
        nColumns = 0;
        table = null;

        if (isOk) {

            valueColumnDescription = columns[dataColNum];
            descriptions = new ColumnDescription[1];
            descriptions[0] = copyDescription(columns[vertColNum], "");
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

        // DO NOTHING
    }

    @Override
    public boolean flush() {

        if (! isOk) {

            return false;
        }

        if (table != null) {

            out.header(descriptions);
            for (String []row : table) {

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

        // Add the column definition
        ColumnDescription[] newDescs = new ColumnDescription[descriptions.length+1];
        System.arraycopy(descriptions, 0, newDescs, 0, descriptions.length);
        newDescs[descriptions.length] = copyDescription(valueColumnDescription, columnName);
        descriptions = newDescs;

        int newIdx = nColumns++;
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

    private static ColumnDescription copyDescription(ColumnDescription source, String newName) {

        ColumnDescription dest = new ColumnDescription(
                newName,
                source.getWidth(),
                source.getAlignment(),
                source.getOverflowBehavior(),
                source.isResizeable());

        return dest;
    }

    private int findColumn(ColumnDescription []columns, String name) {

        if (isNumber(name)) {

            int val = Integer.parseInt(name);
            if (val < 0 || val >= columns.length) {

                session.err.println("Column #" + val + " does not exist in result set "
                    + "(which has " + columns.length + " columns)");
                return -1;
            }

            return val;
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
}
