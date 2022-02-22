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

public class ISQLRenderer extends Renderer {

    private final int screenWidth;

    public ISQLRenderer(Session session, RendererManager renderMan) {
        super(session, renderMan);

        // We use this a lot so fetch it once.
        screenWidth = session.getScreenWidth();
    }

    /**
     * {@inheritDoc}
     */
    public void header(ColumnDescription[] columns) {
        super.header(columns);
        int totalWidth = 0;

        // Print the column names.
        for (int i = 0; i < columns.length; i++) {

            // isql caps the display width of the column header at 512.
            if (columns[i].getWidth() > 512) {
                columns[i].setWidth(512);
            }
            if (i > 0 && (totalWidth + columns[i].getWidth() + 1) > screenWidth) {
                session.out.println();
                session.out.print(" \t");
                totalWidth = 9;
            } else {
                session.out.print(' ');
                ++totalWidth;
            }
            printColumnName(columns[i], columns[i].getName());
            totalWidth += columns[i].getWidth();
        }
        session.out.println();

        // Now our dashes.
        for (int i = 0; i < columns.length; i++) {
            if (i > 0 && (totalWidth + columns[i].getWidth() + 1) > screenWidth) {
                session.out.println();
                session.out.print(" \t");
                totalWidth = 9;
            } else {
                session.out.print(' ');
                ++totalWidth;
            }
            dashes(columns[i].getWidth());
            totalWidth += columns[i].getWidth();
        }
        session.out.println();
    }

    public boolean row(String[] row) {
        int totalWidth = 0;

        for (int i = 0; i < columns.length; i++) {
            if (i > 0 && (totalWidth + columns[i].getWidth() + 1) > screenWidth) {
                session.out.println();
                session.out.print(" \t");
                totalWidth = 9;
            } else {
                session.out.print(' ');
                ++totalWidth;
            }
            printColumnValue(columns[i], row[i]);
            totalWidth += columns[i].getWidth();
        }
        session.out.println();

        return true;
    }

    @Override
    public boolean flush() {
        session.out.println();
        return true;
    }
}
