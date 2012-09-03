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

import static org.sqsh.options.ArgumentRequired.NONE;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SessionRedrawBufferMessage;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.OptionProperty;

/**
 * Implements the \select command.
 */
public class Select
    extends Command
    implements DatabaseCommand {
    
    private static class Options
        extends SqshOptions {
        
        @OptionProperty(
            option='p', longOption="print", arg=NONE,
            description="Print statement to screen, do not append to SQL buffer")
        public boolean printOnly = false;
        
        @OptionProperty(
            option='n', longOption="natural-join", arg=NONE,
            description="Create a natural join instead of a key join")
        public boolean naturalJoin = false;
        
        @Argv(program="\\select", min=1,  usage="table [table ...]")
        public List<String> arguments = new ArrayList<String>();
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        
        /*
         * Make sure the caller provided some table names.
         */
        if (options.arguments.size() == 0) {
            
            session.err.println("Use: \\select table [table ...]");
            return 1;
        }
        
        
        /*
         * First, gather up a description of every table.
         */
        Table []descriptions = new Table[options.arguments.size()];
        for (int i = 0; i < descriptions.length; i++) {
            
            try {
                
                descriptions[i] = describe(session, options.arguments.get(i));
                descriptions[i].alias = Character.toString((char) ('a' + i));
            }
            catch (SQLException e) {
                
                session.err.println("Failed to fetch column descriptions for "
                    + "table " + options.arguments.get(i) + ": " 
                    + e.getMessage());
                return 1;
            }
            
            /*
             * If no columns where found for a given table, that means
             * it couldn't be found.
             */
            if (descriptions[i].columns.length == 0) {
                
                session.err.println("Unable to find description of table "
                    + options.arguments.get(i));
                return 1;
            }
        }
        
        /*
         * Now we can start trying to build our query!
         */
        String linesep = System.getProperty("line.separator");
        StringBuilder query = new StringBuilder();
        
        /*
         * Build the select list.
         */
        query.append("SELECT ");
        for (int i = 0; i < descriptions.length; i++) {
            
            Table table = descriptions[i];
            for (int j = 0; j < table.columns.length; j++) {
                
                if (i > 0 || j > 0) {
                
                    query.append(',')
                        .append(linesep)
                        .append("       ");
                }
                
                query.append(table.alias)
                    .append('.')
                    .append(SQLTools.quoteIdentifier(table.columns[j].name));
            }
        }
        
        /*
         * Next, build the FROM clause.
         */
        query.append(linesep).append("  FROM ");
        for (int i = 0; i < descriptions.length; i++) {
            
            Table table = descriptions[i];
            if (i > 0) {
                
                query.append(", ");
            }
            
            query.append(table.name)
                .append(' ')
                .append(table.alias);
        }
        
        /*
         * Finally, the WHERE clause.
         */
        if (descriptions.length > 1) {
            
            String join = null;
            
            if (options.naturalJoin) {
                
                join = getNaturalJoin(descriptions, linesep);
            }
            else {
                
                try {
                    
                    join = getKeyJoin(session, descriptions, linesep);
                }
                catch (SQLException e) {
                    
                    SQLTools.printException(session, e);
                    return 1;
                }
            }
            
            if (join.length() > 0) {
                
                query.append(linesep)
                    .append(" WHERE ")
                    .append(join);
            }
        }
        
        
        /*
         * Ok, we are finished. Now lets update the query buffer.
         */
        if (options.printOnly) {
            
            session.out.println(query.toString());
        }
        else {
            
            session.getBufferManager().getCurrent().set(query.toString());
            throw new SessionRedrawBufferMessage();
        }
        
        return 0;
    }
    
    /**
     * Attempts to fetch a join clause by asking the database about
     * primary/foriegn key relationships between tables.
     * 
     * @param session The session.
     * @param tables The tables to join
     * @param linesep Linesep
     * @return The join
     * @throws SQLException
     */
    private String getKeyJoin(Session session,
            Table []tables, String linesep)
        throws SQLException {
        
        Connection conn = session.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        
        StringBuilder join = new StringBuilder();
        int joinCount = 0;
        
        for (int t1 = 0; t1 < tables.length; ++t1) {
                
            Table table1 = tables[t1];
            for (int t2 = 0; t2 < tables.length; ++t2) {
                    
                Table table2 = tables[t2];
                
                ResultSet rs = meta.getCrossReference(
                        table1.catalog,
                        table1.schema,
                        table1.tableName,
                        table2.catalog,
                        table2.schema,
                        table2.tableName
                    );
                
                while (rs.next()) {
                    
                    if (joinCount > 0) {
                                    
                        join.append(linesep)
                            .append("   AND ");
                    }
                    
                    join.append(table1.alias)
                        .append('.')
                        .append(SQLTools.quoteIdentifier(rs.getString(4)))
                        .append(" = ")
                        .append(table2.alias)
                        .append('.')
                        .append(SQLTools.quoteIdentifier(rs.getString(8)));
                    
                    ++joinCount;
                }
            }
        }
        
        return join.toString();
    }
    
    /**
     * Generates a "natural" join between tables...that is, it attempts
     * to join all columns of the same name and datatype.
     * 
     * @param tables The tables.
     * @param linesep The line separator
     * @return The join.
     */
    private String getNaturalJoin(Table []tables, String linesep) {
        
        StringBuilder join = new StringBuilder();
        int joinCount = 0;
        
        for (int t1 = 0; t1 < tables.length; ++t1) {
                
            Table table1 = tables[t1];
            for (int t2 = t1 + 1; t2 < tables.length; ++t2) {
                    
                Table table2 = tables[t2];
                for (Column c1 : table1.columns) {
                        
                    for (Column c2 : table2.columns) {
                            
                        if (c1.name.equals(c2.name)
                                && c1.type == c2.type) {
                                
                            if (joinCount > 0) {
                                    
                                join.append(linesep)
                                    .append("   AND ");
                            }
                                
                            join.append(table1.alias)
                                .append('.')
                                .append(SQLTools.quoteIdentifier(c1.name))
                                .append(" = ")
                                .append(table2.alias)
                                .append('.')
                                .append(SQLTools.quoteIdentifier(c2.name));
                                
                            ++joinCount;
                        }
                    }
                }
            }
        }
        
        return join.toString();
    }
    
    /**
     * Helper method to fetch the description of a table.
     * 
     * @param session The session 
     * @param name The name of the table.
     * @return A description of the table.
     * @throws SQLException Thrown if things don't work out so good.
     */
    private Table describe (Session session, String name)
        throws SQLException {
        
        Connection conn = session.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        SQLTools.ObjectDescription nameDescription = 
            SQLTools.parseObjectName(name);
        
        Table table = new Table();
        table.name = name;
        
        ResultSet result = meta.getColumns(nameDescription.getCatalog(),
            nameDescription.getSchema(), nameDescription.getName(), "%");
        
        ArrayList<Column> cols = new ArrayList<Column>();
        while (result.next()) {
            
            Column col = new Column();
            table.catalog = result.getString(1);
            table.schema = result.getString(2);
            table.tableName = result.getString(3);
            col.name = result.getString(4);
            col.type = result.getInt(5);
            
            cols.add(col);
        }
        
        table.columns = cols.toArray(new Column[0]);
        return table;
    }
    
    private static class Table {
        
        public String   name;
        public String   catalog;
        public String   tableName;
        public String   schema;
        public String   owner;
        public String   alias;
        public Column []columns;
    }
    
    private static class Column {
        
        public String name;
        public int type;
    }
}
