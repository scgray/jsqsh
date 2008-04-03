package org.sqsh.options;

/**
 * Enumeration utilized to indicate whether or not an option requires
 * and argument.
 */
public enum ArgumentRequired {
    
    /**
     * Indicates that the option does not take an argument.
     */
    NONE,
    
    /**
     * Indicates that the option requires an argument.
     */
    REQUIRED,
    
    /**
     * Indicates that the option may take an argument.
     */
    OPTIONAL
}
