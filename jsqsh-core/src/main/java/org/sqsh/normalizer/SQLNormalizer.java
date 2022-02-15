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
package org.sqsh.normalizer;

/**
 * A normalizer is used by metadata commands, such as "\describe" to normalize the names of objects according to
 * database-specific rules.  For example, with DB2 and Oracle, identifiers are normalized to upper case unless they are
 * surrounded by double quotes.
 */
public interface SQLNormalizer {
    /**
     * @return A unique logical name for the normalizer
     */
    String getName();

    /**
     * Called to normalize an identifier.  The identifier will be passed in the fashion that it was received from the
     * metadata command, which will typically be in the same fashion that the user provided it. For example:
     * <pre>
     *    \describe "My Table".Foo
     * </pre>
     * Would likely call this method twice, once with "My Table" (including double quotes) and one with Foo (no double
     * quotes), and this function should normalize the identifier according to the implementations chosen rules.
     *
     * @param identifier The identifier to normalize
     * @return The normalized identifier.
     */
    String normalize(String identifier);
}
