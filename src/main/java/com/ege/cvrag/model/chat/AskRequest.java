package com.ege.cvrag.model.chat;

/** Incoming payload for POST /ask — the user's question. */
public record AskRequest(String question) {}
