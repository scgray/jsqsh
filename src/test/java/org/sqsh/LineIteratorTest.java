/*
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2014
 * The source code for this program is not published or otherwise divested of
 * its trade secrets, irrespective of what has been deposited with the U.S. 
 * Copyright Office.
 */
package org.sqsh;

import org.junit.Test;
import org.junit.Assert;

public class LineIteratorTest {
    
    @Test
    public void testLineIterator() {
        
        LineIterator lineIter;
        
        lineIter = new WordWrapLineIterator("this is a string", 10);
        Assert.assertEquals("this is a", lineIter.next());
        Assert.assertEquals("string", lineIter.next());
        
        lineIter = new WordWrapLineIterator("this is a string", 4);
        Assert.assertEquals("this", lineIter.next());
        Assert.assertEquals("is a", lineIter.next());
        Assert.assertEquals("stri", lineIter.next());
        Assert.assertEquals("ng", lineIter.next());
        
        lineIter = new WordWrapLineIterator("this\nis\na\nstring\n", 4);
        Assert.assertEquals("this", lineIter.next());
        Assert.assertEquals("is", lineIter.next());
        Assert.assertEquals("a", lineIter.next());
        Assert.assertEquals("stri", lineIter.next());
        Assert.assertEquals("ng", lineIter.next());
        
        lineIter = new WordWrapLineIterator("this     is a string", 15);
        Assert.assertEquals("this     is a", lineIter.next());
        Assert.assertEquals("string", lineIter.next());
    }
}
