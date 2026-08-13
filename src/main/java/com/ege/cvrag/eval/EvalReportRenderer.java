package com.ege.cvrag.eval;

import com.ege.cvrag.model.eval.CaseResult;
import com.ege.cvrag.model.eval.EvalReport;

/** Renders an {@link EvalReport} as a human-readable Markdown document. */
public final class EvalReportRenderer {

    private EvalReportRenderer() {
        // utility class
    }

    public static String toMarkdown(EvalReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# RAG Evaluation Report\n\n");
        sb.append("- Cases: ").append(report.totalCases()).append('\n');
        sb.append("- Retrieval k: ").append(report.evalTopK()).append('\n');
        sb.append(String.format("- Recall@%d: %.2f%n", report.evalTopK(), report.recallAtK()));
        sb.append(String.format("- MRR: %.2f%n", report.meanReciprocalRank()));
        sb.append(String.format("- Mean keyword coverage: %.2f%n%n", report.meanKeywordCoverage()));

        sb.append("| Question | Expected section | Hit | RR | Keyword cov. |\n");
        sb.append("|---|---|:--:|:--:|:--:|\n");
        for (CaseResult r : report.results()) {
            sb.append("| ").append(r.question())
                    .append(" | ").append(r.expectedSection())
                    .append(" | ").append(r.retrievalHit() ? "✅" : "❌")
                    .append(String.format(" | %.2f | %.2f |%n", r.reciprocalRank(), r.keywordCoverage()));
        }
        return sb.toString();
    }
}
