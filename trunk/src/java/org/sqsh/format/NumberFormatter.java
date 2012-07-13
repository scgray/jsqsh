/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
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
