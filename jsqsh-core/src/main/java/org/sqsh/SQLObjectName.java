/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to parse object names like database.schema.table.   Missing pieces of
 * the name will attempt to be filled by querying the underlying JDBC driver, 
 * and names will be normalized according to the database specific name normalization
 * rules (if they are installed).
 */
public class SQLObjectName {
    
    private String catalog;
    private String schema;
    private String name;
    private List<String> extraName = null;
    private boolean isMalformed = false;
    private int currentPart = 0;
    private int maxParts;
    
    StringBuilder buffer = new StringBuilder();
    
    public SQLObjectName (SQLConnectionContext conn, String str) {
        
        this(conn, str, 3);
    }
    
    public SQLObjectName (SQLConnectionContext conn, String str, int maxParts) {
        
        this.maxParts = maxParts;
        parse(str);
        
        boolean hasExplicitCatalog = false;
        if (catalog == null || catalog.length() == 0) {
            
            try {
                
                catalog = conn.getConnection().getCatalog();
            }
            catch (SQLException e) {
                
                catalog = null;
                hasExplicitCatalog = true;
            }
        }
        else {
            
            catalog =  conn.normalizeIdentifier(catalog);
        }
        
        if (schema == null || schema.length() == 0) {
            
            if (hasExplicitCatalog) {
                
                schema = null;
            }
            else {
                
                schema = conn.getCurrentSchema();
            }
        }
        else {
            
            schema = conn.normalizeIdentifier(schema);
        }
        
        if (name == null || name.length() == 0) {
            
            name = null;
        }
        else {
            
            name = conn.normalizeIdentifier(name);
        }
    }
    
    public boolean isMalformed() {
        
        return isMalformed;
    }
    
    public String getCatalog() {
        
        return catalog;
    }
    
    public String getSchema() {
        
        return schema;
    }
    
    public String getName() {
        
        return name;
    }
    
    public int getAdditionalSize() {
        
        return extraName == null ? 0 : extraName.size();
    }
    
    public String getAdditionalPart(int idx) {
        
        return extraName.get(idx);
    }
    
    private void parse (String str) {
        
        if (str == null) {
            
            isMalformed = true;
            return;
        }
        
        int idx = 0;
        final int len = str.length();
        
        /*
         * Skip leading white space.
         */
        for (; idx < len && Character.isWhitespace(str.charAt(idx)); idx++);
        
        while (idx < len) {
            
            char ch = str.charAt(idx++);
            if (ch == '"') {
                
                buffer.append('"');
                
                boolean done = false;
                while (!done && idx < len) {
                    
                    ch = str.charAt(idx++);
                    if (ch == '"') {
                        
                        if (idx < len && str.charAt(idx) == '"') {
                            
                            buffer.append('"');
                            ++idx;
                        }
                        else {
                            
                            done = true;
                        }
                    }
                    else {
                        
                        buffer.append(ch);
                    }
                }
                
                if (! done) {
                    
                    isMalformed = true;
                }
                
                buffer.append('"');
            }
            else if (ch == '.') {
                
                addPart(buffer.toString());
                buffer.setLength(0);
            }
            else {
                
                buffer.append(ch);
            }
        }
        
        addPart(buffer.toString());
    }
    
    private void addPart(String name) {
        
        if (currentPart == 0) {
            
            this.name = name;
        }
        else if (currentPart == 1) {
            
            schema = this.name;
            this.name = name;
        }
        else if (currentPart == 2) {
            
            catalog = this.schema;
            schema = this.name;
            this.name = name;
        }
        else {
            
            if (currentPart >= maxParts) {
                
                isMalformed = true;
            }
            else {
                
                if (extraName == null) {
                    
                    extraName = new ArrayList<String>(maxParts);
                }
                
                extraName.add(name);
            }
        }
        
        ++currentPart;
    }
    
}
