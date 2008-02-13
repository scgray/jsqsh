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
package org.sqsh.options;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The @Argv annotation is used to mark an array of strings or 
 * vector of strings that represents the location to store the
 * arguments for a program that are not command line options.
 * In addition, attributes such as usage="" may be provided as
 * a description of the usage of the program as a whole.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Argv {
    
    /**
     * Specifies the usage of the program.
     * @return
     */
    String usage();
    
    /**
     * This specifies the program for which the option processing
     * is taking place. This name will become part of
     * the usage string and exceptions thrown during argument processing.
     */
    String program();
    
    /**
     * If provided, specifies the minimum number of non-option
     * arguments that the program requires for operation.
     */
    int min() default 0;
    
    /**
     * If provided, specifies the maximum number of non-option
     * arguments that the program requires for operation. A value
     * < 0 indicates unbounded arguments.
     */
    int max() default -1;
}
