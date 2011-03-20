package org.sqsh.variables;

import org.sqsh.CannotSetValueError;
import org.sqsh.Variable;

public class DimensionVariable
    extends Variable {
    
    private int width = 500;
    private int height = 250;
    
    public DimensionVariable() {
        
    }

    @Override
    public String setValue (String value)
        throws CannotSetValueError {
        
        boolean ok = true;
        
        if (value.indexOf('x') <= 0) {
            
            ok = false;
        }
        
        if (ok) {
            
            String []parts = value.split("x");
            if (parts.length != 2) {
            
                ok = false;
            }
            else {
                
                try {
                    
                    int tmpWidth = Integer.parseInt(parts[0]);
                    int tmpHeight = Integer.parseInt(parts[1]);
                    
                    width = tmpWidth;
                    height = tmpHeight;
                }
                catch (NumberFormatException e) {
                    
                    ok = false;
                }
            }
        }
        
        if (!ok) {
            
            throw new CannotSetValueError("Invalid dimension specification '"
                + value + "'. Dimensions must be of the form "
                + "WxH where W is width, and H is height");
        }
        
        return null;
    }

    @Override
    public String toString () {

        return (width + "x" + height);
    }

    
    /**
     * @return the width
     */
    public int getWidth () {
    
        return width;
    }
    
    /**
     * @param width the width to set
     */
    public void setWidth (int width) {
    
        this.width = width;
    }
    
    /**
     * @return the height
     */
    public int getHeight () {
    
        return height;
    }

    
    /**
     * @param height the height to set
     */
    public void setHeight (int height) {
    
        this.height = height;
    }
}
