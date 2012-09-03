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
 * A formatter is used to convert a data type (usually a base java type or
 * a java.lang.sql datatype) to a readable string.
 */
public interface Formatter {
    
    /**
     * Returns the maximum number of characters required to display a value
     * as a textual string.
     * 
     * @return The maximum number of characters required to display a value.
     */
    int getMaxWidth();

    /**
     * Formats a value into a string.
     * 
     * @param value The value to be formatted.
     * @return The formatted value.
     * @throws FormatError If the formatter fails to format a value
     */
    String format(Object value);
}
