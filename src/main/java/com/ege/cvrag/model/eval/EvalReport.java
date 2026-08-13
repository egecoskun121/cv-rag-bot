package com.ege.cvrag.model.eval;

import java.util.List;

/**
 * Aggregate evaluation results across all cases.
 *
 * @param totalCases          number of cases evaluated
 * @param evalTopK            the k used for retrieval metrics
 * @param recallAtK           fraction of cases whose expected section was retrieved
 * @param meanReciprocalRank  average reciprocal rank of the expected section (MRR)
 * @param meanKeywordCoverage average keyword coverage of the answers
 * @param results             per-case details
 */
public record EvalReport(int totalCases,
                         int evalTopK,
                         double recallAtK,
                         double meanReciprocalRank,
                         double meanKeywordCoverage,
                         List<CaseResult> results) {}
