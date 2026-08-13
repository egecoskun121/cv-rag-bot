package com.ege.cvrag.agent;

import com.ege.cvrag.model.ollama.OllamaTool;

import java.util.Map;

/**
 * A tool the agent can call. Each tool advertises its schema ({@link #definition()})
 * and runs when the model invokes it by {@link #name()}.
 */
public interface AgentTool {

    /** The name the model uses to invoke this tool. */
    String name();

    /** The schema advertised to the model. */
    OllamaTool definition();

    /** Executes the tool with the model-supplied arguments and returns a text result. */
    String execute(Map<String, Object> arguments);
}
