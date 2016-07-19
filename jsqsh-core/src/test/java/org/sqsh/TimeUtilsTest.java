package org.sqsh;

import org.junit.Test;
import org.junit.Assert;
import org.sqsh.util.TimeUtils;

public class TimeUtilsTest {
	
	@Test
	public void testMillisToDurationString() {
		
		String s;
		
		s = TimeUtils.millisToDurationString(1L);
		Assert.assertEquals("0.001s", s);
		s = TimeUtils.millisToDurationString(12L);
		Assert.assertEquals("0.012s", s);
		s = TimeUtils.millisToDurationString(123L);
		Assert.assertEquals("0.123s", s);
		s = TimeUtils.millisToDurationString(1123L);
		Assert.assertEquals("1.123s", s);
	}
}
