package org.sqsh.renderers;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;

/**
 * Outputs results as CSV (comma separated values). This class attempts to
 * adhere to <a href="http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm">
 * http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm</a>.
 */
public class CSVRenderer
    extends Renderer {
    
    public CSVRenderer(Session session, RendererManager renderMan) {
        
        super(session, renderMan);
    }
    
    public void header (ColumnDescription []columns) {
        
        super.header(columns);
        
        if (manager.isShowHeaders()) {
            
            String []row = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                
                row[i] = columns[i].getName();
                if (row[i] == null) {
                    
                    row[i] = "";
                }
            }
            
            row(row);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean flush () {

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean row (String[] row) {

        StringBuffer line = new StringBuffer();
        for (int i = 0; i < row.length; i++) {
            
            if (i > 0) {
                
                line.append(',');
            }
            
            String field = row[i];
            boolean needsQuoting = 
                (field.indexOf('"') >= 0
                    || field.indexOf('\n') >= 0
                    || field.indexOf(',') >= 0
                    || (field.length() > 0 
                            && (Character.isWhitespace(field.charAt(0))
                                  || Character.isWhitespace(field.charAt(field.length() -1)))));
            
            if (needsQuoting == false) {
                
                line.append(field);
            }
            else {
                
                line.append('"');
                for (int j = 0; j < field.length(); j++) {
                    
                    char ch = field.charAt(j);
                    if (ch == '"') {
                        
                        line.append('"');
                    }
                    
                    line.append(ch);
                }
                
                line.append('"');
            }
        }
        
        session.out.println(line);
        return (session.out.checkError() == false);
    }
    
    @Override
    public void footer (String footer) {
        
        /*
         * This style will never display footer information.
         */
    }
}
