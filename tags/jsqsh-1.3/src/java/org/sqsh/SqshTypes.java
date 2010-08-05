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
 * The existence of this class is a little bit hokey, but I'll try to 
 * explain. There are a couple of pieces of jsqsh that need to know some
 * specifics about datatypes that are specific to certain drives (at the
 * time of this writing, it is specifically the Oracle CURSOR datatype).
 * Since I don't want to actually include the drivers that contain these
 * special datatypes in with jsqsh, I provide my own constants here. 
 * This is probably not a good practice, but here I am :)
 */
public class SqshTypes {
    
    /**
     * Corresponds to oracle.jdbc.OracleTypes.CURSOR.
     */
    public static final int ORACLE_CURSOR = -10;
}
