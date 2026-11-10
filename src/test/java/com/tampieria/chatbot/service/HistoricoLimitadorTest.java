package com.tampieria.chatbot.service;

import com.tampieria.chatbot.dto.openai.OpenAiMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HistoricoLimitadorTest {

    private final HistoricoLimitador limitador = new HistoricoLimitador(20);

    @Test
    void deveManterTodasQuandoCabemNoLimite() {
        var msgs = List.of(
                new OpenAiMessage("user", "oi"),
                new OpenAiMessage("assistant", "olá"));

        assertThat(limitador.limitar(msgs)).hasSize(2);
    }

    @Test
    void deveDescartarMaisAntigasPrimeiro() {
        var msgs = List.of(
                new OpenAiMessage("user", "aaaaaaaaaaaa"),      // 12
                new OpenAiMessage("assistant", "bbbbbbbbbbbb"),  // 12
                new OpenAiMessage("user", "cccc"));              // 4

        var resultado = limitador.limitar(msgs);

        assertThat(resultado).extracting(OpenAiMessage::content)
                .containsExactly("bbbbbbbbbbbb", "cccc");
    }

    @Test
    void deveSempreManterAMensagemDeSistema() {
        var msgs = List.of(
                new OpenAiMessage("system", "regras do assistente"),
                new OpenAiMessage("user", "aaaaaaaaaaaaaaaaaaaaaaaa"));

        var resultado = limitador.limitar(msgs);

        assertThat(resultado.get(0).role()).isEqualTo("system");
    }

    @Test
    void deveSempreManterAUltimaMensagemMesmoSeExcederOLimite() {
        var msgs = List.of(new OpenAiMessage("user", "x".repeat(100)));

        assertThat(limitador.limitar(msgs)).hasSize(1);
    }
}
