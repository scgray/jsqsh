package org.sqsh.renderers;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * Renders INSERT statements based upon a result set.
 */
public class InsertRenderer
    extends Renderer {

    private String table = "TABLE";
    private StringBuilder sb = new StringBuilder();

    public InsertRenderer(Session session, RendererManager manager) {

        super(session, manager);

        String tab = session.getVariable("insert_table");
        if (tab != null) {

            table = tab;
        }
    }

    /* (non-Javadoc)
     * @see org.sqsh.Renderer#header(org.sqsh.ColumnDescription[])
     */
    @Override
    public void header (ColumnDescription[] columns) {

        super.header(columns);
    }
    
    @Override
    public boolean row (String[] row) {
        
        sb.setLength(0);
        sb.append("INSERT ")
            .append(table)
            .append(" (");

        for (int i = 0; i < columns.length; i++) {
            
            ColumnDescription col = columns[i];
            
            if (i > 0) {
                
                sb.append(", ");
            }
            
            sb.append(col.getName());
        }
        
        sb.append(") VALUES (");
        
        for (int i = 0; i < row.length; i++) {
            
            if (i > 0) {
                
                sb.append(", ");
            }
            
            ColumnDescription col = columns[i];
            if (col.getType() != ColumnDescription.Type.STRING) {
                
                sb.append(row[i]);
            }
            else {
                
                sb.append('"').append(row[i]).append('"');
            }
        }
        sb.append(")");
        
        session.out.println(sb.toString());
        return (session.out.checkError() == false);
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

        /* NO FOOTERS */
    }

}
