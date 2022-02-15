/*
 * Copyright 2007-2022 Scott C. Gray
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
package org.sqsh.options;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker for defining a command line option that is used to set a member of an object when provided. This annotation
 * can be provided for a field of an object to indicate that the value of that field can be set with a command line
 * option.  It is important to ensure that the field is public or that it is running in a security context that allows
 * the field to be set.
 */
@Retention(RUNTIME)
@Target({FIELD})
public @interface OptionProperty {

    /**
     * Specifies the command line option character (e.g. 'e' indicates '-e' is the option)
     */
    char option();

    /**
     * If non-null specifies the a long name for the option. This name should not include any leading dashes (e.g. it
     * should be "long-name" not "--long-name")
     */
    String longOption() default "";

    /**
     * String describing the meaning of this option.
     */
    String description() default "";

    /**
     * Deprecated options won't show up in help
     *
     * @return whether or not the option is deprecated
     */
    boolean deprecated() default false;

    /**
     * Specifies if an argument to the option is required, optional, or not required.
     */
    ArgumentRequired arg() default ArgumentRequired.NONE;

    /**
     * Specifies a display name for the argument to the option that will be used when generating usage text. If not
     * provided, the usage text will be:
     * <pre>
     *   -x val  This is the usage for the option
     * </pre>
     * If argName is provided then "val" will be replaced with the name provided.
     */
    String argName() default "val";
}
