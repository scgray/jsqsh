/*
 * Copyright 2007-2022 Scott C. Gray
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

import java.sql.Types;

/**
 * Object that is used to pass parameters to a callable or 
 * prepared statement.
 */
public class CallParameter {
    
    public static final int INPUT  = 1;
    public static final int OUTPUT = 2;
    public static final int INOUT  = 3;
    
    private int    idx;
    private int    dataType;
    private int    direction;
    private String value;
    
    public CallParameter (int idx, int dataType,
            int direction, String value) {
        
        this.idx = idx;
        this.dataType = dataType;
        this.direction = direction;
        this.value = value;
    }
    
    public CallParameter (int idx, String value) {
        
        this.idx = idx;
        this.value = value;
        this.direction = INPUT;
        this.dataType = Types.VARCHAR;
    }
    
    public int getIdx() {
        
        return idx;
    }
    
    public void setIdx(int idx) {
        
        this.idx = idx;
    }
    
    public int getType() {
        
        return dataType;
    }
    
    public void setType(int type) {
        
        this.dataType = type;
    }
    
    public int getDirection() {
        
        return direction;
    }
    
    public void setDirection(int direction) {
        
        this.direction = direction;
    }
    
    public String getValue() {
        
        return value;
    }
    
    public void setValue (String value) {
        
        this.value = value;
    }
}
