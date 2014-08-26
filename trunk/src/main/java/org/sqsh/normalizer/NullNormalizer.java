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
package org.sqsh.normalizer;

/**
 * Does no normalization of an identifier. If the identifier is surrounded
 * by double quotes, the quotes are stripped off, otherwise the identifier
 * is returned as it is received.
 */
public class NullNormalizer implements SQLNormalizer {
    
    @Override
    public String getName() {
        
        return "NONE";
    }
    
    @Override
    public String normalize (String identifier) {
        
        if (identifier == null) {
            
            return null;
        }
        
        int len = identifier.length();
        
        if (len >= 2
            && identifier.charAt(0) == '"'
            && identifier.charAt(len-1) == '"') {
            
            return identifier.substring(1, len-1);
        }
        
        return identifier;
    }
}
