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
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;

/**
 * Implements the \describe command.
 */
public class Describe
    extends Command {
    
    private static class Options {
        
        @Option(name="-a",
                usage="Shows all available information")
            public boolean showAll = false;
        
        @Argument
            public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public int execute (Session session, String[] argv)
        throws Exception {
        
        Options options = new Options();
        String type = null;
        int rc = parseOptions(session, argv, options);
        if (rc != 0) {
            
            return rc;
        }
        
        if (options.arguments.size() != 1) {
            
            session.err.println("Use: \\describe [-a] table_name");
            return 1;
        }
        
        SQLContext context = session.getSQLContext();
        if (context == null) {
            
            session.err.println("No database connection has been established."
                + " Use the \\connect command to create a connection.");
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
            SQLRenderer renderer = session.getSQLRenderer();
            renderer.displayResults(session, result, cols);
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
}
