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
public class TableReference {

    private String database;
    private String owner;
    private String table;
    private String alias;
    
    public TableReference (String database, String owner, String table) {
        
        this.database = database;
        this.owner = owner;
        this.table = table;
    }

    /**
     * @return the database containing the table that is referenced.
     *   or null if the table is unknown.
     */
    public String getDatabase() {
        
        return database;
    }
    
    /**
     * @return the table owner of the table that is referenced.
     *   or null if the owner is unknown.
     */
    public String getOwner() {
        
        return owner;
    }
    
    /**
     * @return the table name.
     */
    public String getTable() {
        
        return table;
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
}
