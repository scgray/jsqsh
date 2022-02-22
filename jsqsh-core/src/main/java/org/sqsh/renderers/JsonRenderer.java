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
import org.sqsh.SQLTools;
import org.sqsh.Session;

/**
 * Displays SQL result sets as JSON values.
 */
public class JsonRenderer extends Renderer {

    private boolean firstResult = true;
    private int nrows = 0;

    public JsonRenderer(Session session, RendererManager renderMan) {
        super(session, renderMan);
    }

    @Override
    public void header(ColumnDescription[] columns) {
        if (firstResult) {
            session.out.println('[');
            firstResult = false;
        }
        super.header(columns);
    }

    @Override
    public boolean row(String[] row) {
        if (nrows > 0) {
            session.out.println(',');
        }

        if (row.length > 1) {
            session.out.println("   {");
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    session.out.println(',');
                }
                ColumnDescription desc = columns[i];
                session.out.print("      \"");
                session.out.print(escape(desc.getName()));
                session.out.print("\": ");
                if (SQLTools.needsQuotes(desc.getNativeType())) {
                    session.out.print('"');
                    session.out.print(escape(row[i]));
                    session.out.print('"');
                } else {
                    session.out.print(row[i]);
                }
            }
            session.out.println();
            session.out.print("   }");
        } else {
            session.out.print("   ");
            session.out.print(row[0]);
        }

        ++nrows;
        return true;
    }

    /**
     * Given a string that may contain double quotes, escapes them if they exist.
     *
     * @param str The string to check
     * @return The escaped string.
     */
    private String escape(String str) {

        int idx = str.indexOf('"');
        if (idx < 0) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str.length() + 4);
        sb.append(str, 0, idx);
        sb.append("\\\"");
        ++idx;
        while (idx < str.length()) {
            int nextIdx = str.indexOf('"', idx);
            if (nextIdx < 0) {
                nextIdx = str.length();
            }
            sb.append(str, idx, nextIdx);
            idx = nextIdx + 1;
        }
        return sb.toString();
    }

    @Override
    public boolean flush() {
        session.out.println();
        session.out.println(']');
        return true;
    }
}
