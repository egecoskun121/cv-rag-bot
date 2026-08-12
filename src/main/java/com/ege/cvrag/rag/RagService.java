package com.ege.cvrag.rag;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.model.CvChunk;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The Retrieval-Augmented Generation pipeline, wired by hand.
 *
 * On every question we do the RAG dance explicitly (what QuestionAnswerAdvisor
 * used to hide):
 *   1. embed the question with the same model used to index the CV,
 *   2. similarity-search pgvector for the top-K closest chunks,
 *   3. stitch those chunks into a context block,
 *   4. ask the LLM to answer using only that context.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final OllamaClient ollama;
    private final PgVectorStore vectorStore;
    private final int topK;

    public RagService(OllamaClient ollama,
                      PgVectorStore vectorStore,
                      @Value("${app.rag.top-k}") int topK) {
        this.ollama = ollama;
        this.vectorStore = vectorStore;
        this.topK = topK;
    }

    public String ask(String question) {
        log.info("Answering question via RAG (topK={}): {}", topK, question);

        // 1. Embed the question (same model as indexing -> comparable vectors).
        float[] queryEmbedding = ollama.embed(question);

        // 2. Retrieve the most relevant CV chunks.
        List<CvChunk> hits = vectorStore.search(queryEmbedding, topK);
        log.debug("Retrieved {} chunks: {}", hits.size(),
                hits.stream().map(CvChunk::section).toList());

        // 3. Build the context block from the retrieved chunks.
        String context = hits.stream()
                .map(CvChunk::content)
                .collect(Collectors.joining(RagBotConstants.CONTEXT_SEPARATOR));

        // 4. Ask the LLM to answer grounded in that context.
        String userPrompt = RagBotConstants.USER_PROMPT_TEMPLATE.formatted(context, question);

        return ollama.chat(RagBotConstants.SYSTEM_PROMPT, userPrompt);
    }
}
