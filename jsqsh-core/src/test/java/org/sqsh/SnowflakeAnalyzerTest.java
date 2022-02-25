package org.sqsh;

import org.junit.Test;
import org.sqsh.analyzers.SnowflakeAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that the SnowflakeAnalyzer can properly determine when the terminator character ';'
 * is actually the terminator character and not ending a line inside of a Snowscript block.
 */
public class SnowflakeAnalyzerTest {

    private enum Type {
        /**
         * Queries marked as @positive are "positive only", meaning that they should always
         * find the terminator character.
         */
        POSITIVE_ONLY,
        /**
         * Queries marked as @negative are "negative only", meaning that they should never
         * find the terminator character.
         */
        NEGATIVE_ONLY,
        /**
         *
         * Queries marked as @negativePositive will never find the terminator character, as written
         * in the test file, but they will be tried again by appending the terminator character and
         * should find it.
         */
        NEGATIVE_AND_POSITIVE,
    }

    private static class Query {
        private final Type type;
        private final String query;

        public Query(Type type, String query) {
            this.type = type;
            this.query = query;
        }
    }

    @Test
    public void testAnalysis() throws Exception {
        test("/snowflake_analyzer.txt");
    }

    @Test
    public void testExamples() throws Exception {
        test("/snowflake_docs.txt");
    }

    private void test(String resource) throws Exception {
        List<Query> queries = loadQueries(resource);
        SnowflakeAnalyzer analyzer = new SnowflakeAnalyzer();

        for (Query query : queries) {
            switch (query.type) {
                case NEGATIVE_ONLY:
                    assertNotTerminated(analyzer, query.query);
                    break;

                case POSITIVE_ONLY:
                    assertTerminated(analyzer, query.query);
                    break;

                case NEGATIVE_AND_POSITIVE:
                    assertNotTerminated(analyzer, query.query);
                    assertTerminated(analyzer, query.query + ";");
                    break;
            }
        }
    }


    private void assertTerminated(SnowflakeAnalyzer analyzer, String text) {
        assertThat(analyzer.isTerminated(text, ';'))
                .as("Query should be terminated: %s", text)
                .isTrue();
    }

    private void assertNotTerminated(SnowflakeAnalyzer analyzer, String text) {
        assertThat(analyzer.isTerminated(text, ';'))
                .as("Query should not be terminated: %s", text)
                .isFalse();
    }

    List<Query> loadQueries(String filename) throws IOException {
        InputStream maybeStream = SnowflakeAnalyzerTest.class.getResourceAsStream(filename);
        assertThat(maybeStream).isNotNull();

        List<Query> queries = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        Type testType = Type.POSITIVE_ONLY;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(maybeStream))) {
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#")) {
                    line = reader.readLine();
                    continue;
                }

                if (line.startsWith("@")) {
                    if (sb.length() > 0) {
                        queries.add(new Query(testType, trim(sb)));
                        sb.setLength(0);
                    }

                    if (line.startsWith("@negativePositive")) {
                        testType = Type.NEGATIVE_AND_POSITIVE;
                    } else if (line.startsWith("@negative")) {
                        testType = Type.NEGATIVE_ONLY;
                    } else if (line.startsWith("@positive")) {
                        testType = Type.POSITIVE_ONLY;
                    } else {
                        throw new AssertionError("Unknown test type: " + line);
                    }
                } else {
                    sb.append(line).append("\n");
                }
                line = reader.readLine();
            }
        }
        if (sb.length() > 0) {
            queries.add(new Query(testType, trim(sb)));
        }

        return queries;
    }

    private String trim(StringBuilder sb) {
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

}
