package org.sqsh.renderers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.SQLTools;
import org.sqsh.Session;

/**
 * Renders INSERT statements based upon a result set.
 */
public class InsertRenderer
    extends Renderer {

    private String table = "TABLE";
    private String insert = null;
    private int batchSize = 50;
    private Connection conn = null;
    private StringBuilder insertBatch = new StringBuilder();
    
    private int rowCount = 0;
    private String nullRepresentation = null;

    public InsertRenderer(Session session, RendererManager manager) {

        super(session, manager);
        
        /*
         * This is used below in a hack to determine if I am looking
         * at the NULL string representation used by the data formatter.
         */
        nullRepresentation = session.getDataFormatter().getNull();

        String tab = session.getVariable("insert_table");
        if (tab != null) {

            table = tab;
        }
    }
    
    /**
     * @return the name of the table that will be used
     * in the INSERT statements.
     */
    public String getTable () {
    
        return table;
    }
    
    /**
     * @param table The table name that will be used for the
     * INSERT statements.
     */
    public void setTable (String table) {
    
        this.table = table;
    }
    
    /**
     * @return Returns the number of rows that will be  inserted 
     * before the batch is executed.
     */
    public int getBatchSize () {
    
        return batchSize;
    }
    
    /**
     * @param batchSize The number of rows that will be inserted before the 
     * batch is executed.
     */
    public void setBatchSize (int batchSize) {
    
        this.batchSize = batchSize;
    }
    
    /**
     * This method provides a database connection to the renderer that will
     * be used to actually execute the INSERT statements. If no connection
     * is provided, the statements will be printed to the screen instead.
     * 
     * @param conn the connection.
     */
    public void setConnection (Connection conn) {
    
        this.conn = conn;
    }


    /* (non-Javadoc)
     * @see org.sqsh.Renderer#header(org.sqsh.ColumnDescription[])
     */
    @Override
    public void header (ColumnDescription[] columns) {

        super.header(columns);

        if (insert != null) {

           return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ")
          .append(SQLTools.quoteIdentifier(table))
          .append(" (");
        
        for (int i = 0; i < columns.length; i++) {
            
            ColumnDescription col = columns[i];
            
            if (i > 0) {
                
                sb.append(", ");
            }
            
            String name = col.getName();
            if (name == null) {
                
                name = "NONAME";
            }
            
            sb.append(SQLTools.quoteIdentifier(name));
        }
        
        sb.append(") VALUES (");
        insert = sb.toString();
    }
    
    @Override
    public boolean row (String[] row) {
        
        boolean ok = true;
        StringBuilder sb = new StringBuilder();
        sb.append(insert);
        
        for (int i = 0; i < row.length; i++) {
            
            /*
             * COMPLETE AND UTTER HACK: The values for the row
             * have been formatted for us by our caller (presumably
             * the SQLRenderer, by using the DataFormatter class). Because
             * of this, NULL values will be returned as a string that was
             * provided by the DataFormatter.getNull() method. Because of this
             * we need to "reverse" this process and actually spit a NULL
             * into our INSERT statement. 
             * 
             * This is a hack because if the current NULL representation is
             * too common a value, we may accidentally null a column that 
             * wasn't really null. This could be worked around, but would
             * require more effort than I really want to put in.
             */
            if (row[i] != null
                    && nullRepresentation.equals(row[i])) {
                
                row[i] = null;
            }
            
            if (i > 0) {
                
                sb.append(", ");
            }
            
            if (row[i] == null) {
                
                sb.append("NULL");
            }
            else {
            
                ColumnDescription col = columns[i];
            	if (col.getType() != ColumnDescription.Type.STRING) {
                
                	sb.append(row[i]);
            	}
            	else {
                
                	sb.append('\'').append(quote(row[i])).append('\'');
            	}
            }
        }
        sb.append(")");
        
        ++rowCount;
        ok = insertRow(sb.toString());
        if (ok && (rowCount % batchSize) == 0) {
            
            ok = insertGo();
        }
        
        return ok;
    }
    
    /**
     * Attempts to execute the INSERT statement(s) against
     * a database connection.
     * 
     * @param str String containing an insert statement. 
     * @return true if the insert succeeded, false otherwise
     */
    private boolean insertRow (String str) {
        
        /*
         * If there is no connection, then just print the
         * INSERT statement to the screen.
         */
        if (conn == null) {
            
            session.out.println(str);
            return (session.out.checkError() == false);
        }
        
        /*
         * We have a connection, so buffer the statement.
         */
        if (insertBatch.length() > 0) {
            
            insertBatch.append('\n');
        }
        insertBatch.append(str);
        return true;
    }
        
    /**
     * Called when the batch is to be executed via a "go".
     * 
     * @return true if it worked, false otherwise.
     */
    private boolean insertGo() {
        
        /*
         * If there is no connection, then just print the
         * word "go" to the screen.
         */
        if (conn == null) {
            
            session.out.println("go");
            return true;
        }
        
        /*
         * If our batch is empty then nothing to do.
         */
        if (insertBatch.length() == 0) {
            
            return true;
        }
        
        /*
         * Otherwise, attempt to execute.
         */
        try {
            
            Statement statement = conn.createStatement();
            statement.execute(insertBatch.toString());
            statement.getUpdateCount();
            statement.close();
            
            conn.commit();
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.err, e);
            
            insertBatch.setLength(0);
            return false;
        }
        
        insertBatch.setLength(0);
        return true;
    }
    
    /**
     * Protects single quotes in a string.
     * 
     * @param str
     *            The string that may or may not have single quotes.
     * @return The string with the quotes escaped.
     */
    private String quote (String str) {
        
        if (str.indexOf('\'') < 0)  {
            
            return str;
        }
        
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            
            char ch = str.charAt(i);
            if (ch == '\'') {
                
                sb.append("''");
            }
            else {
                
                sb.append(ch);
            }
        }
        
        return sb.toString();
    }

    @Override
    public boolean flush () {

        return true;
    }


    /* (non-Javadoc)
     * @see org.sqsh.Renderer#footer(java.lang.String)
     */
    @Override
    public void footer (String footer) {

        insertGo();
    }
}