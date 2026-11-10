package com.tampieria.chatbot.service;

import com.tampieria.chatbot.dto.openai.OpenAiMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HistoricoLimitador {

    private final int maxCaracteres;

    public HistoricoLimitador(int maxCaracteres) {
        this.maxCaracteres = maxCaracteres;
    }

    public List<OpenAiMessage> limitar(List<OpenAiMessage> mensagens) {
        throw new UnsupportedOperationException("ainda não implementado");
    }
}
