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

import org.sqsh.Formatter;
import java.text.DecimalFormat;

public class NumberFormatter
    implements Formatter {
    
    private int precision;
    private int scale;
    private DecimalFormat format;
    
    public NumberFormatter(int precision, int scale) {
        
        this.precision = precision;
        this.scale = scale;
        
        if (scale <= 0) {
            
            format = new DecimalFormat("#");
        }
        else {
                
            StringBuilder sb = new StringBuilder();
            sb.append("#.");
            for (int i = 0; i < scale; i++) {
                    
                sb.append('0');
            }
            
            format = new DecimalFormat(sb.toString());
        }
    }

    @Override
    public String format (Object value) {
        
        if (!(value instanceof Number))
        {
            throw new IllegalArgumentException("Cannot format " 
               + value.getClass().getName() + " as a number");
        }
        
        return format.format(value);
    }

    @Override
    public int getMaxWidth () {

        /*
         * This is an Oracle-ism. If the precision is 0 then this is
         * just a regular floating point number so we will assume 38
         * digits of precision + sign + decimal point.
         */
        if (precision == 0) {
            
            return 21;
        }
        
        /*
         * Here's a good reference for Oracle NUMBER() type
         * http://www-eleves-isia.cma.fr/documentation/OracleDoc/NUMBER-DATATYPE.html
         */
        
        /*
         * Oracle'ism: If scale is less than zero, that indicates the
         * number of significant digits to the left of the decimal:
         *    7456123.89 -> NUMBER (7,-2) -> 7456100
         * So here our maximum size is the precision + the sign.
         */
        if (scale <= 0) {
            
            return precision + 1;
        }
        
        /*
         * Oracle'ism: If scale is greater than precision, that indicates
         * a purely fractional number, like so:
         *     .00000123->NUMBER(2,7)->.0000012 
         */
        if (scale > 0 && scale > precision) {
            
            /* 
             * Extra room for decimal and sign.
             */
            return scale + 2;
        }
        
        /*
         * Leave room for the decimal and the sign.
         */
        return precision + 2;
    }
}
