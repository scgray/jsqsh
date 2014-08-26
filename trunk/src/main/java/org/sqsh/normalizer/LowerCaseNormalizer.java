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
 * Normalizes unquoted identifiers to upper case. Quoted identifiers are
 * left as was provided.
 */
public class LowerCaseNormalizer implements SQLNormalizer {
    
    @Override
    public String getName() {
        
        return "LOWER CASE";
    }
    
    @Override
    public String normalize (String identifier) {
        
        if (identifier == null) {
            
            return null;
        }
        
        int len = identifier.length();
        
        /*
         * Identifier was surrounded by double quotes, so strip them off and
         * return the contents as was provided by the user.
         */
        if (len >= 2
            && identifier.charAt(0) == '"'
            && identifier.charAt(len-1) == '"') {
            
            return identifier.substring(1, len-1);
        }
        
        
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            
            sb.append(Character.toLowerCase(identifier.charAt(i)));
        }
        
        return sb.toString();
    }
}
