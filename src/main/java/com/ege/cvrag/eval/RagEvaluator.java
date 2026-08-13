package com.ege.cvrag.eval;

import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.model.cv.CvChunk;
import com.ege.cvrag.model.eval.CaseResult;
import com.ege.cvrag.model.eval.EvalCase;
import com.ege.cvrag.model.eval.EvalReport;
import com.ege.cvrag.rag.RagService;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs a gold set of {@link EvalCase}s through the live pipeline and scores them.
 *
 * Retrieval metrics use their own {@code evalTopK} (smaller than the production
 * top-k) so Recall@k and MRR actually discriminate — with production top-k
 * covering the whole CV, recall would trivially be 1.0. Answer quality uses the
 * real {@link RagService} (production top-k) and keyword coverage.
 *
 * This needs Ollama + pgvector, so it runs as a local tool, not in CI. The pure
 * scoring in {@link RetrievalMetrics} is what CI unit-tests.
 */
@Component
public class RagEvaluator {

    private final OllamaClient ollama;
    private final PgVectorStore vectorStore;
    private final RagService ragService;
    private final int evalTopK;

    public RagEvaluator(OllamaClient ollama,
                        PgVectorStore vectorStore,
                        RagService ragService,
                        @Value("${app.eval.top-k}") int evalTopK) {
        this.ollama = ollama;
        this.vectorStore = vectorStore;
        this.ragService = ragService;
        this.evalTopK = evalTopK;
    }

    public EvalReport evaluate(List<EvalCase> cases) {
        List<CaseResult> results = cases.stream()
                .map(this::evaluateCase)
                .toList();

        double recall = results.stream()
                .mapToDouble(r -> r.retrievalHit() ? 1.0 : 0.0)
                .average().orElse(0.0);
        double mrr = results.stream()
                .mapToDouble(CaseResult::reciprocalRank)
                .average().orElse(0.0);
        double coverage = results.stream()
                .mapToDouble(CaseResult::keywordCoverage)
                .average().orElse(0.0);

        return new EvalReport(results.size(), evalTopK, recall, mrr, coverage, results);
    }

    /** Runs one case through the pipeline and scores its retrieval + answer. */
    private CaseResult evaluateCase(EvalCase c) {
        float[] queryEmbedding = ollama.embed(c.question());
        List<String> sections = vectorStore.search(queryEmbedding, evalTopK).stream()
                .map(CvChunk::section)
                .toList();

        boolean hit = RetrievalMetrics.hit(sections, c.expectedSection());
        double rr = RetrievalMetrics.reciprocalRank(sections, c.expectedSection());

        String answer = ragService.ask(c.question());
        double coverage = RetrievalMetrics.keywordCoverage(answer, c.expectedKeywords());

        return new CaseResult(
                c.question(), c.expectedSection(), sections, hit, rr, coverage, answer);
    }
}
