/*
 * Copyright 2007-2012 Scott C. Gray, Ryan Stouffer
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
package org.sqsh.renderers;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;
import org.sqsh.AliasManager;
import org.sqsh.Alias;

/**
 * Outputs results as delimeted values. This class attempts to
 * completly mimic the behavior of the CSVRenderer but add a
 * feature set to allow for more types of delimiters and other
 * style features like 
 * marker - a string to wrap values, normally a "
 * delimiter - a string to separate values, normally a ,
 * newline - the value to separate the lines, normally a \n
 * escape - the value to use if a marker is found in a string, normally a ""
 * set the values by setting the session variables
 */
public class DelimiterRenderer
    extends Renderer {
    
    private StringBuilder line = new StringBuilder(); 

    public static String MARKER = "\"";
    public static String DELIMITER = ",";
    public static String NEWLINE = "\n";
    

    private String marker = MARKER;
    private String delimiter = DELIMITER;
    private String newline = NEWLINE;
    private String escape = MARKER+MARKER;
    
    public DelimiterRenderer(Session session, RendererManager renderMan) {
 
        super(session, renderMan);
    }

    /* this should only be called once during header generation to allow for changes to the aliases */
    private void setValues() {

        AliasManager aliasManager = session.getAliasManager();

        Alias aliasMarker = aliasManager.getAlias("marker");
        Alias aliasNewline = aliasManager.getAlias("newline");    
        Alias aliasDelimiter = aliasManager.getAlias("delimiter");    
        Alias aliasEscape = aliasManager.getAlias("escape");    
   
        if (aliasMarker != null) {        
            marker = fixBackSlashes(aliasMarker.getText());
        }
        else {
            marker = MARKER;
        }

        if (aliasNewline != null) {      
            newline = fixBackSlashes(aliasNewline.getText());
        }
        else {

            newline = NEWLINE;
        }

        if (aliasDelimiter != null) {      
            delimiter = fixBackSlashes(aliasDelimiter.getText());
        }
        else {
            delimiter = DELIMITER;
        }

        if (aliasEscape != null) { 
            escape = fixBackSlashes(aliasEscape.getText());
        }
        if (aliasDelimiter != null) {

            escape = marker+marker;
        }
    }

 	/*I am being super lazy here.  Apache commons has a render
          that will rerender all escaped characters, but like I said
          lazy.   I am not sure if the code is worth it */
    private String fixBackSlashes(String val) {

        return val.replace("\\n","\n")
                .replace("\\t","\t")
                .replace("\\\\","\\")
                .replace("\\r","\r"); 
    }

    public void header (ColumnDescription []columns) {
        
        super.header(columns);
        
        setValues();
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
    
    private boolean needsQuoting(String str) {
        
        final int sz = str.length();
        
        if (sz == 0) {
            
            return true;
        }
        
        if (Character.isWhitespace(str.charAt(0))
            || Character.isWhitespace(str.charAt(sz -1))) {
            
            return true;
        }
        
        return str.contains(marker) || str.contains(newline) || str.contains(delimiter);
    }

    /** {@inheritDoc} */
    @Override
    public boolean row (String[] row) {

        line.setLength(0);

        for (int i = 0; i < row.length; i++) {
            
            String field = row[i];

            if (i > 0) {

                 line.append(delimiter);
            }
            
            if (!isNull(field)) {
                
                if (!needsQuoting(field)) {
                    
                    line.append(field);
                }
                else {
                    
                    line.append(marker);
                    line.append(field.replace(marker, escape));
                    line.append(marker);
                }
            }
        }
        
        session.out.print(line);
        session.out.print(newline);

        return (session.out.checkError() == false);
    }
    
    @Override
    public void footer (String footer) {
        
        /*
         * This style will never display footer information.
         */
    }
}
