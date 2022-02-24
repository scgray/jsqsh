package org.sqsh;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleKeywordTokenizerTest {
    @Test
    public void testCommentsIgnore() {
        validate("select 1 /* comment with ; */",
                "SELECT",
                "1");
        validate("select /* comment with ;*/1",
                "SELECT",
                "1");
        validate("select -- hello\n1",
                "SELECT",
                "1");
    }

    private void validate(String query, String...expected) {
        SimpleKeywordTokenizer tokenizer = new SimpleKeywordTokenizer(query, ';', true);
        List<String> tokens = new ArrayList<>();
        String token = tokenizer.next();
        while (token != null) {
            tokens.add(token);
            token = tokenizer.next();
        }
        assertThat(tokens)
                .as("String: %s", query)
                .containsExactly(expected);
    }
}
