package com.ege.cvrag.model.eval;

import java.util.List;

/**
 * The outcome of running one {@link EvalCase} through the pipeline.
 *
 * @param question         the question asked
 * @param expectedSection  the section that should have been retrieved
 * @param retrievedSections the sections actually retrieved (top-k), in rank order
 * @param retrievalHit     whether the expected section was in the retrieved set
 * @param reciprocalRank   1/rank of the expected section (0 if missing)
 * @param keywordCoverage  fraction of expected keywords present in the answer [0,1]
 * @param answer           the generated answer
 */
public record CaseResult(String question,
                         String expectedSection,
                         List<String> retrievedSections,
                         boolean retrievalHit,
                         double reciprocalRank,
                         double keywordCoverage,
                         String answer) {}
