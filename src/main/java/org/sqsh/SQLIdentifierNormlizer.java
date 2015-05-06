/*
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2014
 * The source code for this program is not published or otherwise divested of
 * its trade secrets, irrespective of what has been deposited with the U.S. 
 * Copyright Office.
 */
package org.sqsh;

/**
 * Normalizer used by metadata commands, such as "\describe" to normalize
 * the names of objects according to database-specific rules.  For example, with
 * DB2 and Oracle, identifiers are normalized to upper case unless they are
 * surrounded by double quotes.
 */
public class SQLIdentifierNormlizer {
    
    public static enum Direction {
        
        UPPER_CASE,
        LOWER_CASE,
        NONE
    }
    
    private Direction unquotedIdentifierDirection;
    private Direction quotedIdentifierDirection;
    
    /**
     * Creates a normalizer.
     * 
     * @param unquotedDirection How to normalize unquoted identifiers.
     * @param quotedDirection How to normalize quoted identifiers
     */
    public SQLIdentifierNormlizer (Direction unquotedDirection, 
            Direction quotedDirection) {
        
        this.unquotedIdentifierDirection = unquotedDirection;
        this.quotedIdentifierDirection = quotedDirection;
    }
    
    /**
     * Normalizes an identifier. If the identifier was quoted, the quotes will be
     * removed prior to normalization.
     * 
     * @param identifier The identifier
     * @return The normalized identifier;
     */
    public String normalize(String identifier) {
        
        if (identifier == null) {
            
            return null;
        }
        
        int len = identifier.length();
        
        if (len >= 2
            && identifier.charAt(0) == '"'
            && identifier.charAt(len-1) == '"') {
            
            if (quotedIdentifierDirection == Direction.NONE) {
                
                return identifier.substring(1, len-1);
            }
            
            StringBuilder sb = new StringBuilder(len-2);
            if (quotedIdentifierDirection == Direction.UPPER_CASE) {
                
                for (int i = 1; i < (len-1); i++) {
                    
                    sb.append(Character.toUpperCase(identifier.charAt(i)));
                }
            }
            else {
                
                for (int i = 1; i < (len-1); i++) {
                    
                    sb.append(Character.toLowerCase(identifier.charAt(i)));
                }
            }
            
            return sb.toString();
        }
        
        if (unquotedIdentifierDirection == Direction.NONE) {
            
            return identifier;
        }
        else if (unquotedIdentifierDirection == Direction.UPPER_CASE) {
            
            return identifier.toUpperCase();
        }
        
        return identifier.toLowerCase();
    }
}
