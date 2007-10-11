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
import org.sqsh.Renderer;
import org.sqsh.SQLContext;
import org.sqsh.SQLRenderer;
import org.sqsh.Session;

/**
 * Implements the \tables command.
 */
public class Tables
    extends Command {
    
    private static class Options {
        
        @Option(name="-p",
                usage="Provides a pattern to match against table names")
            public String tablePattern = "%";
        
        @Option(name="-s",
                usage="Provides a pattern to match against schema (owner) names")
            public String schemaPattern = "%";
        
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
        
        if (options.arguments.size() > 0) {
            
            type = options.arguments.get(0);
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
            
            String []types = null;
            if (type != null) {
                
                types = new String[1];
                if ("TABLE".equals(type.toUpperCase())
                    || "USER".equals(type.toUpperCase())) {
                    
                    types[0] = "TABLE";
                }
                else if ("VIEW".equals(type.toUpperCase())) {
                    
                    types[0] = "VIEW";
                }
                else if ("SYSTEM".equals(type.toUpperCase())) {
                    
                    types[0] = "SYSTEM TABLE";
                }
                else if ("ALIAS".equals(type.toUpperCase())) {
                    
                    types[0] = "ALIAS";
                }
                else if ("SYNONYM".equals(type.toUpperCase())) {
                    
                    types[0] = "SYNONYM";
                }
                else {
                    
                    session.err.println("The object type '" 
                        + type + "' is not recognized. "
                        + "Valid types are: user, system, view, "
                        + "alias, synonym");
                    return 1;
                }
            }
            
            
            DatabaseMetaData meta = con.getMetaData();
            
            /*
             * We don't want to show all columns.
             */
            HashSet<Integer> cols = new HashSet<Integer>();
            cols.add(2); /* Owner */
            cols.add(3); /* Table name */
            cols.add(4); /* Table Type */
            
            result = meta.getTables(con.getCatalog(),
                options.schemaPattern, options.tablePattern, types);
            
            SQLRenderer sqlRenderer = session.getSQLRenderer();
            Renderer renderer = 
                session.getRendererManager().getCommandRenderer(session);
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

}
