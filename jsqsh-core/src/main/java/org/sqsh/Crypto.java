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
package org.sqsh;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provides basic encryption/description using the java security 
 * encryption framework.
 */
public class Crypto {
    
    public static byte[] DEFAULT_KEY = {
        
        (byte) 0xd1, (byte) 0x09, (byte) 0x79, (byte) 0x1,
        (byte) 0x96, (byte) 0xdf, (byte) 0x6a, (byte) 0x69,
        (byte) 0xf3, (byte) 0x08, (byte) 0x52, (byte) 0xac,
        (byte) 0xb2, (byte) 0xb0, (byte) 0x4a, (byte) 0x91
    };
    
    /**
     * Encrypt a value using a default key.
     * @param value The value to be encrypted.
     * @return The encrypted form of the value.
     */
    public static byte[] encrypt (byte []value) {
        
        return (encrypt(null, value));
    }
    
    /**
     * Encrypt using a provided key.
     * 
     * @param key The key to use for encryption. The encryption 
     *   algorithm used is Blowfish so the key should conform
     *   to what Blowfish expects.
     *   
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    public static byte[] encrypt (byte []key, byte []value) {
        
        if (key == null) {
            
            key = DEFAULT_KEY;
        }
        
        try {
            
            SecretKeySpec skeySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(value);
        }
        catch (Exception e) {
            
            return value;
        }
    }
    
    /**
     * Decrypt a value using the default key.
     * @param value The value to decrypt.
     * @return The decrypted value.
     */
    public static byte[] decrypt(byte []value) {
        
        return decrypt(null, value);
    }
    
    /**
     * Decrypt a value using a provided key.
     * 
     * @param key The key to use for encryption. The encryption 
     *   algorithm used is Blowfish so the key should conform
     *   to what Blowfish expects.
     * @param value The value to be decrypted.
     * @return The decrypted value.
     */
    public static byte[] decrypt(byte []key, byte []value) {
        
        if (key == null) {
            
            key = DEFAULT_KEY;
        }
        
        try {
            
            SecretKeySpec skeySpec = new SecretKeySpec(key, "Blowfish");
    
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
    
            return cipher.doFinal(value);
        }
        catch (Exception e) {
            
            return value;
        }
    }
    
    public static void main(String argv[]) {
        
        try {
            
            KeyGenerator kgen = KeyGenerator.getInstance("Blowfish");
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            
            for (int i = 0; i < raw.length; i++) {
                
                if (i > 0) {
                    
                    System.out.print(", ");
                }
                
                System.out.print("0x" + Integer.toHexString((int) 0xff & raw[i]));
            }
            System.out.println();
        }
        catch (Exception e) {
            
            e.printStackTrace();
        }
    }

}
