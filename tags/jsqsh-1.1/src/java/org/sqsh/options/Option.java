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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker for defining a command line option that is used to set a 
 * member of an object when provided. This annotation can be provided
 * for a field of an object to indicate that the value of that field
 * can be set with a command line option.  It is important to ensure
 * that the field is public or that it is running in a security context
 * that allows the field to be set.
 */
@Retention(RUNTIME)
@Target({FIELD})
public @interface Option {
    
    /**
     * Specifies the command line option character (e.g. 'e' indicates '-e'
     * is the option)
     */
    char option();
    
    /**
     * If non-null specifies the a long name for the option. This name should
     * not include any leading dashes (e.g. it should be "long-name" not
     * "--long-name")
     */
    String longOption() default "";
    
    /**
     * String describing the meaning of this option.
     */
    String description() default "";
    
    /**
     * Specifies if an argument to the option is required, optional, or
     * not required.
     */
    ArgumentRequired arg() default ArgumentRequired.NONE;
    
    /**
     * Specifies a display name for the argument to the option that will
     * be used when generating usage text. If not provided, the usage
     * text will be:
     * <pre>
     *   -x val  This is the usage for the option
     * </pre>
     * If argName is provided then "val" will be replaced with the name
     * provided.
     */
    String argName() default "val";
}
