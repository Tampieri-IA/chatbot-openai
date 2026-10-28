package com.tampieria.chatbot.controller;

import com.tampieria.chatbot.dto.ChatRequestDTO;
import com.tampieria.chatbot.dto.ChatResponseDTO;
import com.tampieria.chatbot.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Endpoints do chatbot com IA")
public class ChatController {

    private final ChatService chatService;


    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @Operation(summary = "Envia uma mensagem para o chatbot e recebe a resposta da IA")
    public ChatResponseDTO chat(@Valid @RequestBody ChatRequestDTO request){
        String resposta = chatService.enviarMensagem(request.sessaoId(), request.mensagemUsuario());
        return new ChatResponseDTO(resposta);
    }
}
