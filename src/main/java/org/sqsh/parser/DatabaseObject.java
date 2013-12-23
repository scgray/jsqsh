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
package org.sqsh.parser;

/**
 * Representation of a database object. 
 */
public class DatabaseObject {

    private String catalog;
    private String schema;
    private String name;
    private String column = null;
    private String alias = null;
    
    /**
     * Creates a database object.
     * 
     * @param catalog  The catalog containing the object (can be null)
     * @param schema The schema containing the object (can be null)
     * @param name The name of the object (can be null).
     */
    public DatabaseObject (String catalog, String schema, String name) {
        
        this.catalog = catalog;
        this.schema = schema;
        this.name = name;
    }
    
    /**
     * Creates an object from a sequence of name parts.
     * 
     * @param nameParts An array of parts of an object name.
     */
    public DatabaseObject (String []nameParts) {
        
        if (nameParts.length == 1) {
            
            name = nameParts[0];
        }
        else if (nameParts.length == 2) {
            
            schema = nameParts[0];
            name = nameParts[1];
        }
        else if (nameParts.length == 3) {
            
            catalog = nameParts[0];
            schema = nameParts[1];
            name = nameParts[2];
        }
        else if (nameParts.length == 4) {
            
            catalog = nameParts[0];
            schema = nameParts[1];
            name = nameParts[2];
            column = nameParts[3];
        }
        
    }

    /**
     * @return the catalog containing the object that is referenced.
     *   or null if the object is unknown.
     */
    public String getCatalog() {
        
        return catalog;
    }
    
    /**
     * @return the schema of the object that is referenced.
     *   or null if the schema is unknown.
     */
    public String getSchema() {
        
        return schema;
    }
    
    /**
     * @return the object name.
     */
    public String getName() {
        
        return name;
    }
    
    /**
     * @return The alias for the table or null if no alias is defined.
     */
    public String getAlias() {
        
        return alias;
    }
    
    /**
     * @param alias The alias for the object
     */
    public void setAlias(String alias) {
        
        this.alias = alias;
    }
    
    /**
     * @return the column of the object or null if none was found.
     */
    public String getColumn () {
    
        return column;
    }

    
    /**
     * @param column Sets the column of the object.
     */
    public void setColumn (String column) {
    
        this.column = column;
    }

    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        if (catalog != null) {
            
            sb.append('[')
                .append(catalog)
                .append(']')
                .append('.');
        }
        if (schema != null) {
            
            sb.append('[')
                .append(schema)
                .append(']')
                .append('.');
        }
        sb.append('[')
            .append(name)
            .append(']');
        
        if (alias != null) {
            
            sb.append(" [as ");
            sb.append(alias);
            sb.append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * Compares two database objects for equality.
     */
    public boolean equals (Object o) {
        
        if (o instanceof DatabaseObject) {
            
            DatabaseObject that = (DatabaseObject) o;
            
            boolean matches = 
                (that.catalog == null && this.catalog == null)
                    || (that.catalog != null && this.catalog != null
                            && that.catalog.equals(this.catalog));
            
            matches = matches && 
                (that.schema == null && this.schema == null)
                    || (that.schema != null && this.schema != null
                            && that.schema.equals(this.schema));
            
            matches = matches && 
                (that.name == null && this.name == null)
                    || (that.name != null && this.name != null
                            && that.name.equals(this.name));
            
            matches = matches && 
                (that.column == null && this.column == null)
                    || (that.column != null && this.column != null
                            && that.column.equals(this.column));
            
            matches = matches && 
                (that.alias == null && this.alias == null)
                    || (that.alias != null && this.alias != null
                            && that.alias.equals(this.alias));
            
            return matches;
        }
        
        return false;
    }
}
