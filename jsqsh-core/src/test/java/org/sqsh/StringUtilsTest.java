package org.sqsh;

import org.junit.Assert;
import org.junit.Test;
import org.sqsh.util.StringUtils;
import static org.sqsh.util.StringUtils.BadEscapeHandling.*;

public class StringUtilsTest {

    @Test
    public void testHexByteExpansion() {

        Assert.assertEquals("I want a dog", StringUtils.expandEscapes("I want \\x61 dog"));
        Assert.assertEquals("You smell",
                StringUtils.expandEscapes("\\x59\\x6F\\x75 \\x73\\x6d\\x65\\x6c\\x6c"));

        Assert.assertEquals("a5", StringUtils.expandEscapes("\\x615"));

        String[] baddies = { "\\x5", "\\x0g", "\\xj5" };
        for (int i = 0; i < baddies.length; i++) {

            try {

                StringUtils.expandEscapes("This: " + baddies[i] + " is a bad escape");
                Assert.fail(baddies[i] + " should have thrown an exception");
            }
            catch (IllegalArgumentException e) {

                // GOOD!
            }
        }
    }

    @Test
    public void testUnicodeExpansion() {

        Assert.assertEquals("I want a dog", StringUtils.expandEscapes("I want \\u0061 dog"));
        Assert.assertEquals("You smell",
                StringUtils.expandEscapes("\\u0059\\u006F\\u0075 \\u0073\\u006d\\u0065\\u006c\\u006c"));

        Assert.assertEquals("a5", StringUtils.expandEscapes("\\u00615"));

        String[] baddies = { "\\u5", "\\u05", "\\u005", "\\uGHIJ \\123g" };
        for (int i = 0; i < baddies.length; i++) {

            try {

                StringUtils.expandEscapes("This: " + baddies[i] + " is a bad escape");
                Assert.fail(baddies[i] + " should have thrown an exception");
            }
            catch (IllegalArgumentException e) {

                // GOOD!
            }
        }
    }

    @Test
    public void testOctalExpansion() {

        Assert.assertEquals("I want a dog", StringUtils.expandEscapes("I want \\141 dog"));
        Assert.assertEquals("You smell",
                StringUtils.expandEscapes("\\131\\157\\165 \\163\\155\\145\\154\\154"));

        Assert.assertEquals("a5", StringUtils.expandEscapes("\\1415"));

        String[] baddies = { "\\0", "\\01", "\\189" };
        for (int i = 0; i < baddies.length; i++) {

            try {

                StringUtils.expandEscapes("This: " + baddies[i] + " is a bad escape");
                Assert.fail(baddies[i] + " should have thrown an exception");
            }
            catch (IllegalArgumentException e) {

                // GOOD!
            }
        }
    }

    @Test
    public void testBadEscape() {

        Assert.assertEquals("Leave bad \\j escape",
                StringUtils.expandEscapes("Leave bad \\j escape", LEAVE_ESCAPE_CHARACTER));
        Assert.assertEquals("Drop bad j escape",
                StringUtils.expandEscapes("Drop bad \\j escape", DROP_ESCAPE_CHARACTER));
        Assert.assertEquals("Drop bad 831 escape",
                StringUtils.expandEscapes("Drop bad \\831 escape", DROP_ESCAPE_CHARACTER));
    }
}
