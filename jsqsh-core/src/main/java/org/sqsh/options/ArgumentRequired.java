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

/**
 * Enumeration utilized to indicate whether or not an option requires
 * and argument.
 */
public enum ArgumentRequired {
    
    /**
     * Indicates that the option does not take an argument.
     */
    NONE,
    
    /**
     * Indicates that the option requires an argument.
     */
    REQUIRED,
    
    /**
     * Indicates that the option may take an argument.
     */
    OPTIONAL
}
