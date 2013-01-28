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
package org.sqsh.commands;

import static org.sqsh.options.ArgumentRequired.REQUIRED;
import static org.sqsh.options.ArgumentRequired.NONE;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sqsh.ColumnDescription;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.Renderer;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

public class Show 
    extends Command
    implements DatabaseCommand {
    
    static Set<Integer> essentialTableCols = new HashSet<Integer>();
    static {
        essentialTableCols.add(1); /* TABLE_CAT */
        essentialTableCols.add(2); /* TABLE_SCHEM */
        essentialTableCols.add(3); /* TABLE_NAME */
        essentialTableCols.add(4); /* TABLE_TYPE */
    }
    
    static Set<Integer> essentialColumnCols = new HashSet<Integer>();
    static {
        essentialColumnCols.add(1); /* TABLE_CAT */
        essentialColumnCols.add(2); /* TABLE_SCHEM */
        essentialColumnCols.add(3); /* TABLE_NAME */
        essentialColumnCols.add(4); /* COLUMN_NAME */
        essentialColumnCols.add(6); /* TYPE_NAME */
        essentialColumnCols.add(7); /* COLUMN_SIZE */
        essentialColumnCols.add(9); /* DECIMAL_DIGITS */
        essentialColumnCols.add(11); /* NULLABLE */
    }
    
    static Set<Integer> essentialFunctionParamsCols = new HashSet<Integer>();
    static {
        essentialFunctionParamsCols.add(1); /* FUNCTION_CAT */
        essentialFunctionParamsCols.add(2); /* FUNCTION_SCHEM */
        essentialFunctionParamsCols.add(3); /* FUNCTION_NAME */
        essentialFunctionParamsCols.add(4); /* COLUMN_NAME */
        essentialFunctionParamsCols.add(5); /* COLUMN_TYPE */
        essentialFunctionParamsCols.add(7); /* TYPE_NAME */
        essentialFunctionParamsCols.add(8); /* PRECISION */
        essentialFunctionParamsCols.add(9); /* LENGTH */
        essentialFunctionParamsCols.add(10);/* SCALE */
        essentialFunctionParamsCols.add(12);/* NULLABLE */
    }
    
    static Set<Integer> essentialFunctionCols = new HashSet<Integer>();
    static {
        essentialFunctionCols.add(1); /* FUNCTION_CAT */
        essentialFunctionCols.add(2); /* FUNCTION_SCHEM */
        essentialFunctionCols.add(3); /* FUNCTION_NAME */
        essentialFunctionCols.add(5); /* FUNCTION_TYPE */
        essentialFunctionCols.add(6); /* SPECIFIC_NAME */
    }
    
    static Set<Integer> essentialProcedureParamsCols = new HashSet<Integer>();
    static {
        essentialProcedureParamsCols.add(1); /* PROCEDURE_CAT */
        essentialProcedureParamsCols.add(2); /* PROCEDURE_SCHEM */
        essentialProcedureParamsCols.add(3); /* PROCEDURE_NAME */
        essentialProcedureParamsCols.add(4); /* COLUMN_NAME */
        essentialProcedureParamsCols.add(5); /* COLUMN_TYPE */
        essentialProcedureParamsCols.add(7); /* TYPE_NAME */
        essentialProcedureParamsCols.add(8); /* PRECISION */
        essentialProcedureParamsCols.add(9); /* LENGTH */
        essentialProcedureParamsCols.add(10);/* SCALE */
        essentialProcedureParamsCols.add(12);/* NULLABLE */
        essentialProcedureParamsCols.add(20);/* SPECIFIC_NAME */
    }
    
    static Set<Integer> essentialTypesCols = new HashSet<Integer>();
    static {
        essentialTypesCols.add(1); /* TYPE_NAME */
        essentialTypesCols.add(3); /* PRECISION */
        essentialTypesCols.add(3); /* PROCEDURE_NAME */
        essentialTypesCols.add(7); /* NULLABLE */
        essentialTypesCols.add(12);/* AUTO_INCREMENT */
    }
    
    private static class Options
        extends SqshOptions {
        
        @OptionProperty(
            option='e', longOption="essential", arg=NONE, 
            description="Restricts columns to the most esstential columns")
        public boolean essential = false;
        
        @OptionProperty(
            option='c', longOption="catalog", arg=REQUIRED, argName="catalog",
            description="Provides the catalog in which to search")
        public String catalog = null;
        
        @OptionProperty(
            option='t', longOption="table", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against table names")
        public String tablePattern = null;
        
        @OptionProperty(
            option='s', longOption="schema", arg=REQUIRED, argName="pattern",
            description="Provides a pattern to match against schema names")
        public String schemaPattern = null;
        
        @OptionProperty(
            option='T', longOption="type", arg=REQUIRED, argName="name",
            description="Restricts the search to specific table types")
        public String tableType = null;
        
        @Argv(program="\\show", min=1, max=5,
            usage="[options] [\n" 
               + "           attributes [<pattern>]\n"
               + "         | catalogs\n"
               + "         | client info\n"
               + "         | column privs [<col-pattern>]\n"
               + "         | columns [-e] [<tab-pattern>]\n"
               + "         | driver version\n"
               + "         | exported keys [<tab-pattern>]\n"
               + "         | function params [-e] [<func-pattern>]\n"
               + "         | functions [-e] [<func-pattern>]\n"
               + "         | imported keys [<tab-pattern>]\n"
               + "         | primary keys [<tab-pattern>]\n"
               + "         | procedure params [-e] [<proc-pattern>]\n"
               + "         | procedures [<proc-pattern>]\n"
               + "         | server version\n"
               + "         | schemas [<schema-pattern>]\n"
               + "         | super tables [<tab-pattern>]\n"
               + "         | super types [<type-pattern>]\n"
               + "         | table privs [<tab-pattern>]\n"
               + "         | tables [-e] [<tab-pattern>]\n"
               + "         | table types\n"
               + "         | types [-e]\n"
               + "         | user types [<type-pattern]\n"
               + "         | version columns <table>\n"
               + "       ]"
               )
        public List<String> arguments = new ArrayList<String>();
        
        /*
         * This isn't an argument. It is used by each of the sub commands to
         * set a set of columns that they wish to have displayed.
         */
        public Set<Integer> columns = null;
    }
    
    @Override
    public SqshOptions getOptions() {
    
        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        
        Options options = (Options) opts;
        
        
        Connection con = session.getConnection();
        String subCommand = options.arguments.get(0);
        
        if ("features".equalsIgnoreCase(subCommand)) {
            
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
            doFeatures(session, renderer, con);
            return 0;
        }
        else if ("server".equalsIgnoreCase(subCommand)) {
            
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
            doServer(session, renderer, con, options);
            return 0;
        }
        else if ("driver".equalsIgnoreCase(subCommand)) {
            
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
            doDriver(session, renderer, con, options);
            return 0;
        }
        
        Renderer renderer = 
            session.getRendererManager().getCommandRenderer(session);
        ResultSet result = null;
        
        try {
            
            if ("tables".equalsIgnoreCase(subCommand)) {
                
                result = doTables(session, con, options);
            }
            else if ("attributes".equalsIgnoreCase(subCommand)) {
                
                result = doAttributes(session, con, options);
            }
            else if ("catalogs".equalsIgnoreCase(subCommand)) {
                
                result = doCatalogs(session, con, options);
            }
            else if ("client".equalsIgnoreCase(subCommand)) {
                
                result = doClient(session, con, options);
            }
            else if ("column".equalsIgnoreCase(subCommand)) {
                
                result = doColumn(session, con, options);
            }
            else if ("columns".equalsIgnoreCase(subCommand)) {
                
                result = doColumns(session, con, options);
            }
            else if ("exported".equalsIgnoreCase(subCommand)) {
                
                result = doExportedKeys(session, con, options);
            }
            else if ("function".equalsIgnoreCase(subCommand)) {
                
                result = doFunction(session, con, options);
            }
            else if ("functions".equalsIgnoreCase(subCommand)) {
                
                result = doFunctions(session, con, options);
            }
            else if ("imported".equalsIgnoreCase(subCommand)) {
                
                result = doImportedKeys(session, con, options);
            }
            else if ("primary".equalsIgnoreCase(subCommand)) {
                
                result = doPrimaryKeys(session, con, options);
            }
            else if ("procedure".equalsIgnoreCase(subCommand)) {
                
                result = doProcedure(session, con, options);
            }
            else if ("procedures".equalsIgnoreCase(subCommand)) {
                
                result = doProcedures(session, con, options);
            }
            else if ("schemas".equalsIgnoreCase(subCommand)) {
                
                result = doSchemas(session, con, options);
            }
            else if ("super".equalsIgnoreCase(subCommand)) {
                
                result = doSuper(session, con, options);
            }
            else if ("table".equalsIgnoreCase(subCommand)) {
                
                result = doTable(session, con, options);
            }
            else if ("types".equalsIgnoreCase(subCommand)) {
                
                result = doTypes(session, con, options);
            }
            else if ("user".equalsIgnoreCase(subCommand)) {
                
                result = doUser(session, con, options);
            }
            else if ("version".equalsIgnoreCase(subCommand)) {
                
                result = doVersion(session, con, options);
            }
            else {
                
                session.err.println("Uncrecognized object type \""
                    + subCommand + "\". See \"\\help show\" for details");
                return 1;
            }
            
            if (result != null) {
                
                SQLRenderer sqlRenderer = session.getSQLRenderer();
                sqlRenderer.displayResults(renderer, session, result, 
                    options.columns);
            }
        }
        catch (SQLException e) {
            
            session.err.println("Failed to retrieve database metadata: "
                + e.getMessage());
            
            return 1;
        }
        finally {
            
            if (result != null) {
                
                try {
                    
                    result.close();
                }
                catch (SQLException e) {
                    
                    /* IGNORED */
                }
            }
        }
        
        return 0;
    }
    
    private ResultSet doAttributes(Session session, Connection con, Options options)
        throws SQLException {
        
        String attributes = null;
        if (options.arguments.size() == 2) {
            
            attributes = options.arguments.get(1);
        }
        else if (options.arguments.size() > 2) {
            
            session.err.println("Use: \\show attributes [pattern]");
            return null;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getAttributes(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.tablePattern,
            attributes);
    }
    
    private ResultSet doCatalogs(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() != 1) {
            
            session.err.println("Use: \\show catalogs");
            return null;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getCatalogs();
    }
    
    private ResultSet doClient(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() != 2 
                || !options.arguments.get(1).equalsIgnoreCase("info")) {
            
            session.err.println("Use: \\show client info");
            return null;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getClientInfoProperties();
    }
    
    private ResultSet doColumn(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2
            || options.arguments.size() > 3
            || !options.arguments.get(1).equalsIgnoreCase("privs")) {
            
            session.err.println("Use: \\show column privs <pattern>");
            return null;
        }
        
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getColumnPrivileges(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.tablePattern,
            options.arguments.size() == 3 ? options.arguments.get(2) : null);
    }
    
    private ResultSet doColumns(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() > 2) {
            
            session.err.println("Use: \\show columns <table-pattern>");
            return null;
        }
        
        if (options.essential) {
            
            options.columns = essentialColumnCols;
        }
        
        String table = options.tablePattern;
        if (options.arguments.size() == 2) {
            if (table != null) {
                
                session.err.println("Cannot provide both a table pattern and -t");
                return null;
            }
            table = options.arguments.get(1);
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getColumns(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            table, 
            null);
    }
    
    private ResultSet doExportedKeys(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2
            || options.arguments.size() > 3
            || !options.arguments.get(1).equalsIgnoreCase("keys")) {
            
            session.err.println("Use: \\show exported keys [<table-pattern>]");
            return null;
        }
        
        String table = options.tablePattern;
        if (options.arguments.size() == 3) {
            if (table != null) {
                
                session.err.println("Cannot provide both a table name and -t");
                return null;
            }
            table = options.arguments.get(2);
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getExportedKeys(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            table);
    }
    
    private ResultSet doFunction(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2
            || options.arguments.size() > 3
            || !options.arguments.get(1).equalsIgnoreCase("params")) {
            
            session.err.println("Use: \\show function params [<function-pattern>]");
            return null;
        }
        
        if (options.essential) {
            
            options.columns = essentialFunctionParamsCols;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getFunctionColumns(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.arguments.size() == 3 ? options.arguments.get(2) : null,
            null);
    }
    
    private ResultSet doFunctions(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() > 2) {
            
            session.err.println("Use: \\show functions [<function-pattern>]");
            return null;
        }
        
        if (options.essential) {
            
            options.columns = essentialFunctionCols;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getFunctions(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.arguments.size() == 2 ? options.arguments.get(1) : null);
    }
    
    private ResultSet doImportedKeys(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2
            || options.arguments.size() > 3
            || !options.arguments.get(1).equalsIgnoreCase("keys")) {
            
            session.err.println("Use: \\show imported keys [<table-pattern>]");
            return null;
        }
        
        String table = options.tablePattern;
        if (options.arguments.size() == 3) {
            if (table != null) {
                
                session.err.println("Cannot provide both a table name and -t");
                return null;
            }
            table = options.arguments.get(2);
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getImportedKeys(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            table);
    }
    
    private ResultSet doPrimaryKeys(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2
            || options.arguments.size() > 3
            || !options.arguments.get(1).equalsIgnoreCase("keys")) {
            
            session.err.println("Use: \\show primary keys [<table-pattern>]");
            return null;
        }
        
        String table = options.tablePattern;
        if (options.arguments.size() == 3) {
            if (table != null) {
                
                session.err.println("Cannot provide both a table name and -t");
                return null;
            }
            table = options.arguments.get(2);
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getPrimaryKeys(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            table);
    }
    
    private ResultSet doProcedure(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2
            || options.arguments.size() > 3
            || !options.arguments.get(1).equalsIgnoreCase("params")) {
            
            session.err.println("Use: \\show procedure params [<proc-pattern>]");
            return null;
        }
        
        if (options.essential) {
            
            options.columns = essentialProcedureParamsCols;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getProcedureColumns(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.arguments.size() == 3 ? options.arguments.get(2) : null,
            null);
    }
    
    private ResultSet doProcedures(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() > 2) {
            
            session.err.println("Use: \\show procedures [<proc-pattern>]");
            return null;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getProcedures(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.arguments.size() == 2 ? options.arguments.get(1) : null);
    }
    
    private ResultSet doSchemas(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() >  2) {
            
            session.err.println("Use: \\show schemas [-c catalog] <schema-pattern>");
            return null;
        }
        
        String schema = options.schemaPattern;
        if (options.arguments.size() == 2) {
            if (schema != null) {
                
                session.err.println("Cannot provide both a schema pattern and -s");
                return null;
            }
            schema = options.arguments.get(1);
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getSchemas(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            schema
            );
    }
    
    private ResultSet doSuper(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2)  {
            session.err.println("Use: \\show super [tables | types] ...");
            return null;
        }
        
        String obj = options.arguments.get(1);
        if (obj.equalsIgnoreCase("tables")) {
            
            if (options.arguments.size() > 3) {
                
                session.err.println("Use: \\show super tables [-t tab-pattern | <tab-pattern>]");
                return null;
            }
        
            String table = options.tablePattern;
            if (options.arguments.size() == 3) {
                if (table != null) {
                
                    session.err.println("Cannot provide both a table pattern and -t");
                    return null;
                }
                table = options.arguments.get(2);
            }
        
            DatabaseMetaData meta = con.getMetaData();
            return meta.getSuperTables(
                (options.catalog == null ? con.getCatalog() : options.catalog),
                options.schemaPattern,
                table);
        }
        else if (obj.equalsIgnoreCase("types")) {
            
            if (options.arguments.size() > 3) {
                
                session.err.println("Use: \\show super types [<type-pattern>]");
                return null;
            }
        
            DatabaseMetaData meta = con.getMetaData();
            return meta.getSuperTypes(
                (options.catalog == null ? con.getCatalog() : options.catalog),
                options.schemaPattern,
                options.arguments.size() == 3 ? options.arguments.get(2) : null);
        }
        else {
            
            session.err.println("Use: \\show super [tables | types] ...");
            return null;
        }
    }
    
    private ResultSet doTables(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() > 3) {
            
            session.err.println("Use: \\show tables [-t table-pattern | <table-pattern>]");
            return null;
        }
        
        if (options.essential) {
            
            options.columns = essentialTableCols;
        }
        
        String table = options.tablePattern;
        if (options.arguments.size() == 2) {
            if (table != null) {
                
                session.err.println("Cannot provide both a table pattern and -t");
                return null;
            }
            table = options.arguments.get(1);
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getTables(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            table,
            options.tableType == null ? null : new String [] { options.tableType });
    }
    
    private ResultSet doTable(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2) {
            session.err.println("Use: \\show table [privs | types] ...");
        }
        
        String obj = options.arguments.get(1);
        
        if (obj.equalsIgnoreCase("privs"))
        {
            if (options.arguments.size() > 3) {
                session.err.println("Use: \\show table privs [<table-pattern>]");
                return null;
            }
            
            String table = options.tablePattern;
            if (options.arguments.size() == 3) {
                if (table != null) {
                    
                    session.err.println("Cannot provide both a table pattern and -t");
                    return null;
                }
                table = options.arguments.get(2);
            }
            
            DatabaseMetaData meta = con.getMetaData();
            return meta.getTablePrivileges(
                (options.catalog == null ? con.getCatalog() : options.catalog),
                options.schemaPattern,
                table);
        }
        else if (obj.equalsIgnoreCase("types")) {
            
            if (options.arguments.size() > 2) {
                session.err.println("Use: \\show table types");
                return null;
            }
            
            DatabaseMetaData meta = con.getMetaData();
            return meta.getTableTypes();
        }
        else {
            
            session.err.println("Use: \\show table [privs [-t table-pat | "
                + "<table-pattern>] | types]");
            return null;
        }
    }
    
    private ResultSet doTypes(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() != 1) {
            
            session.err.println("Use: \\show types");
            return null;
        }
        
        if (options.essential) {
            
            options.columns = essentialTypesCols;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getTypeInfo();
    }
    
    private ResultSet doUser(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() < 2
            || options.arguments.size() > 3
            || !options.arguments.get(1).equalsIgnoreCase("types")) {
            
            session.err.println("Use: \\show user types [<type-pattern>]");
            return null;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getUDTs(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.arguments.size() == 3 ? options.arguments.get(2) : null,
            null);
    }
    
    private ResultSet doVersion(Session session, Connection con, Options options)
        throws SQLException {
        
        if (options.arguments.size() != 3) {
            
            session.err.println("Use: \\show version columns [-c catalog] [-s pattern] table");
            return null;
        }
        
        DatabaseMetaData meta = con.getMetaData();
        return meta.getVersionColumns(
            (options.catalog == null ? con.getCatalog() : options.catalog),
            options.schemaPattern,
            options.arguments.get(2));
    }
    
    private void doServer (Session session, Renderer renderer, Connection con, Options options) 
        throws SQLException {
        
        if (options.arguments.size() != 2
            || !options.arguments.get(1).equalsIgnoreCase("version")) {
            
            session.err.println("Use: \\show server version");
            return;
        }
        
        ColumnDescription []columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Property", 20);
        columns[1] = new ColumnDescription("Value", 50);
        
        renderer.header(columns);
        
        DatabaseMetaData meta = con.getMetaData();
        addPair(renderer, "Major version", meta.getDatabaseMajorVersion());
        addPair(renderer, "Minor version", meta.getDatabaseMinorVersion());
        addPair(renderer, "Product name",  meta.getDatabaseProductName());
        addPair(renderer, "Product version", meta.getDatabaseProductVersion());
        
        renderer.flush();
    }
    
    private void doDriver (Session session, Renderer renderer, Connection con, Options options) 
        throws SQLException {
        
        if (options.arguments.size() != 2
            || !options.arguments.get(1).equalsIgnoreCase("version")) {
            
            session.err.println("Use: \\show driver version");
            return;
        }
        
        ColumnDescription []columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Property", 20);
        columns[1] = new ColumnDescription("Value", 50);
        
        renderer.header(columns);
        
        DatabaseMetaData meta = con.getMetaData();
        addPair(renderer, "Major version", meta.getDriverMajorVersion());
        addPair(renderer, "Minor version", meta.getDriverMinorVersion());
        addPair(renderer, "Driver name",  meta.getDriverName());
        addPair(renderer, "Driver version", meta.getDriverVersion());
        
        renderer.flush();
    }
    
    private void doFeatures (Session session, Renderer renderer, Connection con) 
        throws SQLException {
        
        ColumnDescription []columns = new ColumnDescription[2];
        columns[0] = new ColumnDescription("Feature", 80);
        columns[1] = new ColumnDescription("Value", 10);
        
        DatabaseMetaData meta = con.getMetaData();
        
        renderer.header(columns);
        
        addPair(renderer, "All procedures are callable", meta.allProceduresAreCallable());
        addPair(renderer, "All tables are selectable", meta.allTablesAreSelectable());
        addPair(renderer, "Auto-commit closes results", meta.autoCommitFailureClosesAllResultSets());
        addPair(renderer, "DDL causes commit", meta.dataDefinitionCausesTransactionCommit());
        addPair(renderer, "DDL ignored in transactions", meta.dataDefinitionIgnoredInTransactions());
        addPair(renderer, "Deletes are detected (forward)", 
            meta.deletesAreDetected(ResultSet.TYPE_FORWARD_ONLY));
        addPair(renderer, "Deletes are detected (insensitive)", 
            meta.deletesAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE));
        addPair(renderer, "Deletes are detected (sensitive)", 
            meta.deletesAreDetected(ResultSet.TYPE_SCROLL_SENSITIVE));
        addPair(renderer, "Max size includes blobs", meta.doesMaxRowSizeIncludeBlobs());
        addPair(renderer, "Inserts are detected (forward)", 
            meta.insertsAreDetected(ResultSet.TYPE_FORWARD_ONLY));
        addPair(renderer, "Inserts are detected (insensitive)", 
            meta.insertsAreDetected(ResultSet.TYPE_SCROLL_INSENSITIVE));
        addPair(renderer, "Inserts are detected (sensitive)", 
            meta.insertsAreDetected(ResultSet.TYPE_SCROLL_SENSITIVE));
        addPair(renderer, "Catalog in fully qualified name", 
            meta.isCatalogAtStart());
        addPair(renderer, "Read only", meta.isReadOnly());
        renderer.flush();
    }
    
    private void addPair(Renderer renderer, String name, boolean value) {
        
        String row[] = new String[2];
        row[0] = name;
        row[1] = Boolean.toString(value);
        renderer.row(row);
    }
    
    private void addPair(Renderer renderer, String name, int value) {
        
        String row[] = new String[2];
        row[0] = name;
        row[1] = Integer.toString(value);
        renderer.row(row);
    }
    
    private void addPair(Renderer renderer, String name, String value) {
        
        String row[] = new String[2];
        row[0] = name;
        row[1] = value;
        renderer.row(row);
    }
}
