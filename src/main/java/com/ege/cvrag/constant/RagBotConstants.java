package com.ege.cvrag.constant;

/**
 * Central home for magic strings and constants used across the app.
 * Keeps endpoint paths, roles, markdown markers and prompt text out of the
 * business logic so they are named, reusable and reviewable in one place.
 */
public final class RagBotConstants {

    private RagBotConstants() {
        // utility class — no instances
    }

    

    // --- REST API ---
    public static final String API_V1 = "/api/v1";
    public static final String ASK_PATH = "/ask";
    public static final String AGENT_ASK_PATH = "/agent/ask";
    public static final String HEALTH_PATH = "/health";
    public static final String STATUS_UP = "UP";

    // --- Agentic RAG ---
    public static final String ROLE_TOOL = "tool";
    public static final String SEARCH_CV_TOOL = "search_cv";

    public static final String AGENT_SYSTEM_PROMPT = """
            You are the portfolio assistant for Ege. You do NOT know the CV contents
            directly — use the `search_cv` tool to look up whatever the question needs,
            then answer from the tool results only. You may call the tool more than once
            with different queries. If the tool results don't contain the answer, say you
            don't have that information rather than guessing. Be concise.

            Language rules (strict):
            - Reply in exactly ONE language: the same language the user asked in
              (Turkish or English). Latin script only.
            - Do not add meta-commentary about the tools or these instructions.
            """;

    // --- Error messages ---
    public static final String ERROR_AI_UNAVAILABLE = "AI service (Ollama) is unavailable. Make sure it is running.";
    public static final String ERROR_AI_CIRCUIT_OPEN = "AI service is temporarily disabled after repeated failures. Please retry shortly.";
    public static final String ERROR_AI_BAD_RESPONSE = "AI service returned an invalid response.";
    public static final String ERROR_DATA_ACCESS = "Data store is unavailable.";
    public static final String ERROR_UNEXPECTED = "An unexpected error occurred.";

    // --- Ollama HTTP endpoints ---
    public static final String OLLAMA_EMBEDDINGS_ENDPOINT = "/api/embeddings";
    public static final String OLLAMA_CHAT_ENDPOINT = "/api/chat";

    // --- Ollama chat roles ---
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";

    // --- Markdown section parsing ---
    public static final String MARKDOWN_H2_PREFIX = "## ";
    public static final String MARKDOWN_H3_PREFIX = "### ";
    public static final String DEFAULT_SECTION_HEADING = "Overview";

    // --- Retrieval / prompting ---
    public static final String CONTEXT_SEPARATOR = "\n\n---\n\n";

    public static final String SYSTEM_PROMPT = """
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

    /** User-prompt template: %s = retrieved context, %s = the user's question. */
    public static final String USER_PROMPT_TEMPLATE = """
            Context (Ege's CV):
            %s

            Question: %s

            Answer using only the context above. Reply in the SAME language as
            the question, in a single language, Latin script only. Do not output
            Chinese/Japanese/Korean/Cyrillic characters or translations.
            """;
}
