package com.ege.cvrag.eval;

import com.ege.cvrag.model.eval.EvalCase;
import com.ege.cvrag.model.eval.EvalReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The full RAG evaluation harness. It needs a live Ollama + pgvector, so it is
 * OFF by default and CI skips it. Run it locally against the running stack:
 *
 *   mvn test -Deval=true -Dtest=RagEvaluationHarnessTest
 *
 * It writes a Markdown report to target/eval-report.md and prints it.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "eval", matches = "true")
class RagEvaluationHarnessTest {

    @Autowired
    private RagEvaluator evaluator;

    @Test
    void runEvaluation() throws Exception {
        List<EvalCase> cases = loadCases();

        EvalReport report = evaluator.evaluate(cases);
        String markdown = EvalReportRenderer.toMarkdown(report);

        Files.writeString(Path.of("target/eval-report.md"), markdown);
        System.out.println("\n" + markdown);

        assertThat(report.totalCases()).isEqualTo(cases.size());
        assertThat(report.recallAtK()).isBetween(0.0, 1.0);
        assertThat(report.meanReciprocalRank()).isBetween(0.0, 1.0);
    }

    private List<EvalCase> loadCases() throws Exception {
        try (InputStream in = new ClassPathResource("eval/cases.json").getInputStream()) {
            return new ObjectMapper().readValue(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    new TypeReference<List<EvalCase>>() {});
        }
    }
}
