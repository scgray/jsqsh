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

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * This object represents the command line options that are accepted by
 * a jsqsh command. All jsqsh commands accept at least the following
 * options due to inheritence from this class:
 * 
 * <pre>
 *    -g  Send output to a popup graphical window
 * </pre>
 * 
 * Commands should avoid utilizing these command line switches.
 */
public class SqshOptions {
    
    @Option(name="-g",usage="Send output to a popup graphical window")
        public boolean isGraphical = false;
    
    @Argument
        public List<String> arguments = new ArrayList<String>();
}
