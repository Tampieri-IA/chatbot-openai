package com.tampieria.chatbot.controller;

import com.tampieria.chatbot.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void deveRetornar200ComRespostaDoChat() throws Exception {
        when(chatService.enviarMensagem("sessao-123", "Olá")).thenReturn("Oi! Tudo bem?");

        String requestBody = """
            {
                "sessaoId": "sessao-123",
                "mensagemUsuario": "Olá"
            }
            """;

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Oi! Tudo bem?"));
    }

    @Test
    void deveRetornar400QuandoMensagemForVazia() throws Exception {
        String requestBody = """
                {
                    "sessaoId": "sessao-123",
                    "mensagem": ""
                }
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}