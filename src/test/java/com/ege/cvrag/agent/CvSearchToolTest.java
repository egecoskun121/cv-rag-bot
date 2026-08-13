package com.ege.cvrag.agent;

import com.ege.cvrag.model.ollama.OllamaTool;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CvSearchToolTest {

    // definition() and the blank-query branch don't touch the collaborators,
    // so null dependencies are fine for these unit tests.
    private final CvSearchTool tool = new CvSearchTool(null, null, 5);

    @Test
    void advertisesSearchCvFunctionWithRequiredQuery() {
        assertThat(tool.name()).isEqualTo("search_cv");

        OllamaTool def = tool.definition();
        assertThat(def.type()).isEqualTo("function");
        assertThat(def.function().name()).isEqualTo("search_cv");
        assertThat(def.function().parameters()).containsEntry("required", java.util.List.of("query"));
    }

    @Test
    void returnsMessageWhenQueryMissingOrBlank() {
        assertThat(tool.execute(Map.of())).isEqualTo("No query provided.");
        assertThat(tool.execute(Map.of("query", "   "))).isEqualTo("No query provided.");
        assertThat(tool.execute(null)).isEqualTo("No query provided.");
    }
}
