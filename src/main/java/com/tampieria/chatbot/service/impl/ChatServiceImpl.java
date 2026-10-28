package com.tampieria.chatbot.service.impl;

import com.tampieria.chatbot.dto.openai.OpenAiMessage;
import com.tampieria.chatbot.dto.openai.OpenAiRequest;
import com.tampieria.chatbot.dto.openai.OpenAiResponse;
import com.tampieria.chatbot.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("!mock") // só carrega quando o perfil "mock" NÃO estiver ativo
public class ChatServiceImpl implements ChatService {

    private final RestClient restClient;
    private final Map<String, List<OpenAiMessage>> historicoPorSessao = new ConcurrentHashMap<>();

    @Value("${openai.api.model}")
    private String model;

    public ChatServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @param sessaoId
     * @param mensagemUsuario
     * @return
     */
    @Override
    public String enviarMensagem(String sessaoId, String mensagemUsuario) {
        List<OpenAiMessage> historico = historicoPorSessao.computeIfAbsent(
                sessaoId, k -> new ArrayList<>(List.of(
                        new OpenAiMessage("system", "Você é um assistente útil direto.")
                ))
        );

        historico.add(new OpenAiMessage("user", mensagemUsuario));
        OpenAiRequest request = new OpenAiRequest(model, historico);

        OpenAiResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenAiResponse.class);

        String resposta = response.choices().get(0).message().content();
        historico.add(new OpenAiMessage("assistant", resposta));
        return resposta;
    }
}
