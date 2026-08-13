package com.ege.cvrag.agent;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.model.ollama.OllamaChatMessage;
import com.ege.cvrag.model.ollama.OllamaTool;
import com.ege.cvrag.model.ollama.OllamaToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agentic RAG — the agent harness. Instead of a fixed embed→search→answer
 * pipeline, the model runs in a tool-use loop and decides when (and with what
 * query) to retrieve:
 *
 *   1. send the conversation + tool definitions to the model,
 *   2. if it replies with tool calls, run each tool and append the results,
 *   3. repeat until it replies with a final answer (or maxIterations is hit).
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final OllamaClient ollama;
    private final List<OllamaTool> toolDefinitions;
    private final Map<String, AgentTool> toolsByName;
    private final int maxIterations;

    public AgentService(OllamaClient ollama,
                        List<AgentTool> tools,
                        @Value("${app.agent.max-iterations}") int maxIterations) {
        this.ollama = ollama;
        this.toolDefinitions = tools.stream().map(AgentTool::definition).toList();
        this.toolsByName = tools.stream().collect(Collectors.toMap(AgentTool::name, Function.identity()));
        this.maxIterations = maxIterations;
    }

    public String ask(String question) {
        List<OllamaChatMessage> messages = new ArrayList<>();
        messages.add(new OllamaChatMessage("system", RagBotConstants.AGENT_SYSTEM_PROMPT));
        messages.add(new OllamaChatMessage("user", question));

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            OllamaChatMessage reply = ollama.chatWithTools(messages, toolDefinitions);
            messages.add(reply);

            List<OllamaToolCall> toolCalls = reply.toolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                return contentOrEmpty(reply);            // final answer
            }

            log.info("Agent iteration {}: {} tool call(s)", iteration, toolCalls.size());
            toolCalls.stream().map(this::runTool).forEach(messages::add);
        }

        // Iterations exhausted: force a final answer with no tools available.
        log.warn("Agent hit maxIterations={}, forcing a final answer", maxIterations);
        return contentOrEmpty(ollama.chatWithTools(messages, List.of()));
    }

    private OllamaChatMessage runTool(OllamaToolCall call) {
        String toolName = call.function().name();
        AgentTool tool = toolsByName.get(toolName);
        String result = (tool == null)
                ? "Unknown tool: " + toolName
                : tool.execute(call.function().arguments());
        return new OllamaChatMessage(RagBotConstants.ROLE_TOOL, result);
    }

    private static String contentOrEmpty(OllamaChatMessage message) {
        return message.content() == null ? "" : message.content();
    }
}
