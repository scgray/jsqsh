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
package org.sqsh.commands;

import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.Renderer;
import org.sqsh.SQLConnectionContext;
import org.sqsh.SQLObjectName;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.sqsh.options.ArgumentRequired.NONE;

/**
 * Implements the \describe command.
 */
public class Describe extends Command implements DatabaseCommand {
    private static class Options extends SqshOptions {

        @OptionProperty(option = 'a', longOption = "all", arg = NONE,
                description = "Show all available information")
        public boolean showAll = false;

        @Argv(program = "\\describe", min = 1, max = 1, usage = "object_name")
        public List<String> arguments = new ArrayList<>();
    }

    @Override
    public boolean keepDoubleQuotes() {
        return true;
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;
        if (options.arguments.size() != 1) {
            session.err.println("Use: \\describe [-a] table_name");
            return 1;
        }

        SQLConnectionContext sqlContext = (SQLConnectionContext) session.getConnectionContext();
        Connection con = session.getConnection();
        DatabaseMetaData meta = con.getMetaData();

        SQLObjectName objName = new SQLObjectName(sqlContext, options.arguments.get(0));
        if (objName.isMalformed()) {
            session.err.println("Malformed object name: " + options.arguments.get(0));
            return 1;
        }

        String catalog = (objName.getCatalog() == null ? "%" : objName.getCatalog());
        String schema = (objName.getSchema() == null ? "%" : objName.getSchema());
        String name = (objName.getName() == null ? "%" : objName.getName());

        try (ResultSet result = meta.getColumns(catalog, schema, name, null)) {
            HashSet<Integer> cols = null;
            if (isTable(meta, catalog, schema, name)) {
                if (!options.showAll) {
                    cols = new HashSet<>();
                    cols.add(2); /* owner */
                    cols.add(4); /* column */
                    cols.add(6); /* type */
                    cols.add(7); /* size */
                    cols.add(9); /* decimal digits */
                    cols.add(18); /* nullable? */
                }
            } else {
                if (!options.showAll) {
                    cols = new HashSet<>();
                    cols.add(2); /* owner */
                    cols.add(4); /* column */
                    cols.add(7); /* type */
                    cols.add(8); /* precision */
                    cols.add(9); /* length */
                    cols.add(10); /* scale */
                }
            }
            SQLRenderer sqlRenderer = session.getSQLRenderer();
            Renderer renderer = session.getRendererManager().getCommandRenderer(session);
            sqlRenderer.displayResults(renderer, session, result, cols);
        } catch (SQLException e) {
            SQLTools.printException(session, e);
            return 1;
        }
        return 0;
    }

    /**
     * Does a crappy test to determine of an object name is a table.
     *
     * @param meta Database metadata descriptor.
     * @param catalog The database name
     * @param schema The schema of the object
     * @param name The name of the object.
     * @return True if it is a table.
     */
    private boolean isTable(DatabaseMetaData meta, String catalog, String schema, String name) {
        int nRows = 0;
        try {
            ResultSet result = meta.getColumns(catalog, schema, name, null);
            while (result.next()) {
                ++nRows;
            }
        } catch (SQLException e) {
            /* IGNORED */
        }
        return (nRows > 0);
    }

}
