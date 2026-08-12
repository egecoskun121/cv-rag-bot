package com.ege.cvrag.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * The "retrieval-augmented generation" glue.
 *
 * QuestionAnswerAdvisor does the RAG dance for us on every call:
 *   1. embed the user's question,
 *   2. similarity-search the vector store for the top-K closest chunks,
 *   3. inject those chunks into the prompt as context,
 *   4. ask the LLM to answer using ONLY that context.
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

    private final ChatClient chatClient;

    public RagService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        // topK=8 comfortably covers a 1-page CV's chunks so no section
                        // (e.g. Education) is ever missed by retrieval. Lower this for
                        // large corpora where precision matters more than full recall.
                        .searchRequest(SearchRequest.builder().topK(8).build())
                        .build())
                .build();
    }

    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
