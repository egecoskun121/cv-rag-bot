package com.ege.cvrag.web;

import com.ege.cvrag.agent.AgentService;
import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.chat.AskRequest;
import com.ege.cvrag.model.chat.AskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agentic RAG endpoint: the model decides when/what to retrieve via tools. */
@RestController
@RequestMapping(RagBotConstants.API_V1)
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping(RagBotConstants.AGENT_ASK_PATH)
    public ResponseEntity<AskResponse> ask(@RequestBody AskRequest request) {
        log.info("Received agent question: {}", request.question());
        return ResponseEntity.ok(new AskResponse(agentService.ask(request.question())));
    }
}
