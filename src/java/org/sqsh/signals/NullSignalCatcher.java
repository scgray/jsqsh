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
package org.sqsh.signals;

/**
 * Implementation of a signal handler for JVM's that don't support the
 * functionality. This implementation does nothing but print out a warning
 * that signal handling will not be supported.
 */
public class NullSignalCatcher
    extends AbstractSignalCatcher {
    
    /**
     * Creates a signal handler.
     * 
     * @param manager The manager that created us.
     */
    public NullSignalCatcher(SignalManager manager) {
        
        super(manager);
        System.out.println(
            "WARNING: This JVM does not support signal handling. This means "
                 + "that CTRL-C cannot be used to cancel queries. Currently "
                 + "only the Sun JVM (http://java.sun.com) is supported.");
    }
}
