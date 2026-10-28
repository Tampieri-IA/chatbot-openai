package com.tampieria.chatbot.service.impl;

import com.tampieria.chatbot.service.ChatService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock") // só carrega quando o perfil "mock" estiver ativo
public class ChatServiceMock implements ChatService {

    @Override
    public String enviarMensagem(String sessaoId, String mensagem) {
        return "Resposta simulada (mock): você disse " + mensagem + ". "
                + "Essa é uma resposta de teste, sem chamar a API da OpenAI.";
    }
}
