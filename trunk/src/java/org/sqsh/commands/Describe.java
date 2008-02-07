package org.sqsh.commands;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.Renderer;
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;
import org.sqsh.SqshOptions;

/**
 * Implements the \describe command.
 */
public class Describe
    extends Command
    implements DatabaseCommand {
    
    private static class Options
        extends SqshOptions {
        
        @Option(name="-a",
                usage="Shows all available information")
            public boolean showAll = false;
    }
    
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options) opts;
        String type = null;
        if (options.arguments.size() != 1) {
            
            session.err.println("Use: \\describe [-a] table_name");
            return 1;
        }
        
        Connection con = session.getConnection();
        ResultSet result = null;
        try {
            
            String catalog = null;
            String schema = null;
            String name = null;
            String []parts = options.arguments.get(0).split("\\.");
            
            if (parts.length == 1) {
                
                name = parts[0];
                schema = "%";
                catalog = con.getCatalog();
            }
            else if (parts.length == 2) {
                
                schema = parts[0];
                if (parts[1].equals("")) {
                    
                    name = "%";
                }
                else {
                    
                    name = parts[1];
                }
                catalog = con.getCatalog();
            }
            else {
                
                catalog = parts[0];
                schema = parts[1];
                name = parts[2];
            }
            
            DatabaseMetaData meta = con.getMetaData();
            HashSet<Integer> cols = null;
            
            if (isTable(meta, catalog, schema, name)) {
                
                if (options.showAll == false) {
                    
                    cols = new HashSet<Integer>();
                    cols.add(2); /* owner */
                    cols.add(4); /* column */
                    cols.add(6); /* type */
                    cols.add(7); /* size */
                    cols.add(9); /* decimal digits */
                    cols.add(18); /* nullable? */
                }
                
                result = meta.getColumns(catalog, schema, name, null);
            }
            else {
                
                if (options.showAll == false) {
                    
                    cols = new HashSet<Integer>();
                    cols.add(2); /* owner */
                    cols.add(4); /* column */
                    cols.add(7); /* type */
                    cols.add(8); /* precision */
                    cols.add(9); /* length */
                    cols.add(10); /* scale */
                }
                
                result = meta.getProcedureColumns(catalog, schema, name, null);
            }
            
            SQLRenderer sqlRenderer = session.getSQLRenderer();
            Renderer renderer = session.getRendererManager()
                .getCommandRenderer(session);
            sqlRenderer.displayResults(renderer, session, result, cols);
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
    
    /**
     * Does a crappy test to determine of an object name is a table.
     * @param meta Database metadata descriptor.
     * @param catalog The database name
     * @param schema The schema of the object
     * @param name The name of the object.
     * @return True if it is a table.
     */
    private boolean isTable(DatabaseMetaData meta, String catalog,
            String schema, String name) {
        
        int nRows = 0;
        
        try {
            
            ResultSet result = meta.getColumns(catalog, schema, name, null);
            while (result.next()) {
                
                ++nRows;
            }
        }
        catch (SQLException e) {
            
            /* IGNORED */
        }
        
        return (nRows > 0);
    }
    
}
