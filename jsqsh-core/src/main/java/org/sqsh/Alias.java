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
 * Representation of a sqsh alias. An alias is similar to a variable expansion except that it does not require
 * any special syntax...anywhere the alias is found in a line, it is replaced.
 */
public class Alias implements Comparable<Alias> {
    
    /**
     * The actual alias.
     */
    private final String name;
    
    /**
     * The replacement text for the alias.
     */
    private final String text;
    
    /**
     * A global alias can be replaced anywhere within a line of text, if
     * an alias is not global, then it will only be replaced if it is the
     * first word on the line.
     */
    private final boolean isGlobal;

    /**
     * Creates a new alias.
     * 
     * @param name The name of the alias.
     * @param text The replacement text for the alias.
     * @param isGlobal A global alias can be replaced anywhere within a line of text, if an alias is not global,
     *    then it will only be replaced if it is the first word on the line.
     */
    public Alias (String name, String text, boolean isGlobal) {
        this.name = name;
        this.text = text;
        this.isGlobal = isGlobal;
    }
    
    /**
     * Returns the name of the alias.
     * 
     * @return the name of the alias.
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return whether the alias is global.
     */
    public boolean isGlobal () {
        return isGlobal;
    }
    
    /**
     * @return the text that will replace the alias.
     */
    public String getText () {
        return text;
    }

    /**
     * Compares the names of two aliases. This method is provided primarily to allow for easy sorting of aliases on display.
     * 
     * @param that The object to compare to.
     * @return The results of the comparison.
     */
    @Override
    public int compareTo(Alias that) {
        return this.name.compareTo(that.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Alias) {
            return ((Alias) o).getName().equals(name);
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
