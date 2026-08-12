package com.ege.cvrag.web;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.chat.AskRequest;
import com.ege.cvrag.model.chat.AskResponse;
import com.ege.cvrag.rag.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RagBotConstants.API_V1)
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(RagBotConstants.ASK_PATH)
    public ResponseEntity<AskResponse> ask(@RequestBody AskRequest request) {
        log.info("Received question: {}", request.question());
        AskResponse response = new AskResponse(ragService.ask(request.question()));
        return ResponseEntity.ok(response);
    }
}
