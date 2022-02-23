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

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.Renderer;
import org.sqsh.SQLConnectionContext;
import org.sqsh.SQLObjectName;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.sqsh.options.ArgumentRequired.NONE;
import static org.sqsh.options.ArgumentRequired.REQUIRED;

public class Show extends Command implements DatabaseCommand {

    static Set<Integer> essentialTableCols = Set.of(
        1,   // TABLE_CAT
        2,   // TABLE_SCHEMA
        3,   // TABLE_NAME
        4);  // TABLE_TYPE

    static Set<Integer> essentialColumnCols = Set.of(
        1,   // TABLE_CAT
        2,   // TABLE_SCHEMA
        3,   // TABLE_NAME
        4,   // COLUMN_NAME
        6,   // TYPE_NAME
        7,   // COLUMN_SIZE
        9,   // DECIMAL_DIGITS
        11); // NULLABLE

    static Set<Integer> essentialFunctionParamsCols = Set.of(
        1,  // FUNCTION_CAT
        2,  // FUNCTION_SCHEMA
        3,  // FUNCTION_NAME
        4,  // COLUMN_NAME
        5,  // COLUMN_TYPE
        7,  // TYPE_NAME
        8,  // PRECISION
        9,  // LENGTH
        10, // SCALE
        12);// NULLABLE

    static Set<Integer> essentialFunctionCols = Set.of(
        1,  // FUNCTION_CAT */
        2,  // FUNCTION_SCHEMA
        3,  // FUNCTION_NAME
        5,  // FUNCTION_TYPE
        6); // SPECIFIC_NAME

    static Set<Integer> essentialProcedureParamsCols = Set.of(
        1,  // PROCEDURE_CAT
        2,  // PROCEDURE_SCHEMA
        3,  // PROCEDURE_NAME
        4,  // COLUMN_NAME
        5,  // COLUMN_TYPE
        7,  // TYPE_NAME
        8,  // PRECISION
        9,  // LENGTH
        10, // SCALE
        12, // NULLABLE
        20);// SPECIFIC_NAME

    static Set<Integer> essentialTypesCols = Set.of(
        1,  // TYPE_NAME
        3,  // PRECISION
        4,  // PROCEDURE_NAME
        7,  // NULLABLE
        12);// AUTO_INCREMENT

    private static class Options extends SqshOptions {
        
        @OptionProperty(
            option='e', longOption="essential", arg=NONE, 
            description="Restricts columns to the most esstential columns")
        public boolean essential = false;
        
        @OptionProperty(deprecated=true,
            option='c', longOption="catalog", arg=REQUIRED, argName="catalog",
            description="Provides the catalog in which to search")
        public String catalog = null;
        
        @OptionProperty(deprecated=true,
            option='t', longOption="table", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against table names")
        public String tablePattern = null;
        
        @OptionProperty(deprecated=true,
            option='s', longOption="schema", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against schema names")
        public String schemaPattern = null;
        
        @OptionProperty(
            option='p', longOption="pattern", arg=REQUIRED, argName="pattern",
            description="Provides additional search pattern")
        public String pattern = null;
        
        @OptionProperty(
            option='T', longOption="type", arg=REQUIRED, argName="name",
            description="Restricts the search to specific table types")
        public String tableType = null;
        
        @Argv(program="\\show", min=1, max=5,
            usage="[options] [\n" 
               + "           attributes [-p pattern] [[[catalog.]schema-pattern.]type-pattern]\n"
               + "         | catalogs\n"
               + "         | client info\n"
               + "         | column privs [-p col-pattern] [[[catalog.]schema-pattern.]obj-pattern]\n"
               + "         | columns [-e] [-p col-pattern] [[[catalog.]schema-pattern.]obj-pattern]\n"
               + "         | driver version\n"
               + "         | exported keys [[catalog.]schema.]obj-name\n"
               + "         | function params [-e] [-p param-pattern] [[[catalog.]schema-pattern.]func-pattern]\n"
               + "         | functions [-e] [[[catalog.]schema-pattern.]func-pattern]\n"
               + "         | imported keys [[catalog.]schema.]table\n"
               + "         | primary keys [[catalog.]schema.]table\n"
               + "         | procedure params [-e] [-p param-pat] [[[catalog.]schema-pattern.]proc-pattern]\n"
               + "         | procedures [[[catalog.]schema-pattern.]proc-pattern]\n"
               + "         | server version\n"
               + "         | schemas [[catalog.]schema-pattern]\n"
               + "         | super tables [[[catalog.]schema-pattern.]table-pattern]\n"
               + "         | super types [[[catalog.]schema-pattern.]type-pattern]]\n"
               + "         | table privs [[[catalog.]schema-pattern.]table-pattern]\n"
               + "         | tables [-e] [-T type] [[[catalog.]schema-pattern.]table-pattern]\n"
               + "         | table types\n"
               + "         | types [-e]\n"
               + "         | user types [[[catalog.]schema-pattern.]type-pattern]\n"
               + "         | version columns [[[catalog.]schema.]table\n"
               + "       ]"
               )
        public List<String> arguments = new ArrayList<>();
        
        // This isn't an argument. It is used by each of the sub commands to set a set of columns that they wish to
        // have displayed.
        public Set<Integer> columns = null;
    }

    @Override
    public SqshOptions getOptions() {
        return new Options();
    }

    @Override
    public boolean keepDoubleQuotes() {
        return true;
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        Options options = (Options) opts;
        Connection con = session.getConnection();
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        String subCommand = options.arguments.get(0);
        options.catalog = ctx.normalizeIdentifier(options.catalog);
        options.schemaPattern = ctx.normalizeIdentifier(options.schemaPattern);
        options.tablePattern = ctx.normalizeIdentifier(options.tablePattern);
        if ("features".equalsIgnoreCase(subCommand)) {
            Renderer renderer = session.getRendererManager().getCommandRenderer(session);
            doFeatures(session, renderer, con);
            return 0;
        } else if ("server".equalsIgnoreCase(subCommand)) {
            Renderer renderer = session.getRendererManager().getCommandRenderer(session);
            doServer(session, renderer, con, options);
            return 0;
        } else if ("driver".equalsIgnoreCase(subCommand)) {
            Renderer renderer = session.getRendererManager().getCommandRenderer(session);
            doDriver(session, renderer, con, options);
            return 0;
        }
        Renderer renderer = session.getRendererManager().getCommandRenderer(session);
        ResultSet result = null;
        try {
            if ("tables".equalsIgnoreCase(subCommand)) {
                result = doTables(session, con, options);
            } else if ("attributes".equalsIgnoreCase(subCommand)) {
                result = doAttributes(session, con, options);
            } else if ("catalogs".equalsIgnoreCase(subCommand)) {
                result = doCatalogs(session, con, options);
            } else if ("client".equalsIgnoreCase(subCommand)) {
                result = doClient(session, con, options);
            } else if ("column".equalsIgnoreCase(subCommand)) {
                result = doColumn(session, con, options);
            } else if ("columns".equalsIgnoreCase(subCommand)) {
                result = doColumns(session, con, options);
            } else if ("exported".equalsIgnoreCase(subCommand)) {
                result = doExportedKeys(session, con, options);
            } else if ("function".equalsIgnoreCase(subCommand)) {
                result = doFunction(session, con, options);
            } else if ("functions".equalsIgnoreCase(subCommand)) {
                result = doFunctions(session, con, options);
            } else if ("imported".equalsIgnoreCase(subCommand)) {
                result = doImportedKeys(session, con, options);
            } else if ("primary".equalsIgnoreCase(subCommand)) {
                result = doPrimaryKeys(session, con, options);
            } else if ("procedure".equalsIgnoreCase(subCommand)) {
                result = doProcedure(session, con, options);
            } else if ("procedures".equalsIgnoreCase(subCommand)) {
                result = doProcedures(session, con, options);
            } else if ("schemas".equalsIgnoreCase(subCommand)) {
                result = doSchemas(session, con, options);
            } else if ("super".equalsIgnoreCase(subCommand)) {
                result = doSuper(session, con, options);
            } else if ("table".equalsIgnoreCase(subCommand)) {
                result = doTable(session, con, options);
            } else if ("types".equalsIgnoreCase(subCommand)) {
                result = doTypes(session, con, options);
            } else if ("user".equalsIgnoreCase(subCommand)) {
                result = doUser(session, con, options);
            } else if ("version".equalsIgnoreCase(subCommand)) {
                result = doVersion(session, con, options);
            } else {
                session.err.println("Unrecognized object type \"" + subCommand + "\". See \"\\help show\" for details");
                return 1;
            }
            if (result != null) {
                SQLRenderer sqlRenderer = session.getSQLRenderer();
                sqlRenderer.displayResults(renderer, session, result, options.columns);
            }
        } catch (AbstractMethodError e) {
            session.err.println("Operation not supported by JDBC driver");
        } catch (SQLException e) {
            session.err.println("Failed to retrieve database metadata: " + e.getMessage());
            return 1;
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {

                    /* IGNORED */
                }
            }
        }
        return 0;
    }

    private ResultSet doAttributes(Session session, Connection con, Options options) throws SQLException {
        SQLObjectName name = null;
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        if (options.arguments.size() == 1) {
            name = new SQLObjectName(ctx, "%");
        } else if (options.arguments.size() == 2) {
            name = new SQLObjectName(ctx, options.arguments.get(1));
        } else if (options.arguments.size() > 2) {
            session.err.println("Use: \\show attributes [-p pattern] [[[catalog.]schema-pattern.]type-pattern]");
            return null;
        }
        DatabaseMetaData meta = con.getMetaData();
        return meta.getAttributes(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()),
                ctx.normalizeIdentifier(options.pattern));
    }

    private ResultSet doCatalogs(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 1) {
            session.err.println("Use: \\show catalogs");
            return null;
        }
        DatabaseMetaData meta = con.getMetaData();
        return meta.getCatalogs();
    }

    private ResultSet doClient(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 2 || !options.arguments.get(1).equalsIgnoreCase("info")) {
            session.err.println("Use: \\show client info");
            return null;
        }
        DatabaseMetaData meta = con.getMetaData();
        return meta.getClientInfoProperties();
    }

    private ResultSet doColumn(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() < 2 || options.arguments.size() > 3
                || !options.arguments.get(1).equalsIgnoreCase("privs")) {
            session.err.println("Use: \\show column privs [-p col-pattern] [[[catalog.]schema-pattern.]table-pattern]");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 3)
                ? new SQLObjectName(ctx, options.arguments.get(2))
                : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getColumnPrivileges(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()),
                ctx.normalizeIdentifier(options.pattern));
    }

    private ResultSet doColumns(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() > 2) {
            session.err.println("Use: \\show columns [-p col-pattern] [[[catalog.]schema-pattern.]table-pattern]");
            return null;
        }
        if (options.essential) {
            options.columns = essentialColumnCols;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 2) ? new SQLObjectName(ctx, options.arguments.get(1)) : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getColumns(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()),
                ctx.normalizeIdentifier(options.pattern));
    }

    private ResultSet doExportedKeys(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 3 || !options.arguments.get(1).equalsIgnoreCase("keys")) {
            session.err.println("Use: \\show exported keys [[catalog.]schema.]table-pattern");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = new SQLObjectName(ctx, options.arguments.get(2));
        DatabaseMetaData meta = con.getMetaData();
        return meta.getExportedKeys(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()));
    }

    private ResultSet doFunction(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() < 2 || options.arguments.size() > 3
                || !options.arguments.get(1).equalsIgnoreCase("params")) {
            session.err.println("Use: \\show function params [-p param-pattern] [[[catalog.]schema-pattern.]func-pattern]");
            return null;
        }
        if (options.essential) {
            options.columns = essentialFunctionParamsCols;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 3) ? new SQLObjectName(ctx, options.arguments.get(2)) : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getFunctionColumns(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()),
                ctx.normalizeIdentifier(options.pattern));
    }

    private ResultSet doFunctions(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() > 2) {
            session.err.println("Use: \\show functions [[[catalog.]schema-pattern.]func-pattern]");
            return null;
        }
        if (options.essential) {
            options.columns = essentialFunctionCols;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 2) ? new SQLObjectName(ctx, options.arguments.get(1)) : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getFunctions(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()));
    }

    private ResultSet doImportedKeys(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 3 || !options.arguments.get(1).equalsIgnoreCase("keys")) {
            session.err.println("Use: \\show imported keys [[catalog.]schema.]table");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = new SQLObjectName(ctx, options.arguments.get(2));
        DatabaseMetaData meta = con.getMetaData();
        return meta.getImportedKeys(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()));
    }

    private ResultSet doPrimaryKeys(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 3 || !options.arguments.get(1).equalsIgnoreCase("keys")) {
            session.err.println("Use: \\show primary keys [[catalog.]schema.]table");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = new SQLObjectName(ctx, options.arguments.get(2));
        DatabaseMetaData meta = con.getMetaData();
        return meta.getPrimaryKeys(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()));
    }

    private ResultSet doProcedure(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() < 2 || options.arguments.size() > 3
                || !options.arguments.get(1).equalsIgnoreCase("params")) {
            session.err.println("Use: \\show procedure params [-e] [-p param-pat] [[[catalog.]schema-pattern.]proc-pattern]");
            return null;
        }
        if (options.essential) {
            options.columns = essentialProcedureParamsCols;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 3)
                ? new SQLObjectName(ctx, options.arguments.get(2))
                : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getProcedureColumns(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()),
                ctx.normalizeIdentifier(options.pattern));
    }

    private ResultSet doProcedures(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() > 2) {
            session.err.println("Use: \\show procedures [[[catalog.]schema-pattern.]proc-pattern]");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 2)
                ? new SQLObjectName(ctx, options.arguments.get(1))
                : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getProcedures(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()));
    }

    private ResultSet doSchemas(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() > 2) {
            session.err.println("Use: \\show schemas [[catalog.]schema-pattern]");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        String arg = "%.%";
        if (options.arguments.size() == 2) {
            arg = options.arguments.get(1) + ".%";
        }
        SQLObjectName name = new SQLObjectName(ctx, arg);
        DatabaseMetaData meta = con.getMetaData();
        try {
            return meta.getSchemas(
                    (options.catalog != null ? options.catalog : name.getCatalog()),
                    (options.schemaPattern != null ? options.schemaPattern : name.getSchema()));
        } catch (AbstractMethodError e) {
            return new PatternFilteredResultSet(meta.getSchemas(),
                    1, (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                    2, (options.catalog != null ? options.catalog : name.getCatalog()));
        }
    }

    private ResultSet doSuper(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() < 2) {
            session.err.println("Use: \\show super [tables | types] ...");
            return null;
        }
        String obj = options.arguments.get(1);
        if (obj.equalsIgnoreCase("tables")) {
            if (options.arguments.size() > 3) {
                session.err.println("Use: \\show super tables [[[catalog.]schema-pattern.]table-pattern]");
                return null;
            }
            SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
            SQLObjectName name = (options.arguments.size() == 3)
                    ? new SQLObjectName(ctx, options.arguments.get(2))
                    : new SQLObjectName(ctx, "%");
            DatabaseMetaData meta = con.getMetaData();
            return meta.getSuperTables(
                    (options.catalog != null ? options.catalog : name.getCatalog()),
                    (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                    (options.tablePattern != null ? options.tablePattern : name.getName()));
        } else if (obj.equalsIgnoreCase("types")) {
            if (options.arguments.size() > 3) {
                session.err.println("Use: \\show super types [[[catalog.]schema-pattern.]type-pattern]");
                return null;
            }
            SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
            SQLObjectName name = (options.arguments.size() == 3) ? new SQLObjectName(ctx, options.arguments.get(2)) : new SQLObjectName(ctx, "%");
            DatabaseMetaData meta = con.getMetaData();
            return meta.getSuperTypes(
                    (options.catalog != null ? options.catalog : name.getCatalog()),
                    (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                    (options.tablePattern != null ? options.tablePattern : name.getName()));
        } else {
            session.err.println("Use: \\show super [tables | types] ...");
            return null;
        }
    }

    private ResultSet doTables(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() > 3) {
            session.err.println("Use: \\show tables [-e] [-T type] [[[<catalog>.]schema-pattern.]<table-pattern>]");
            return null;
        }
        if (options.essential) {
            options.columns = essentialTableCols;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 2)
                ? new SQLObjectName(ctx, options.arguments.get(1))
                : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getTables(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()),
                options.tableType == null ? null : new String[]{options.tableType});
    }

    private ResultSet doTable(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() < 2) {
            session.err.println("Use: \\show table [privs | types] ...");
        }
        String obj = options.arguments.get(1);
        if (obj.equalsIgnoreCase("privs")) {
            if (options.arguments.size() > 3) {
                session.err.println("Use: \\show table privs [[[catalog.]schema-pattern.]table-pattern]");
                return null;
            }
            SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
            SQLObjectName name = (options.arguments.size() == 3)
                    ? new SQLObjectName(ctx, options.arguments.get(2))
                    : new SQLObjectName(ctx, "%");
            DatabaseMetaData meta = con.getMetaData();
            return meta.getTablePrivileges(
                    (options.catalog != null ? options.catalog : name.getCatalog()),
                    (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                    (options.tablePattern != null ? options.tablePattern : name.getName()));
        } else if (obj.equalsIgnoreCase("types")) {
            if (options.arguments.size() > 2) {
                session.err.println("Use: \\show table types");
                return null;
            }
            DatabaseMetaData meta = con.getMetaData();
            return meta.getTableTypes();
        } else {
            session.err.println("Use: \\show table [privs [-t table-pat | " + "<table-pattern>] | types]");
            return null;
        }
    }

    private ResultSet doTypes(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 1) {
            session.err.println("Use: \\show [-e] types");
            return null;
        }
        if (options.essential) {
            options.columns = essentialTypesCols;
        }
        DatabaseMetaData meta = con.getMetaData();
        return meta.getTypeInfo();
    }

    private ResultSet doUser(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() < 2 || options.arguments.size() > 3 || !options.arguments.get(1).equalsIgnoreCase("types")) {
            session.err.println("Use: \\show user types [[[catalog.]schema-pattern.]type-pattern]");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = (options.arguments.size() == 3)
                ? new SQLObjectName(ctx, options.arguments.get(2))
                : new SQLObjectName(ctx, "%");
        DatabaseMetaData meta = con.getMetaData();
        return meta.getUDTs(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()), null);
    }

    private ResultSet doVersion(Session session, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 3) {
            session.err.println("Use: \\show version columns [[catalog.]schema-pattern.]table");
            return null;
        }
        SQLConnectionContext ctx = (SQLConnectionContext) session.getConnectionContext();
        SQLObjectName name = new SQLObjectName(ctx, options.arguments.get(2));
        DatabaseMetaData meta = con.getMetaData();
        return meta.getVersionColumns(
                (options.catalog != null ? options.catalog : name.getCatalog()),
                (options.schemaPattern != null ? options.schemaPattern : name.getSchema()),
                (options.tablePattern != null ? options.tablePattern : name.getName()));
    }

    private void doServer(Session session, Renderer renderer, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 2 || !options.arguments.get(1).equalsIgnoreCase("version")) {
            session.err.println("Use: \\show server version");
            return;
        }
        ColumnDescription[] columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Property", 20);
        columns[1] = new ColumnDescription("Value", 50);
        renderer.header(columns);
        DatabaseMetaData meta = con.getMetaData();
        addPair(renderer, "Major version", meta.getDatabaseMajorVersion());
        addPair(renderer, "Minor version", meta.getDatabaseMinorVersion());
        addPair(renderer, "Product name", meta.getDatabaseProductName());
        addPair(renderer, "Product version", meta.getDatabaseProductVersion());
        renderer.flush();
    }

    private void doDriver(Session session, Renderer renderer, Connection con, Options options) throws SQLException {
        if (options.arguments.size() != 2 || !options.arguments.get(1).equalsIgnoreCase("version")) {
            session.err.println("Use: \\show driver version");
            return;
        }
        ColumnDescription[] columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Property", 20);
        columns[1] = new ColumnDescription("Value", 50);
        renderer.header(columns);
        DatabaseMetaData meta = con.getMetaData();
        addPair(renderer, "Major version", meta.getDriverMajorVersion());
        addPair(renderer, "Minor version", meta.getDriverMinorVersion());
        addPair(renderer, "Driver name", meta.getDriverName());
        addPair(renderer, "Driver version", meta.getDriverVersion());
        renderer.flush();
    }

    private void doFeatures(Session session, Renderer renderer, Connection con) throws SQLException {
        ColumnDescription[] columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Feature", 80);
        columns[1] = new ColumnDescription("Value", 10);
        DatabaseMetaData meta = con.getMetaData();
        renderer.header(columns);
        addPair(renderer, "All procedures are callable", meta.allProceduresAreCallable());
        addPair(renderer, "All tables are selectable", meta.allTablesAreSelectable());
        addPair(renderer, "Auto-commit closes results", meta.autoCommitFailureClosesAllResultSets());
        addPair(renderer, "DDL causes commit", meta.dataDefinitionCausesTransactionCommit());
        addPair(renderer, "DDL ignored in transactions", meta.dataDefinitionIgnoredInTransactions());
        addPair(renderer, "Deletes are detected (forward)", meta.deletesAreDetected(ResultSet.TYPE_FORWARD_ONLY));
        addPair(renderer, "Deletes are detected (insensitive)", meta.deletesAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE));
        addPair(renderer, "Deletes are detected (sensitive)", meta.deletesAreDetected(ResultSet.TYPE_SCROLL_SENSITIVE));
        addPair(renderer, "Max size includes blobs", meta.doesMaxRowSizeIncludeBlobs());
        addPair(renderer, "Inserts are detected (forward)", meta.insertsAreDetected(ResultSet.TYPE_FORWARD_ONLY));
        addPair(renderer, "Inserts are detected (insensitive)", meta.insertsAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE));
        addPair(renderer, "Inserts are detected (sensitive)", meta.insertsAreDetected(ResultSet.TYPE_SCROLL_SENSITIVE));
        addPair(renderer, "Catalog in fully qualified name", meta.isCatalogAtStart());
        addPair(renderer, "Read only", meta.isReadOnly());
        renderer.flush();
    }

    private void addPair(Renderer renderer, String name, boolean value) {
        String[] row = new String[2];
        row[0] = name;
        row[1] = Boolean.toString(value);
        renderer.row(row);
    }

    private void addPair(Renderer renderer, String name, int value) {
        String[] row = new String[2];
        row[0] = name;
        row[1] = Integer.toString(value);
        renderer.row(row);
    }

    private void addPair(Renderer renderer, String name, String value) {
        String[] row = new String[2];
        row[0] = name;
        row[1] = value;
        renderer.row(row);
    }
}
