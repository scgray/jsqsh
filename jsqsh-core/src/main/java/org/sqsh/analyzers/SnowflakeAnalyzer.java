package org.sqsh.analyzers;

import org.sqsh.SQLParseUtil;
import org.sqsh.SimpleKeywordTokenizer;

import java.util.Stack;

public class SnowflakeAnalyzer implements SQLAnalyzer {
    enum BlockType {
        CASE,
        IF,
        DO,
        LOOP,
        REPEAT,
        BLOCK
    }

    @Override
    public String getName() {
        return "Snowflake";
    }

    /**
     * Attempt to determine if a terminator character is terminating a snowflake query. Prior to the introduction
     * of Snowflake Scripting Language (basically PL/SQL), this was moderately straight forward: The terminator
     * is only considered a terminator if:
     * <ul>
     *   <li>It is not contained in a string literal (e.g. {@code 'hello;'})
     *   <li>It is not contained in a quoted identifier (e.g. {@code "hello;"})
     *   <li>It is not contained in "here" document (e.g. {@code $$ x = 10; $$})
     *   <li>It is not contained a comment (e.g. {@code -- this;} and {@code /* this *\/})
     * </ul>
     * Snowflake Scripting Language introduces a challenge for our favorite terminator "{@code ;}" due to the
     * fact that it allows for blocks of statements that are, themselves terminated. So, of course, the
     * general approach is to recognize these blocks, and ignore the terminator when contained within, but
     * how does one recognize a Snowflake Scripting Language block?
     *
     * <p>The naive approach would simply be to look for keywords like {@code BEGIN} and {@code END}, however
     * this can be challenging since you could have variables, functions, literals, etc. with these names, so
     * we have to be a little more selective about recognizing them:
     *
     * <p>There are two identifiable places that a bock can appear (based upon my read of the docs):
     * <ol>
     *   <li>An anonymous block:
     * <pre>
     *     DECLARE
     *        x int := 10;
     *     BEGIN
     *        ...
     *     END;
     * </pre>
     *       An anonymous block can only happen as the first statement in the batch, which means that we can
     *       be queued on the presence of the block if we see the {@code DECLARE} or {@code BEGIN} keywords
     *       as the very first thing in the statement.
     *   <li>The body of a procedure or function, such as:
     * <pre>
     *    CREATE PROCEDURE FOO()
     *    ...
     *    LANGUAGE SQL
     *    ...
     *    AS
     *    BEGIN
     *    ...
     *    END;
     * </pre>
     *       or
     * <pre>
     *    CREATE FUNCTION FOO()
     *    ...
     *    LANGUAGE SQL
     *    ...
     *    AS
     *    BEGIN
     *    ...
     *    END;
     * </pre>
     *      the approach taken for here is to look for the sequence {@code LANGUAGE SQL}, then search forward
     *      to the {@code AS} keyword and see if it is followed by a {@code DECLARE} or {@code BEGIN}.
     * </ol>
     *
     * @param batch The batch to analyze
     * @param terminator The terminator character
     * @return
     */
    @Override
    public boolean isTerminated(CharSequence batch, char terminator) {
        final SnowflakeTokenizer tokenizer = new SnowflakeTokenizer(batch, terminator);

        String token = tokenizer.peek();

        // Anonymous block
        if ("DECLARE".equals(token) || "BEGIN".equals(token)) {
            if (!seekToEndOfSnowscript(tokenizer)) {
                return false;
            }
            token = tokenizer.peek();
        }

        while (token != null) {
            // A PL/SQL block can appear in the body of a CREATE PROCEDURE, so check to see if that is happening
            if ("CREATE".equals(token)) {
                skipCreate(tokenizer);
            } else if (token.length() == 1 && token.charAt(0) == terminator) {
                return true;
            } else {
                token = tokenizer.next();
            }
            token = tokenizer.peek();
        }

        return false;
    }

    private void skipCreate(SnowflakeTokenizer tokenizer) {
        tokenizer.next();  // Skip CREATE keyword

        // CREATE [ OR REPLACE ] [ SECURE ] FUNCTION
        // CREATE [ OR REPLACE ] [ TEMP | TEMPORARY ] FUNCTION
        // CREATE [ OR REPLACE ] PROCEDURE

        tokenizer.skip("OR", "REPLACE");
        tokenizer.skip("SECURE");
        if (!tokenizer.skip("TEMP")) {
            tokenizer.skip("TEMPORARY");
        }

        String token = tokenizer.next();

        // Today, FUNCTIONS cannot contain naked SQL blocks, so nothing to do here.
        if (!"PROCEDURE".equals(token)) {
            tokenizer.unget(token);
            return;
        }

        // Scan forward looking for the "AS" clause, paying attention to any LANGUAGE qualifier.
        boolean isLanguageSQL = true; // Assume SQL until told otherwise
        boolean foundAs = false;
        token = tokenizer.next();
        while (!foundAs && token != null) {
            switch (token) {
                case "EXECUTE":
                    if ("AS".equals(tokenizer.peek())) {
                        tokenizer.next();
                    }
                    token = tokenizer.next();
                    break;
                case "LANGUAGE":
                    isLanguageSQL = "SQL".equals(tokenizer.next());
                    token = tokenizer.next();
                    break;
                case "AS":
                    foundAs = true;
                    break;
                default:
                    token = tokenizer.next();
                    break;
            }
        }

        // Either we haven't hit the AS block yet, or the language wasn't SQL, so no further checking to do.
        if (!foundAs || !isLanguageSQL) {
            return;
        }

        token = tokenizer.peek();
        if ("DECLARE".equals(token) || "BEGIN".equals(token)) {
            seekToEndOfSnowscript(tokenizer);
        }
    }

    private boolean seekToEndOfSnowscript(SnowflakeTokenizer tokenizer) {
        Stack<BlockType> blockStack = new Stack<>();

        String token = tokenizer.next();
        while (token != null) {
            // The DECLARE keyword can only be part of a block, and must eventually be followed by a
            // BEGIN. If we don't see the BEGIN, then the block isn't finished yet, so the token can
            // be within.
            switch (token) {
                case "CREATE":
                    skipCreate(tokenizer);
                    break;
                // CASE WHEN ... END [CASE];
                case "CASE":
                    if (isCaseWhen(tokenizer)) {
                        blockStack.push(BlockType.CASE);
                    }
                    break;
                // BEGIN ... END;
                case "BEGIN":
                    if (isBeginBlock(tokenizer)) {
                        blockStack.push(BlockType.BLOCK);
                    }
                    break;
                // IF (..) .... END IF;
                case "IF":
                    if (isIfBlock(tokenizer)) {
                        blockStack.push(BlockType.IF);
                    }
                    break;
                // FOR ... DO  ... END FOR;
                // WHILE ... DO ... END WHILE;
                case "DO":
                    blockStack.push(BlockType.DO);
                    break;
                // FOR ... LOOP ... END LOOP;
                // WHILE ... LOOP ... END LOOP;
                // LOOP ... END LOOP;
                case "LOOP":
                    blockStack.push(BlockType.LOOP);
                    break;
                // REPEAT ... UNTIL (...) END REPEAT;
                case "REPEAT":
                    blockStack.push(BlockType.REPEAT);
                    break;
                // END
                // END IF
                // END [CASE]
                // END {FOR | WHILE | LOOP}
                case "END":
                    if (!blockStack.isEmpty()) {
                        BlockType currentBlockType = blockStack.peek();
                        switch (currentBlockType) {
                            case CASE:
                                if (isEndCase(tokenizer)) {
                                    blockStack.pop();
                                }
                                break;
                            case IF:
                                if (isEndIfBlock(tokenizer)) {
                                    blockStack.pop();
                                }
                                break;
                            case REPEAT:
                                if (isEndRepeatBlock(tokenizer)) {
                                    blockStack.pop();
                                }
                                break;
                            case BLOCK:
                                if (isEndBlock(tokenizer)) {
                                    blockStack.pop();
                                }
                                break;
                            case DO:
                                if (isEndDoBlock(tokenizer)) {
                                    blockStack.pop();
                                }
                                break;
                            case LOOP:
                                if (isEndLoopBlock(tokenizer)) {
                                    blockStack.pop();
                                }
                                break;
                            default:
                                throw new AssertionError("Unknown block type: " + currentBlockType);
                        }
                        if (blockStack.isEmpty()) {
                            return true;
                        }
                    }
                    break;
                case "DECLARE":
                    // DECLARE must have a BEGIN following it, if not we are still in the DECLARE block
                    if (!seekBegin(tokenizer)) {
                        return false;
                    }
                    break;
                default:
                    break;
            }

            token = tokenizer.next();
        }

        return blockStack.isEmpty();
    }

    /**
     * Skip over a parenthesized expression, handling nested parens along the way.
     */
    private boolean skipParenExpression(SnowflakeTokenizer tokenizer) {
        String next = tokenizer.next();
        if (!"(".equals(next)) {
            tokenizer.unget(next);
            return false;
        }

        int parenCount = 1;
        next = tokenizer.next();
        while (next != null) {
            if ("(".equals(next)) {
                ++parenCount;
            }
            if (")".equals(next)) {
                --parenCount;
                if (parenCount == 0) {
                    return true;
                }
            }
            next = tokenizer.next();
        }

        return false;
    }

    private boolean isCaseWhen(SnowflakeTokenizer tokenizer) {
        // CASE [ (expression) ] WHEN
        skipParenExpression(tokenizer);

        String next = tokenizer.peek();
        if ("WHEN".equals(next)) {
            tokenizer.next();
            return true;
        }

        return false;
    }

    private boolean isIfBlock(SnowflakeTokenizer tokenizer) {
        // At least make sure there is an expression after the IF
        return skipParenExpression(tokenizer);
    }

    private boolean isEndRepeatBlock(SnowflakeTokenizer tokenizer) {
        return isFollowedBy(tokenizer, "REPEAT");
    }

    private boolean isEndIfBlock(SnowflakeTokenizer tokenizer) {
        return isFollowedBy(tokenizer, "IF");
    }

    private boolean isFollowedBy(SnowflakeTokenizer tokenizer, String token) {
        String maybeToken = tokenizer.peek();
        if (token.equals(maybeToken)) {
            tokenizer.next();
            return true;
        }
        return false;
    }

    private boolean isEndCase(SnowflakeTokenizer tokenizer) {
        // END [CASE]
        String maybeCase = tokenizer.peek();
        if ("CASE".equals(maybeCase)) {
            tokenizer.next();
        }
        return true;
    }

    private boolean isEndBlock(SnowflakeTokenizer tokenizer) {
        String next = tokenizer.peek();
        return !("IF".equals(next) || "CASE".equals(next) || "FOR".equals(next) || "LOOP".equals(next));
    }

    private boolean isEndDoBlock(SnowflakeTokenizer tokenizer) {
        // FOR ... DO ... END FOR;
        // WHILE ... DO ... END WHILE;
        String maybeForOrWhile = tokenizer.peek();
        if ("FOR".equals(maybeForOrWhile) || "WHILE".equals(maybeForOrWhile)) {
            tokenizer.next();
            return true;
        }
        return false;
    }

    private boolean isEndLoopBlock(SnowflakeTokenizer tokenizer) {
        // FOR ... LOOP ... END LOOP;
        // WHILE ... LOOP ... END LOOP;
        // LOOP ... END LOOP;
        String maybeLoop = tokenizer.peek();
        if ("LOOP".equals(maybeLoop)) {
            tokenizer.next();
            return true;
        }
        return false;
    }

    private boolean seekBegin(SnowflakeTokenizer tokenizer) {
        String tok = tokenizer.next();
        while (tok != null) {
            if ("BEGIN".equals(tok)) {
                if (isBeginBlock(tokenizer)) {
                    tokenizer.unget(tok);
                    return true;
                }
            }
            tok = tokenizer.next();
        }
        return false;
    }

    /**
     * Called after {@code BEGIN} has been hit to make sure it isn't a {@code BEGIN TRANSACTION} variant.
     */
    private boolean isBeginBlock(SimpleKeywordTokenizer tokenizer) {
        final String next = tokenizer.peek();

        // BEGIN [ { WORK | TRANSACTION } ] [ NAME <name> ]
        return !"TRANSACTION".equals(next)
                && !"WORK".equals(next)
                && !"NAME".equals(next);
    }

    private static class SnowflakeTokenizer extends SimpleKeywordTokenizer {
        public SnowflakeTokenizer(CharSequence text, char terminator) {
            super(text, terminator, true);
        }

        /**
         * Along with string literals and quoted identifier, recognize {@code $variables} as well as
         * "here" documents ({@code $$..$$}).
         */
        @Override
        protected String nextNonKeywordOrIdentifier() {
            if (idx >= len) {
                return null;
            }

           // For things like string literals, we know the terminator cannot appear inside of them,
           // so the contents are not important, but we still return a token to recognize that there
           // is something there (in case the state of the parsing depends on knowing this fact).
           switch (sql.charAt(idx)) {
               case '\'':
                   idx = SQLParseUtil.skipQuotedString(sql, len, idx);
                   return STRING_LITERAL_TOKEN;
               case '"':
                   idx = SQLParseUtil.skipQuotedString(sql, len, idx);
                   return QUOTED_IDENTIFIER_TOKEN;
               case '$':
                   return skipVariableOrHereDocument();
               default:
                   break;
           }
           return null;
       }

        /**
         * Skip a naked variable name, like {@code $foo}, or the multi-line "here" document that snowflake
         * supports of the form:
         * <pre>
         * $$
         *  ... a bunch of stuff ...
         * $$
         * </pre>
         */
       private String skipVariableOrHereDocument() {
            ++idx;
            if (idx < len) {
                if (sql.charAt(idx) == '$') {
                    skipHereDocument();
                    return STRING_LITERAL_TOKEN;
                }
            }
            skipVariable();
            return VARIABLE_TOKEN;
       }

       private int skipVariable() {
           while (idx < len) {
               char ch = sql.charAt(idx);
               if (!Character.isLetterOrDigit(ch) || ch == '_') {
                   return idx;
               }
               ++idx;
           }
           return idx;
       }

       private int skipHereDocument() {
            ++idx;
            char prev = 'a';
            while (idx < len) {
                char ch = sql.charAt(idx++);
                if (ch == '$' && prev == '$') {
                    return idx;
                }
                prev = ch;
            }
            return idx;
       }
    }
}
