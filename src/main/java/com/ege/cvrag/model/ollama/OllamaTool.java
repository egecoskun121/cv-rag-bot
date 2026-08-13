package com.ege.cvrag.model.ollama;

/** A tool definition advertised to the model (Ollama uses {@code {"type":"function", ...}}). */
public record OllamaTool(String type, OllamaFunctionDef function) {

    public static OllamaTool function(OllamaFunctionDef function) {
        return new OllamaTool("function", function);
    }
}
