package com.tampieria.chatbot.service;

import com.tampieria.chatbot.dto.openai.OpenAiMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HistoricoLimitador {

    private static final String ROLE_SYSTEM = "system";

    private final int maxCaracteres;

    public HistoricoLimitador(int maxCaracteres) {
        this.maxCaracteres = maxCaracteres;
    }

    public List<OpenAiMessage> limitar(List<OpenAiMessage> mensagens) {
        List<OpenAiMessage> sistema = mensagens.stream()
                .filter(m -> ROLE_SYSTEM.equals(m.role()))
                .toList();
        List<OpenAiMessage> conversa = mensagens.stream()
                .filter(m -> !ROLE_SYSTEM.equals(m.role()))
                .toList();

        int total = sistema.stream().mapToInt(HistoricoLimitador::getLength).sum();
        Deque<OpenAiMessage> mantidas = new ArrayDeque<>();

        // percorre da mais recente para a mais antiga
        for (int i = conversa.size() - 1; i >= 0; i--) {
            int tamanho = getLength(conversa.get(i));
            // a última mensagem (primeira da iteração) é sempre mantida
            if (total + tamanho > maxCaracteres && !mantidas.isEmpty()) {
                break;
            }
            total += tamanho;
            mantidas.addFirst(conversa.get(i));
        }

        List<OpenAiMessage> resultado = new ArrayList<>(sistema);
        resultado.addAll(mantidas);
        return resultado;
    }

    private static int getLength(OpenAiMessage m) {
        return m.content().length();
    }
}
