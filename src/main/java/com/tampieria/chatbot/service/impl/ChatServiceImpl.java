package com.tampieria.chatbot.service.impl;

import com.tampieria.chatbot.dto.openai.OpenAiMessage;
import com.tampieria.chatbot.dto.openai.OpenAiRequest;
import com.tampieria.chatbot.dto.openai.OpenAiResponse;
import com.tampieria.chatbot.entity.Mensagem;
import com.tampieria.chatbot.entity.enums.RoleEnum;
import com.tampieria.chatbot.exception.OpenAiException;
import com.tampieria.chatbot.repository.MensagemRepository;
import com.tampieria.chatbot.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Profile("!mock") // só carrega quando o perfil "mock" NÃO estiver ativo
public class ChatServiceImpl implements ChatService {

    private static final String SYSTEM_PROMPT = "Você é um assistente útil e direto.";
    private final RestClient restClient;
    private final MensagemRepository repository;
    private final String model;
    private final int historicoMax;

    public ChatServiceImpl(RestClient restClient,
                           MensagemRepository repository,
                           @Value("${openai.api.model}") String model,
                           @Value("${chat.historic.max:20}") int historicoMax) {
        this.restClient = restClient;
        this.repository = repository;
        this.model = model;
        this.historicoMax = historicoMax;
    }

    @Override
    public String enviarMensagem(String sessaoId, String mensagemUsuario) {
        // 1. salva a mensagem do usuario (transação curta, via repository)
        repository.save(Mensagem.builder()
                .sessaoId(sessaoId)
                .role(RoleEnum.USER)
                .conteudo(mensagemUsuario)
                .build());

        // 2. busca as ultimas N mensagens (ja inclui a que acabou de salvar)
        List<Mensagem> recentes = new ArrayList<>(
                repository.findBySessaoIdOrderByCriadoEmDescIdDesc(
                        sessaoId, PageRequest.of(0, historicoMax)));
        Collections.reverse(recentes);

        // 3. monta o payload: system + histórico
        List<OpenAiMessage> payload = new ArrayList<>();
        payload.add(new OpenAiMessage(RoleEnum.SYSTEM.getValor(), SYSTEM_PROMPT));
        recentes.forEach(m -> payload.add(new OpenAiMessage(m.getRole().getValor(), m.getConteudo())));

        // 4. chama a OpenAI, sem transação aberta
        OpenAiResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OpenAiRequest(model, payload))
                    .retrieve()
                    .body(OpenAiResponse.class);
        } catch (RestClientException e) {
            throw new OpenAiException("Falha ao chamar a OpenAI: " + e.getMessage(), e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new OpenAiException("Resposta vazia da OpenAI");
        }
        String resposta = response.choices().get(0).message().content();
        if (resposta == null || resposta.isBlank()) {
            throw new OpenAiException("Resposta vazia da OpenAI");
        }

        // 5. salva a resposta
        repository.save(Mensagem.builder()
                .sessaoId(sessaoId)
                .role(RoleEnum.ASSISTANT)
                .conteudo(resposta)
                .build());

        return resposta;
    }
}
