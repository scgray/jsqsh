package org.sqsh;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.junit.Test;
import org.junit.Assert;
import org.sqsh.MarkdownFormatter.Decoration;
import org.sqsh.MarkdownFormatter.WrappingStream;
import org.sqsh.MarkdownFormatter.Line;

import static org.sqsh.MarkdownFormatter.Decoration.OFF_ESC;
import static org.sqsh.MarkdownFormatter.Decoration.BOLD_ESC;
import static org.sqsh.MarkdownFormatter.Decoration.ITALIC_ESC;
import static org.sqsh.MarkdownFormatter.Decoration.CODE_ESC;

public class MarkdownTest {
    
    private static final String BOLD_CODE_ESC = Decoration.getEscape(
                    Decoration.CODE | Decoration.BOLD);
    private static final String ITALIC_CODE_ESC = Decoration.getEscape(
                    Decoration.CODE | Decoration.ITALIC);

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
        Assert.assertEquals(Line.Type.HEADER, line.type);
        Assert.assertEquals("Header 1", line.getContent());
        Assert.assertEquals(1, line.level);
        Assert.assertEquals(0, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals("This is an example of some text in a markdown file",
           line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals("that we will process with the line processor",
           line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals("", line.getContent());
        Assert.assertEquals(0, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.ULIST, line.type);
        Assert.assertEquals("And a bulleted list", line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.ULIST, line.type);
        Assert.assertEquals("With some subbullets", line.getContent());
        Assert.assertEquals(2, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.ULIST, line.type);
        Assert.assertEquals("Here is a sub-bullet", line.getContent());
        Assert.assertEquals(4, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.ULIST, line.type);
        Assert.assertEquals("And another", line.getContent());
        Assert.assertEquals(6, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals("And the start of a new paragraph", line.getContent());
        Assert.assertEquals(1, line.indent);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals("With a code block", line.getContent());
        Assert.assertEquals(4, line.indent);

        Assert.assertFalse(line.next());
        Assert.assertFalse(line.next());
        input =
            "this is line one\n"
          + " * bullet\n"
          + " * bullet\n"
          + " \n"
          + "paragraph\n";
          
        line.reset(input);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals("this is line one", line.getContent());
        Assert.assertEquals(0, line.indent);
        Assert.assertEquals(0, line.contentStart);
        Assert.assertEquals(0, line.start);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.ULIST, line.type);
        Assert.assertEquals("bullet", line.getContent());
        Assert.assertEquals(17, line.start);
 
        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.ULIST, line.type);
        Assert.assertEquals("bullet", line.getContent());
        Assert.assertEquals(27, line.start);

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals(37, line.start);
        Assert.assertEquals(1, line.indent);
        Assert.assertEquals(38, line.contentEnd);
        Assert.assertEquals(39, line.idx);
        Assert.assertTrue(line.isEmpty());

        Assert.assertTrue(line.next());
        Assert.assertEquals(Line.Type.TEXT, line.type);
        Assert.assertEquals("paragraph", line.getContent());
        Assert.assertEquals(39, line.start);
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
            "  word wrapped\n", 
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
            "  smultiple lines\n",
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
            "  lines\n",
            result);

        bytes.reset();
        print(out, "This     has        trailing spaces                  ");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
              "This has trailing\n" +
            "  spaces\n",
            result);

        bytes.reset();
        print(out, "Wrap at exactly 20 test");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
              "Wrap at exactly 20\n" +
            "  test\n",
            result);

        bytes.reset();
        // Simple word wrapping test
        print(out, "^This is^ /some/ simple text");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            BOLD_ESC+"This is"+OFF_ESC+" "+ITALIC_ESC+"some"+OFF_ESC+"\n" +
            "  simple text\n",
            result);

        bytes.reset();
        // Test a decoration floating between spaces
        print(out, "Test ^ float bold^");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test "+BOLD_ESC+"float bold"+OFF_ESC+"\n",
            result);

        bytes.reset();
        // Test a decoration floating between spaces and wrapping to another line
        print(out, "Test ^ float bold with^ wrap");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test " + BOLD_ESC + "float bold" + OFF_ESC + "\n"
                + "  " + BOLD_ESC + "with" + OFF_ESC + " wrap\n",
            result);

        bytes.reset();
        // Make sure decoration closes itself
        print(out, "Test ^ abandoned decoration");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test " + BOLD_ESC + "abandoned" + OFF_ESC + "\n"
                + "  " + BOLD_ESC + "decoration" + OFF_ESC + "\n",
            result);

        bytes.reset();
        // Test raw mode -- are white spaces retained? Is the style wrapped?
        print(out, "Test `space`");
        out.flush();
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test "+CODE_ESC+"space"+OFF_ESC+"\n",
            result);

        bytes.reset();
        // Test raw mode -- are white spaces retained? Is the style wrapped?
        print(out, "Test`nospace`");
        out.flush();
        result = bytes.toString();
        //System.out.println(result);
        Assert.assertEquals(
            "Test"+CODE_ESC+"nospace"+OFF_ESC+"\n",
            result);

        // Test where adding the space for the raw text would cause
        // it to wrap and the space shouldn't be printed.
        bytes.reset();
        print(out, "Test wrap the raw `foo`");
        out.flush();
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test wrap the raw"+OFF_ESC+"\n" +
             "  "+CODE_ESC+CODE_ESC+"foo"+OFF_ESC+"\n",
            result);

        bytes.reset();
        // Test raw mode -- are white spaces retained? Is the style wrapped?
        print(out, "Test `   are  white  spaces  retained?`");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test "+CODE_ESC+"are white"+OFF_ESC+"\n"
                + "  "+CODE_ESC+"spaces retained?"+OFF_ESC+"\n",
            result);

        bytes.reset();
        // Test raw mode -- are white spaces retained? Is the style wrapped?
        print(out, "Test`   are  white  spaces  retained?`");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test"+CODE_ESC+" are white"+OFF_ESC+"\n"
                + "  "+CODE_ESC+"spaces retained?" + OFF_ESC + "\n",
            result);

        bytes.reset();
        print(out, "aa`bb`cc");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "aa"+CODE_ESC+"bb"+OFF_ESC+"cc\n",
            result);

        bytes.reset();
        print(out, "Simple ^bold^ word");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Simple "+BOLD_ESC+"bold"+OFF_ESC+" word\n",
            result);

        bytes.reset();
        // Test raw + BOLDs
        print(out, "Test ^`bold and raw with wrapping` of text^");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test "+BOLD_ESC+OFF_ESC+BOLD_CODE_ESC+"bold and raw"+OFF_ESC+"\n"
                + "  "+BOLD_CODE_ESC+"with wrapping"+OFF_ESC+BOLD_ESC+" of"+OFF_ESC+"\n"
                + "  "+BOLD_ESC+"text"+OFF_ESC+"\n",
            result);

        bytes.reset();
        // Test raw + ITALIC
        print(out, "Test /`italic and raw with wrapping` of text/");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
            "Test "+ITALIC_ESC+OFF_ESC+ITALIC_CODE_ESC+"italic and"+OFF_ESC+"\n"
                + "  "+ITALIC_CODE_ESC+"raw with wrapping"+OFF_ESC+ITALIC_ESC+OFF_ESC+"\n"
                + "  "+ITALIC_ESC+"of text"+OFF_ESC+"\n",
            result);

        bytes.reset();
        print(out, "Test\nwrapping with explicit newlines");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
              "Test\n"+
            "  wrapping with\n" +
            "  explicit newlines\n",
            result);

        bytes.reset();
        print(out, "^Test\nwrapping^ with explicit newlines");
        out.flush();
        
        result = bytes.toString();
        // System.out.println(result);
        Assert.assertEquals(
              BOLD_ESC+"Test"+OFF_ESC+"\n"+
            "  "+BOLD_ESC+"wrapping"+OFF_ESC+" with\n" +
            "  explicit newlines\n",
            result);
    }
    
    @Test
    public void testBasicMarkdown() throws Exception {
        
        runTest("test1");
    }

    @Test
    public void testBulletedLists() throws Exception {
        
        runTest("bullets");
    }

    @Test
    public void testNumberedLists() throws Exception {
        
        runTest("numbers");
    }
    
    private void print(WrappingStream out, String str) {
        
        int len = str.length();
        int i = 0;
        int decoration = Decoration.OFF;
        boolean isCode = false;
        
        while (i < len) {
            
            char ch = str.charAt(i++);
            switch (ch) {
            case '^':
                if (decoration == Decoration.OFF) {

                    out.bold(true);
                    decoration = Decoration.BOLD;
                }
                else if (decoration == Decoration.BOLD) {

                    out.bold(false);
                    decoration = Decoration.OFF;
                }
                else
                    throw new RuntimeException("Expected '^'");
                break;
            case '/':
                if (decoration == Decoration.OFF) {

                    out.italic(true);
                    decoration = Decoration.ITALIC;
                }
                else if (decoration == Decoration.ITALIC) {

                    out.italic(false);
                    decoration = Decoration.OFF;
                }
                else
                    throw new RuntimeException("Expected '/'");
                break;
            case '`':
                isCode = !isCode;
                out.code(isCode);
                break;
            default:
                out.print(ch);
            }
        }
    }
    
    private void runTest(String testName) throws Exception {

        String tmpDir = System.getProperty("test.tmp.dir");
        Assert.assertTrue("Property test.tmp.dir is not set!", tmpDir != null);
        
        File file = new File(tmpDir);
        Assert.assertTrue(tmpDir + " does not exist or is not a directory", 
           file.isDirectory());
        
        InputStream srcStream = MarkdownTest.class.getResourceAsStream("/"+testName + ".md");
        Assert.assertNotNull("Could not find resource " + testName + ".md", srcStream);

        InputStream goldStream = MarkdownTest.class.getResourceAsStream("/"+testName + ".gold");
        Assert.assertNotNull("Could not find resource " + testName + ".gold", goldStream);

        File outFile = new File(tmpDir + "/" + testName + ".out");
        PrintStream out = new PrintStream(new FileOutputStream(outFile));
        
        StringBuilder sb = new StringBuilder();
        BufferedReader srcReader = new BufferedReader(new InputStreamReader(srcStream));
        String line;
        while ((line = srcReader.readLine()) != null) {
            
            sb.append(line).append('\n');
        }
        srcReader.close();
        
        MarkdownFormatter formatter = new MarkdownFormatter(40, out);
        formatter.format(sb.toString());
        out.close();
        
        InputStream actualStream = new FileInputStream(outFile);
        boolean ok = diff(actualStream, goldStream);
        actualStream.close();
        goldStream.close();
        
        Assert.assertTrue(testName + ".gold differs from "
              + outFile, ok);
        
        if (ok)  {

            outFile.delete();
        }
    }
    
    /**
     * Diffs the actual output & the gold output
     * @throws Exception Thrown if something fails or there are differences
     */
    private boolean diff(InputStream actualFile, InputStream goldFile) throws Exception {

        BufferedReader actual = new BufferedReader(new InputStreamReader(actualFile));
        BufferedReader gold = new BufferedReader(new InputStreamReader(goldFile));
        String goldLine;
        String actualLine;
        
        while ((goldLine = gold.readLine()) != null) {
            
            if ((actualLine = actual.readLine()) == null) {
                
                return false;
            }
            
            if (! goldLine.equals(actualLine)) {
                
                return false;
            }
        }
        
        if (actual.readLine() != null) {
            
            return false;
        }
        
        return true;
    }
}
