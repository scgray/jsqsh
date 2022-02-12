package org.sqsh;

import org.assertj.core.api.Assumptions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TokenizerTest {
    @Test
    public void testBasic() throws Exception {
        assertTokens(
                tokenizer("\\echo one two 'the number three' \"the number four\";"),
                string("\\echo"),
                string("one"),
                string("two"),
                string("the number three"),
                string("the number four"),
                terminator());

        assertTokens(
                tokenizer("   \\echo one      two"),
                string("\\echo"),
                string("one"),
                string("two"));
    }

    @Test
    public void testStringsMashedTogether() throws Exception {
        assertTokens(
                tokenizer("this' is a '\"single\"\\ string"),
                string("this is a single string")
        );
    }

    @Test
    public void testRetainDoubleQuotes() throws Exception {
        final String commandLine = "word1 word2 \"a couple of words\" word3";
        assertTokens(
                Tokenizer.newBuilder(commandLine)
                        .setRetainDoubleQuotes(true)
                        .build(),
                string("word1"),
                string("word2"),
                string("\"a couple of words\""),
                string("word3"));

        // Retain double quotes is not applied to filesnames during redirection
        assertTokens(
                Tokenizer.newBuilder("\\echo \"hello\" > \"my file\".tmp")
                        .setRetainDoubleQuotes(true)
                        .build(),
                string("\\echo"),
                string("\"hello\""),
                redirect("my file.tmp"));
    }

    @Test
    public void testRetainDoubleQuotesMiddle() throws Exception {
        final String commandLine = "this' is a '\"single\"\\ string";
        assertTokens(
                Tokenizer.newBuilder(commandLine)
                        .setRetainDoubleQuotes(true)
                        .build(),
                string("this is a \"single\" string")
        );
    }

    @Test
    public void testVariableExpansion() throws Exception {
        final String commandLine = "\\echo literal $x \"text: $y\" 'text: $z' ${a}${b};";
        assertTokens(
                tokenizer(commandLine,
                        "a", "value a",
                        "b", "value b",
                        "x", "this is $x",
                        "y", "this is $y",
                        "z", "z is not expanded"),
                string("\\echo"),
                string("literal"),
                string("this is $x"),
                string("text: this is $y"),
                string("text: $z"),
                string("value avalue b"),
                terminator());
    }

    @Test
    public void testOutputOverwrite() throws Exception {
        testOutputRedirection(false);
    }

    @Test
    public void testOutputAppend() throws Exception {
        testOutputRedirection(true);
    }

    public void testOutputRedirection(boolean isAppend) throws Exception {
        final String redir = isAppend ? ">>" : ">";

        assertTokens("\\echo hello "+redir+"file.tmp",
                string("\\echo"),
                string("hello"),
                redirect("file.tmp", isAppend));

        assertTokens("\\echo hello "+redir+" file.tmp",
                string("\\echo"),
                string("hello"),
                redirect("file.tmp", isAppend));

        assertTokens("\\echo hello "+redir+" file.tmp wowsers",
                string("\\echo"),
                string("hello"),
                redirect("file.tmp", isAppend),
                string("wowsers"));

        assertTokens("\\echo hello "+redir+" \"file name\".tmp",
                string("\\echo"),
                string("hello"),
                redirect("file name.tmp", isAppend));

        assertTokens("\\echo hello 2"+redir+" \"error file\".tmp",
                string("\\echo"),
                string("hello"),
                redirect(2, "error file.tmp", isAppend));

        // FD must be immediately adjacent to redirect
        assertTokens("\\echo hello 2 "+redir+" \"error file\".tmp",
                string("\\echo"),
                string("hello"),
                string("2"),
                redirect("error file.tmp", isAppend));

        // FD's can only be single digit, so: "992>foo.txt" is token(992), token(>foo.txt)
        assertTokens("\\echo hello 992"+redir+" \"error file\".tmp",
                string("\\echo"),
                string("hello"),
                string("992"),
                redirect("error file.tmp", isAppend));

        assertTokens("\\echo hello 99 2"+redir+" \"error file\".tmp",
                string("\\echo"),
                string("hello"),
                string("99"),
                redirect(2, "error file.tmp", isAppend));

        // x=2; $x>foo.txt -> token(2) token(>foo.txt)
        assertTokens(mapExpander("x", "2"),
                "\\echo hello $x"+redir+" \"error file\".tmp",
                string("\\echo"),
                string("hello"),
                string("2"),
                redirect("error file.tmp", isAppend));

        assertThatThrownBy(() -> assertTokens("\\echo hello " + redir))
                .hasMessageContaining("Expected a target filename following redirection");

        // Must have a filename, not any other token
        assertThatThrownBy(() -> assertTokens("\\echo hello " + redir + " >"))
                .hasMessageContaining("Expected a target filename following redirection");

        // Retain double quotes doesn't apply to filenames
        assertTokens(
                Tokenizer.newBuilder("\\echo \"hi there!\" >\"filename.txt\"")
                        .setRetainDoubleQuotes(true)
                        .build(),
                string("\\echo"),
                string("\"hi there!\""),
                redirect("filename.txt"));
    }

    @Test
    public void testFdDup() throws Exception {
        assertTokens("\\echo hello 2>&1 word",
                string("\\echo"),
                string("hello"),
                dup(2, 1),
                string("word"));

        assertTokens("\\echo hello 1>&2 word",
                string("\\echo"),
                string("hello"),
                dup(1, 2),
                string("word"));

        assertTokens("\\echo hello >&2",
                string("\\echo"),
                string("hello"),
                dup(1, 2));

        assertTokens("\\echo hello 992>&2",
                string("\\echo"),
                string("hello"),
                string("992"),
                dup(1, 2));

        assertThatThrownBy(() -> assertTokens("\\echo hello 2>&"))
                .hasMessageContaining("Expected a number following file descriptor duplication token '>&'");

        assertThatThrownBy(() -> assertTokens("\\echo hello 2>& hello"))
                .hasMessageContaining("Expected a number following file descriptor duplication token '>&'");
    }

    @Test
    public void testTerminator() throws Exception {
        assertTerminator(',', "a b , c",
                string("a"),
                string("b"),
                terminator(','),
                string("c"));

        // "Normal" character as terminator (weird, I know)
        assertTerminator('b', "abc",
                string("a"),
                terminator('b'),
                string("c"));
    }

    @Test
    public void testPipe() throws Exception {
        assertTokens("\\echo blah blah 2>/tmp/error.txt | grep blah | tr [a-z] [A-Z] >/other/file.txt",
                string("\\echo"),
                string("blah"),
                string("blah"),
                redirect(2, "/tmp/error.txt", false),
                pipe("grep blah | tr [a-z] [A-Z] >/other/file.txt"));
    }

    @Test
    public void testBacktick() throws Exception {
        assertTokens("\\echo one two `echo three \"four      five\"` six",
                string("\\echo"),
                string("one"),
                string("two"),
                string("three"),
                string("four"),
                string("five"),
                string("six"));
    }

    @Test
    public void testBacktickVariableExpansion() throws Exception {
        assertTokens(
                mapExpander(
                        "a", "eh eh?",
                        "x", "1   2     3",
                        "y", "not expanded"
                ),
                "\\echo one two `echo $a \"$x\" '${y}__foo'` six",
                string("\\echo"),
                string("one"),
                string("two"),
                string("eh"),
                string("eh?"),
                string("1"),
                string("2"),
                string("3"),
                string("${y}__foo"),
                string("six"));
    }

    @Test
    public void testBacktickDisabled() throws Exception {
        assertTokens(Tokenizer.newBuilder("\\echo one two `echo a b c` six")
                        .setExpandBackTicks(false)
                        .build(),
                string("\\echo"),
                string("one"),
                string("two"),
                string("`echo"),
                string("a"),
                string("b"),
                string("c`"),
                string("six"));
    }

    @Test
    public void testBacktickNullExpansion() throws Exception {
        assumeUnix();

        // A backtick command that returns nothing isn't treated as a token
        assertTokens("\\echo one two `echo a >/dev/null` b",
                string("\\echo"),
                string("one"),
                string("two"),
                string("b"));
    }

    @Test
    public void testIFS() throws Exception {
        assumeUnix();

        assertTokens("\\echo `echo a:b:c`",
                string("\\echo"),
                string("a:b:c"));

        assertTokens(Tokenizer.newBuilder("\\echo `echo a:b:c`")
                        .setFieldSeparator(":")
                        .build(),
                string("\\echo"),
                string("a"),
                string("b"),
                string("c"));
    }

    @Test
    public void testDisableRetainingInitialEscape() throws Exception {
        assertTokens(Tokenizer.newBuilder("\\echo one")
                        .setRetainInitialEscape(false)
                        .build(),
                string("echo"),
                string("one"));
    }

    public void assumeUnix() {
        // Maybe not really enough for all operating systems?
        Assumptions.assumeThat(System.getProperty("os.name")).doesNotContain("Wind");
    }

    public void assertTerminator(int terminator, String str, Token...tokens) throws CommandLineSyntaxException {
        assertTokens(Tokenizer.newBuilder(str)
                        .setTerminator(terminator)
                        .build(),
                tokens);
    }

    private StringExpander mapExpander(String...keysAndValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return new StringExpander(map);
    }

    private Tokenizer tokenizer(String str, String...keysAndValues) {
        return tokenizer(str, mapExpander(keysAndValues));
    }

    private Tokenizer tokenizer(String str) {
        return tokenizer(str, StringExpander.ENVIRONMENT_EXPANDER);
    }

    private Tokenizer tokenizer(String str, int terminator) {
        return tokenizer(str, StringExpander.ENVIRONMENT_EXPANDER, terminator);
    }

    private Tokenizer tokenizer(String str, StringExpander stringExpander) {
        return tokenizer(str, stringExpander, ';');
    }

    private Tokenizer tokenizer(String str, StringExpander stringExpander, int terminator) {
        return Tokenizer.newBuilder(str)
                .setExpander(stringExpander)
                .setTerminator(terminator)
                .build();
    }

    private RedirectOutToken redirect(String filename) {
        return redirect(1, filename, false);
    }

    private RedirectOutToken redirect(String filename, boolean isAppend) {
        return redirect(1, filename, isAppend);
    }

    private RedirectOutToken redirect(int fd, String filename, boolean isAppend) {
        return new RedirectOutToken("", -1, fd, filename, isAppend);
    }

    private FileDescriptorDupToken dup(int fd1, int fd2) {
        return new FileDescriptorDupToken("", -1, fd1, fd2);
    }

    private StringToken string(String str) {
        return new StringToken("", -1, str);
    }

    private TerminatorToken terminator() {
        return terminator(';');
    }

    private TerminatorToken terminator(char ch) {
        return new TerminatorToken("", -1, ch);
    }

    private PipeToken pipe(String str) {
        return new PipeToken("", -1, str);
    }

    public void assertTokens(String str, Token...tokens) throws CommandLineSyntaxException {
        assertTokens(StringExpander.ENVIRONMENT_EXPANDER, str, tokens);
    }

    public void assertTokens(StringExpander stringExpander, String str, Token...tokens) throws CommandLineSyntaxException {
        assertTokens(
                Tokenizer.newBuilder(str)
                        .setExpander(stringExpander)
                        .setTerminator(';')
                        .build(),
                tokens);
    }

    private void assertTokens(Tokenizer tokenizer, Token...expectedTokens) throws CommandLineSyntaxException {
        List<Token> actualTokens = new ArrayList<>();
        Token token = tokenizer.next();
        while (token != null) {
            actualTokens.add(token);
            token = tokenizer.next();
        }
        assertThat(actualTokens).containsExactly(expectedTokens);
    }
}
