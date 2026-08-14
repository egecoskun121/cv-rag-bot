package com.ege.cvrag.rag;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.kafka.QaEventPublisher;
import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.model.cv.CvChunk;
import com.ege.cvrag.model.qa.QaEvent;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
 *
 * When the Q&amp;A event stream is enabled it also emits one {@link QaEvent} per
 * answer for observability — the publisher is optional ({@link ObjectProvider}), so
 * this stays a pure RAG service when the feature is off.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final OllamaClient ollama;
    private final PgVectorStore vectorStore;
    private final ObjectProvider<QaEventPublisher> qaPublisher;
    private final int topK;

    public RagService(OllamaClient ollama,
                      PgVectorStore vectorStore,
                      ObjectProvider<QaEventPublisher> qaPublisher,
                      @Value("${app.rag.top-k}") int topK) {
        this.ollama = ollama;
        this.vectorStore = vectorStore;
        this.qaPublisher = qaPublisher;
        this.topK = topK;
    }

    /**
     * {@code @Cacheable} is inert unless {@code app.cache.enabled=true}. When
     * enabled, a cache hit skips the whole pipeline — including the Q&A event
     * publish, so a repeated question won't double-count in {@code /qa/stats}.
     *
     * {@code condition} skips caching trivially short input (not worth a slot);
     * {@code sync} means concurrent requests for the same question share one LLM
     * call instead of each triggering their own (cache-stampede protection). Note:
     * {@code sync} and {@code unless} can't be combined — Spring's sync path
     * doesn't support a post-invocation "don't cache this" decision.
     */
    @Cacheable(value = RagBotConstants.CACHE_ASK_ANSWERS, key = "#question.toLowerCase().trim()",
            condition = "#question.length() > 3", sync = true)
    public String ask(String question) {
        log.info("Answering question via RAG (topK={}): {}", topK, question);
        long start = System.currentTimeMillis();

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
        String answer = ollama.chat(RagBotConstants.SYSTEM_PROMPT, userPrompt);

        publishQaEvent(question, hits, answer, System.currentTimeMillis() - start);
        return answer;
    }

    /** Emits a Q&A observability event, only if the publisher is enabled/present. */
    private void publishQaEvent(String question, List<CvChunk> hits, String answer, long latencyMs) {
        qaPublisher.ifAvailable(publisher -> {
            List<String> sections = hits.stream().map(CvChunk::section).toList();
            double topScore = hits.isEmpty() ? 0.0 : hits.get(0).score();
            publisher.publish(new QaEvent(question, sections, topScore, answer, latencyMs, Instant.now().toString()));
        });
    }
}
