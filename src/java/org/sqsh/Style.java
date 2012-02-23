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
