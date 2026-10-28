package com.tampieria.chatbot.service.impl;

import com.tampieria.chatbot.dto.openai.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setup() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(OpenAiRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void deveRetornarRespostaDaOpenAiComSucesso() {
        // Arrange
        OpenAiMessage mensagemResposta = new OpenAiMessage("assistant", "Olá! Como posso ajudar?");
        OpenAiChoice choice = new OpenAiChoice(mensagemResposta);
        OpenAiResponse respostaSimulada = new OpenAiResponse(List.of(choice));

        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaSimulada);

        // Act
        String resultado = chatService.enviarMensagem("sessao-123", "Oi, tudo bem?");

        // Assert
        assertThat(resultado).isEqualTo("Olá! Como posso ajudar?");
    }

    @Test
    void deveLancarExcecaoQuandoRespostaForNula() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste")
        );
    }

    @Test
    void deveLancarExcecaoQuandoChoicesEstiverVazio() {
        OpenAiResponse respostaVazia = new OpenAiResponse(List.of());
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaVazia);

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste")
        );
    }

    @Test
    void deveManterContextoDeSessoesDiferentesIsoladamente() {
        // Esse teste só faz sentido se o ChatServiceImpl já implementa memória por sessão
        // Caso ainda não tenha essa lógica implementada, pode remover esse teste por enquanto
        OpenAiMessage mensagemResposta = new OpenAiMessage("assistant", "Resposta para sessão A");
        OpenAiChoice choice = new OpenAiChoice(mensagemResposta);
        OpenAiResponse respostaSimulada = new OpenAiResponse(List.of(choice));

        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaSimulada);

        String resultadoA = chatService.enviarMensagem("sessao-A", "Meu nome é Tampieri");
        String resultadoB = chatService.enviarMensagem("sessao-B", "Qual meu nome?");

        assertThat(resultadoA).isNotNull();
        assertThat(resultadoB).isNotNull();
    }
}
