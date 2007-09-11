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
package org.sqsh;

/**
 * A SQLAnalyzer is responsible for analyzing SQL (duh) so that sqsh 
 * can ask various questions of it. The expectation here is that 
 * there may be different implementations for different database
 * vendors.
 */
public interface SQLAnalyzer {
    
    /**
     * Analyzes a block of SQL to see if it is terminated with the provided
     * terminator character (presumably the character is a semicolon most of
     * the time). SQL is NOT considered terminated if the terminator is
     * located within a block of code (e.g. between a BEGIN/END) or within
     * comments or strings.
     * 
     * @param sql The SQL to analyze
     * @param terminator The terminator character
     * @return True if the SQL is terminated.
     */
    boolean isTerminated(String sql, char terminator);
}
