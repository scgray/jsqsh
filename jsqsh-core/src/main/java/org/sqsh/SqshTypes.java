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
   
    /**
     * Corresponds to com.ibm.db2.jcc.CURSOR
     */
    public static final int DB2_CURSOR = -100008;
}
