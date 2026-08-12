package com.ege.cvrag.web;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.AskRequest;
import com.ege.cvrag.model.AskResponse;
import com.ege.cvrag.rag.RagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RagBotConstants.API_V1)
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(RagBotConstants.ASK_PATH)
    public AskResponse ask(@RequestBody AskRequest request) {
        return new AskResponse(ragService.ask(request.question()));
    }
}
