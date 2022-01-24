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

import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.sqsh.util.TimestampFormatter;

public class TimestampFormatterTest {
    
    @Test
    public void testTimestampFormatter() throws Exception {
        
        TimestampFormatter formatter;
        Timestamp ts  = Timestamp.valueOf("1997-01-01 11:23:15.123456789");
        Timestamp ts1 = Timestamp.valueOf("1997-01-01 11:23:15.012345678");
        Timestamp ts2 = Timestamp.valueOf("1997-01-01 11:23:15.001234567");
        
        formatter = new TimestampFormatter("MM/dd/yy HH:mm:ss.S");
        Assert.assertEquals("01/01/97 11:23:15.1", formatter.format(ts));
        Assert.assertEquals("01/01/97 11:23:15.0", formatter.format(ts1));
        
        formatter = new TimestampFormatter("MM/dd/yy HH:mm:ss.SS");
        Assert.assertEquals("01/01/97 11:23:15.12", formatter.format(ts));
        Assert.assertEquals("01/01/97 11:23:15.01", formatter.format(ts1));
        Assert.assertEquals("01/01/97 11:23:15.00", formatter.format(ts2));
        
        formatter = new TimestampFormatter("MM/dd/yy HH:mm:ss.SSSS");
        Assert.assertEquals("01/01/97 11:23:15.1234", formatter.format(ts));
        Assert.assertEquals("01/01/97 11:23:15.0123", formatter.format(ts1));
        Assert.assertEquals("01/01/97 11:23:15.0012", formatter.format(ts2));
        
        SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss.S");
        Date d  = df.parse("1997-01-01 11:23:15.123");
        
        formatter = new TimestampFormatter("MM/dd/yy HH:mm:ss.S");
        Assert.assertEquals("01/01/97 11:23:15.1", formatter.format(d));
        
        formatter = new TimestampFormatter("MM/dd/yy HH:mm:ss.SS");
        Assert.assertEquals("01/01/97 11:23:15.12", formatter.format(d));
        
        formatter = new TimestampFormatter("MM/dd/yy HH:mm:ss.SSS");
        Assert.assertEquals("01/01/97 11:23:15.123", formatter.format(d));
        
        formatter = new TimestampFormatter("MM/dd/yy HH:mm:ss.SSSS");
        Assert.assertEquals("01/01/97 11:23:15.1230", formatter.format(d));
    }
}
