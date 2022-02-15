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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @Argv annotation is used to mark an array of strings or vector of strings that represents the location to store
 * the arguments for a program that are not command line options. In addition, attributes such as usage="" may be
 * provided as a description of the usage of the program as a whole.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Argv {
    /**
     * Specifies the usage of the program.
     */
    String usage() default "";

    /**
     * This specifies the program for which the option processing is taking place. This name will become part of the
     * usage string and exceptions thrown during argument processing.
     */
    String program();

    /**
     * If provided, specifies the minimum number of non-option arguments that the program requires for operation.
     */
    int min() default 0;

    /**
     * If provided, specifies the maximum number of non-option arguments that the program requires for operation. A
     * value &lt; 0 indicates unbounded arguments.
     */
    int max() default -1;
}
