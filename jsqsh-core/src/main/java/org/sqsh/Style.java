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
 * This class is used as an abstract representation of the current display
 * style from a {@link ConnectionContext}. The idea is that a <code>ConnectionContext</code>
 * can be asked its style from {@link ConnectionContext#getStyle()} and
 * then you can later restore that style with {@link ConnectionContext#setStyle(Style)}.
 * The only reason that this representation is used rather than just a simple
 * string is because some styles have additional internal configuration 
 * information, such as indent.
 */
public class Style {
    
    protected String name;
    
    protected Style (String name) {
        
        this.name = name;
    }
    
    public String getName() {
        
        return name;
    }
    
    @Override
    public String toString() {
        
        return name;
    }
}
