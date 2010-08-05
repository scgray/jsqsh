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

/**
 * Formats a byte or a byte array.
 */
public class ByteFormatter
    implements Formatter {
    
    private byte[] b = new byte[1];
    private int maxBytes;
    
    public ByteFormatter(int maxBytes) {
        
        this.maxBytes = maxBytes;
    }

    public String format (Object value) {
        
        byte []bytes;
        
        if (value instanceof Byte) {
            
            b[0] = ((Byte) value).byteValue();
            bytes = b;
        }
        else {
            
            bytes = ((byte[]) value);
        }
        
        StringBuilder sb = new StringBuilder(2 + (bytes.length * 2));
        byte ch;
        
        sb.append("0x");
        
        String hexDigits[] = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", 
            "a", "b", "c", "d", "e", "f"
        };
        
        for (int i = 0; i < bytes.length; i++) {

            ch = (byte) (bytes[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            
            sb.append(hexDigits[(int) ch]);
            ch = (byte) (bytes[i] & 0x0F);

            sb.append(hexDigits[(int) ch]);
        }
        
        return sb.toString();
    }

    public int getMaxWidth () {

        return 2 + (maxBytes * 2);
    }
}
