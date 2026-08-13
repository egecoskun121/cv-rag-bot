package com.ege.cvrag.model.eval;

import java.util.List;

/**
 * One evaluation case for the RAG harness.
 *
 * @param question         the question to ask
 * @param expectedSection  the CV section that should be retrieved for a correct answer
 * @param expectedKeywords keywords a correct answer is expected to contain
 */
public record EvalCase(String question, String expectedSection, List<String> expectedKeywords) {}
