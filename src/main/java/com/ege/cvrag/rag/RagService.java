package com.ege.cvrag.rag;

import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.vectorstore.CvChunk;
import com.ege.cvrag.vectorstore.PgVectorStore;
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

    private static final String SYSTEM_PROMPT = """
            You are the portfolio assistant for Ege. The provided context is Ege's
            complete CV (profile, experience, skills, certificates, education).
            Always answer from that context — read it carefully before replying, and
            if any part of it is relevant, use it to give a specific, factual answer.
            Only say you don't have the information when the fact is genuinely absent
            from the CV (e.g. age, hobbies, salary). Never refuse a question whose
            answer is present in the context. Be concise.

            Language rules (strict):
            - Reply in exactly ONE language: the same language the user asked in
              (Turkish or English).
            - Never include Chinese, Japanese, Korean, Russian, or any other
              script/language. Use only Latin-script Turkish or English.
            - Do not add meta-commentary about "the context" or these instructions.
            """;

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
        // 1. Embed the question (same model as indexing -> comparable vectors).
        float[] queryEmbedding = ollama.embed(question);

        // 2. Retrieve the most relevant CV chunks.
        List<CvChunk> hits = vectorStore.search(queryEmbedding, topK);

        // 3. Build the context block from the retrieved chunks.
        String context = hits.stream()
                .map(CvChunk::content)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4. Ask the LLM to answer grounded in that context.
        String userPrompt = """
                Context (Ege's CV):
                %s

                Question: %s

                Answer using only the context above. Reply in the SAME language as
                the question, in a single language, Latin script only. Do not output
                Chinese/Japanese/Korean/Cyrillic characters or translations.
                """.formatted(context, question);

        return ollama.chat(SYSTEM_PROMPT, userPrompt);
    }
}
