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
package org.sqsh;

/**
 * Thrown by {@link Variable#setValue(String)} if the value of the variable cannot be set, either because the variable
 * is immutable or because the value provided is invalid.
 */
public class CannotSetValueError extends Error {

    private static final long serialVersionUID = -1877819111531503199L;

    /**
     * Creates a new error.
     *
     * @param message The message of the error.
     */
    public CannotSetValueError(String message) {
        super(message);
    }

    public CannotSetValueError(String message, Throwable cause) {
        super(message, cause);
    }
}
