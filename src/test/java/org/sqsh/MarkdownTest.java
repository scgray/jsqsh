package org.sqsh;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;
import org.junit.Assert;
import org.sqsh.MarkdownFormatter.WrappingStream;
import org.sqsh.MarkdownFormatter.Decoration;
import static org.sqsh.MarkdownFormatter.RAW_DECORATION;

public class MarkdownTest {

    private static final char BOLD = '^';
    private static final char ITALIC = '@';
    private static final char BOLD_ITALIC = '%';
    private static final char RAW = '`';
    
    @Test
    public void testLineClassification() {
        
        String input;
        input =
            "# Header 1\n" +
            "  This is an example of some text in a markdown file\n" +
            "  that we will process with the line processor\n" +
            "\n" +
            "  * And a bulleted list\n" +
            "  * With some subbullets\n" +
            "    * Here is a sub-bullet\n" +
            "      * And another\n" +
            " And the start of a new paragraph\n" +
            "    With a code block\n";

        MarkdownFormatter.Line line = new MarkdownFormatter.Line(input);
        
        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.HEADER, line.type);
        Assert.assertEquals("Header 1", line.getContent());
        Assert.assertEquals(1, line.level);
        Assert.assertEquals(0, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.TEXT, line.type);
        Assert.assertEquals("This is an example of some text in a markdown file",
           line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.TEXT, line.type);
        Assert.assertEquals("that we will process with the line processor",
           line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.TEXT, line.type);
        Assert.assertEquals("", line.getContent());
        Assert.assertEquals(0, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.ULIST, line.type);
        Assert.assertEquals("And a bulleted list", line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.ULIST, line.type);
        Assert.assertEquals("With some subbullets", line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.ULIST, line.type);
        Assert.assertEquals("Here is a sub-bullet", line.getContent());
        Assert.assertEquals(4, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.ULIST, line.type);
        Assert.assertEquals("And another", line.getContent());
        Assert.assertEquals(6, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.TEXT, line.type);
        Assert.assertEquals("And the start of a new paragraph", line.getContent());
        Assert.assertEquals(1, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(MarkdownFormatter.LineType.TEXT, line.type);
        Assert.assertEquals("With a code block", line.getContent());
        Assert.assertEquals(4, line.indent);

        Assert.assertFalse(line.next());
        Assert.assertFalse(line.next());
    }

    @Test
    public void testWrappingStream() {
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bytes);
        
        WrappingStream out = new WrappingStream(ps, 2, 20); 
        
        // Simple word wrapping test
        print(out, "This is some   simple text   that we   expect to be word wrapped");
        out.flush();
        
        String result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "This is some\n" +
            "  simple text that\n" +
            "  we expect to be\n" +
            "  word wrapped", 
            result);

        bytes.reset();
        print(out, "This isareallylongstringthatwillwrapacrossmultiple lines");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "This\n" +
            "  isareallylongstrin\n" +
            "  gthatwillwrapacros\n" +
            "  smultiple lines",
            result);

        bytes.reset();
        print(out, "This isareallylongstringthatwillwrapacrossmultiplelong lines");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "This\n" +
            "  isareallylongstrin\n" +
            "  gthatwillwrapacros\n" +
            "  smultiplelong\n" +
            "  lines",
            result);

        bytes.reset();
        print(out, "This     has        trailing spaces                  ");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
              "This has trailing\n" +
            "  spaces",
            result);

        bytes.reset();
        print(out, "Wrap at exactly 20 test");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
              "Wrap at exactly 20\n" +
            "  test",
            result);

        bytes.reset();
        // Simple word wrapping test
        print(out, BOLD+"This is"+BOLD+" "+ITALIC+"some"+ITALIC+" simple text");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            Decoration.BOLD+"This is"+Decoration.OFF+" "+Decoration.ITALIC+"some"+Decoration.OFF+"\n" +
            "  simple text",
            result);

        bytes.reset();
        // Test a decoration floating between spaces
        print(out, "Test " + BOLD + " float bold"+BOLD);
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test " + Decoration.BOLD + "float bold" + Decoration.OFF,
            result);

        bytes.reset();
        // Test a decoration floating between spaces and wrapping to another line
        print(out, "Test " + BOLD + " float bold with"+BOLD+" wrap");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test " + Decoration.BOLD + "float bold" + Decoration.OFF + "\n"
                + "  " + Decoration.BOLD + "with" + Decoration.OFF + " wrap",
            result);

        bytes.reset();
        // Make sure decoration closes itself
        print(out, "Test " + BOLD + " abandoned decoration");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test " + Decoration.BOLD + "abandoned" + Decoration.OFF + "\n"
                + "  "+Decoration.BOLD + "decoration" + Decoration.OFF,
            result);

        bytes.reset();
        // Test raw mode -- are white spaces retained? Is the style wrapped?
        print(out, "Test " + RAW + "   are  white  spaces  retained?" + RAW);
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test"+RAW_DECORATION+"   are  white "+Decoration.OFF+"\n"
                + "  " + RAW_DECORATION+" spaces  retained?" + Decoration.OFF,
            result);

        bytes.reset();
        // Test raw mode -- are white spaces retained? Is the style wrapped?
        print(out, "Test " + RAW + "   are  white  spaces  retained?" + RAW);
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test"+RAW_DECORATION+"   are  white "+Decoration.OFF+"\n"
                + "  " + RAW_DECORATION+" spaces  retained?" + Decoration.OFF,
            result);

        bytes.reset();
        // Test raw + BOLDs
        print(out, "Test "+BOLD+RAW +"bold and raw with wrapping"+RAW+" of text"+BOLD);
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test "+Decoration.BOLD+RAW_DECORATION+"bold and raw "+Decoration.OFF+"\n"
                + "  "+RAW_DECORATION+"with wrapping"+Decoration.BOLD+" of"+Decoration.OFF+"\n"
                + "  "+Decoration.BOLD+"text"+Decoration.OFF,
            result);

        bytes.reset();
        // Test raw + ITALIC
        print(out, "Test "+ITALIC+RAW +"italic and raw with wrapping"+RAW+" of text"+ITALIC);
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test "+Decoration.ITALIC+Decoration.BOLD_ITALIC+"italic and ra"+Decoration.OFF+"\n"
                + "  "+Decoration.BOLD_ITALIC+"w with wrapping"+Decoration.ITALIC+" of"+Decoration.OFF+"\n"
                + "  "+Decoration.ITALIC+"text"+Decoration.OFF,
            result);

        bytes.reset();
        print(out, "Test\nwrapping with explicit newlines");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
              "Test\n"+
            "  wrapping with\n" +
            "  explicit newlines",
            result);

        bytes.reset();
        print(out, BOLD+"Test\nwrapping"+BOLD+" with explicit newlines");
        out.flush();
        
        result = bytes.toString();
        System.out.println(result);
        Assert.assertEquals(
              Decoration.BOLD+"Test"+Decoration.OFF+"\n"+
            "  "+Decoration.BOLD+"wrapping"+Decoration.OFF+" with\n" +
            "  explicit newlines",
            result);
    }
    
    private void print(WrappingStream out, String str) {
        
        int len = str.length();
        int i = 0;
        char decoration = 0;
        boolean isRaw = false;
        
        while (i < len) {
            
            char ch = str.charAt(i++);
            switch (ch) {
            case BOLD:
                if (decoration == 0) {

                    out.print(Decoration.BOLD);
                    decoration = BOLD;
                }
                else if (decoration == BOLD) {

                    out.print(Decoration.OFF);
                    decoration = 0;
                }
                else
                    throw new RuntimeException("Expected '" + BOLD + "'");
                break;
            case ITALIC:
                if (decoration == 0) {

                    out.print(Decoration.ITALIC);
                    decoration = ITALIC;
                }
                else if (decoration == ITALIC) {

                    out.print(Decoration.OFF);
                    decoration = 0;
                }
                else
                    throw new RuntimeException("Expected '" + ITALIC + "'");
                break;
            case BOLD_ITALIC:
                if (decoration == 0) {

                    out.print(Decoration.BOLD_ITALIC);
                    decoration = BOLD_ITALIC;
                }
                else if (decoration == BOLD_ITALIC) {
                    
                    out.print(Decoration.OFF);
                    decoration = 0;
                }
                else
                    throw new RuntimeException("Expected '" + BOLD_ITALIC + "'");
                break;
            case RAW:
                isRaw = !isRaw;
                out.setRaw(isRaw);
                break;
            default:
                out.print(ch);
            }
        }
    }
}
