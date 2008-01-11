/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
package org.sqsh.parser;

/**
 * Represents a table name that is referenced by a SQL statement.
 */
public class DatabaseObject {

    private String database;
    private String owner;
    private String name;
    private String alias;
    
    public DatabaseObject (String database, String owner, String name) {
        
        this.database = database;
        this.owner = owner;
        this.name = name;
    }

    /**
     * @return the database containing the object that is referenced.
     *   or null if the object is unknown.
     */
    public String getDatabase() {
        
        return database;
    }
    
    /**
     * @return the owner of the object that is referenced.
     *   or null if the owner is unknown.
     */
    public String getOwner() {
        
        return owner;
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
    
    public void setAlias(String alias) {
        
        this.alias = alias;
    }
    
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        if (database != null) {
            
            sb.append('[')
                .append(database)
                .append(']')
                .append('.');
        }
        if (owner != null) {
            
            sb.append('[')
                .append(owner)
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
}
