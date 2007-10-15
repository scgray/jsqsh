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
 * Implementation of SQLParserListener that provides NO-OP's for all of 
 * the methods dictated by the interface. 
 */
public class AbstractParserListener
    implements SQLParserListener {

    /** {@inheritDoc} */
    public void foundClause (SQLParser parser, String clause) {

        /* IGNORED */
    }

    /** {@inheritDoc} */
    public void foundStatement (SQLParser parser, String statement) {

        /* IGNORED */
    }
    
    /** {@inheritDoc} */
    public void enteredSubquery(SQLParser parser) {
        
        /* IGNORED */
    }
    
    /** {@inheritDoc} */
    public void exitedSubquery(SQLParser parser) {
        
        /* IGNORED */
    }

    /** {@inheritDoc} */
    public void foundTableReference (SQLParser parser, TableReference tableRef) {

        /* IGNORED */
    }
}
