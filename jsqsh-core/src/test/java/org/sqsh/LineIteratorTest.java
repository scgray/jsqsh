/*
 * Copyright 2007-2017 Scott C. Gray
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
