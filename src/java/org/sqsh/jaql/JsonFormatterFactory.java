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
package org.sqsh.jaql;

import org.sqsh.Session;

public class JsonFormatterFactory {
    
    public static JaqlFormatter getFormatter (Session session, String name) {
        
        return getFormatter (session, name, -1);
    }
    
    public static JaqlFormatter getFormatter (Session session, String name, int indent) {
        
        /*
         * No name provided? Check the environment.
         */
        if (name == null) {
            
            name = session.getVariable("jaql_style");
        }
        
        /*
         * If "json" is asked for, then we need to process the indent.
         */
        if (name == null || name.equals("json")) {
        
            if (indent < 0) {
                
                String prop = session.getVariable("jaql_indent");
                if (prop == null) {
                    
                    indent = 3;
                }
                else {
                    
                    indent = Integer.parseInt(prop);
                }
            }
            
            return new JsonFormatter (session, indent);
        }
        else if (name.equals("lines")) {
            
            return new JsonLinesFormatter (session);
        }
        else if (name.equals("discard")) {
            
            return new DiscardFormatter (session);
        }
        else if (name.equals("csv")) {
            
            return new JsonCSVFormatter (session);
        }
        else {
            
            return null;
        }
    }
}
