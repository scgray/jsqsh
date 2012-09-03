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
package org.sqsh.format;

import java.sql.Blob;

public class BlobFormatter
    extends ByteFormatter {
    
    public BlobFormatter() {
        
        super(Integer.MAX_VALUE);
    }

    public String format (Object value) {

        Blob blob = (Blob) value;
        
        try {
            
            byte bytes[] = blob.getBytes(0, (int) blob.length());
            return super.format(bytes);
        }
        catch (Exception e) {
            
            /* IGNORED */
        }
        
        return "*ERROR*";
    }
}
