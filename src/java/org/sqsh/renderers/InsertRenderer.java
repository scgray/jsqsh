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
    private String insert = null;

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

        if (insert != null) {

           return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT ")
          .append(table)
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
            
            int len = name.length();
            boolean needQuotes = false;
            for (int j = 0; needQuotes == false && j < len; j++) {
                
                char ch = name.charAt(j);
                if (!(Character.isLetter(ch)
                        || Character.isDigit(ch)
                        || ch == '_')) {
                    
                    needQuotes = true;
                }
            }
            
            if (needQuotes) {
                
                sb.append('"').append(name).append('"');
            }
            else {
                
                sb.append(name);
            }
        }
        
        sb.append(") VALUES (");
        insert = sb.toString();
    }
    
    @Override
    public boolean row (String[] row) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(insert);
        
        for (int i = 0; i < row.length; i++) {
            
            if (i > 0) {
                
                sb.append(", ");
            }
            
            ColumnDescription col = columns[i];
            if (col.getType() != ColumnDescription.Type.STRING) {
                
                sb.append(row[i]);
            }
            else {
                
                sb.append('\'').append(quote(row[i])).append('\'');
            }
        }
        sb.append(")");
        
        session.out.println(sb.toString());
        return (session.out.checkError() == false);
    }
    
    /**
     * Protects single quotes in a string.
     * 
     * @param str The string that may or may not have single quotes.
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

        /* NO FOOTERS */
    }

}
