package com.ege.cvrag.agent;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.model.cv.CvChunk;
import com.ege.cvrag.model.ollama.OllamaFunctionDef;
import com.ege.cvrag.model.ollama.OllamaTool;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The retrieval tool the agent calls: embeds the query and returns the most
 * similar CV chunks. This is the same retrieval the fixed pipeline does, but here
 * the model decides when and with what query to invoke it.
 */
@Component
public class CvSearchTool implements AgentTool {

    private static final String QUERY_ARG = "query";

    private final OllamaClient ollama;
    private final PgVectorStore vectorStore;
    private final int topK;

    public CvSearchTool(OllamaClient ollama,
                        PgVectorStore vectorStore,
                        @Value("${app.agent.top-k}") int topK) {
        this.ollama = ollama;
        this.vectorStore = vectorStore;
        this.topK = topK;
    }

    @Override
    public String name() {
        return RagBotConstants.SEARCH_CV_TOOL;
    }

    @Override
    public OllamaTool definition() {
        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", Map.of(
                        QUERY_ARG, Map.of(
                                "type", "string",
                                "description", "What to look up in Ege's CV")),
                "required", List.of(QUERY_ARG));
        return OllamaTool.function(new OllamaFunctionDef(
                name(), "Search Ege's CV for information relevant to a query.", parameters));
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        Object query = arguments == null ? null : arguments.get(QUERY_ARG);
        if (query == null || query.toString().isBlank()) {
            return "No query provided.";
        }
        float[] embedding = ollama.embed(query.toString());
        List<CvChunk> hits = vectorStore.search(embedding, topK);
        if (hits.isEmpty()) {
            return "No matching CV sections found.";
        }
        return hits.stream()
                .map(CvChunk::content)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
