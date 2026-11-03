package com.tampieria.chatbot.service.impl;

import com.tampieria.chatbot.dto.openai.*;
import com.tampieria.chatbot.entity.Mensagem;
import com.tampieria.chatbot.entity.enums.RoleEnum;
import com.tampieria.chatbot.exception.OpenAiException;
import com.tampieria.chatbot.repository.MensagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    @Mock
    private MensagemRepository repository;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setup() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(OpenAiRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(repository.findBySessaoIdOrderByCriadoEmDescIdDesc(anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        chatService = new ChatServiceImpl(restClient, repository, "gpt-4o-mini", 20);
    }

    private OpenAiResponse respostaCom(String texto) {
        return new OpenAiResponse(List.of(new OpenAiChoice(new OpenAiMessage("assistant", texto))));
    }

    @Test
    void deveRetornarRespostaDaOpenAiComSucesso() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("Olá! Como posso ajudar?"));

        String resultado = chatService.enviarMensagem("sessao-123", "Oi, tudo bem?");

        assertThat(resultado).isEqualTo("Olá! Como posso ajudar?");
    }

    @Test
    void deveSalvarMensagemDoUsuarioEDaIa() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("Resposta"));

        chatService.enviarMensagem("sessao-123", "Pergunta");

        ArgumentCaptor<Mensagem> captor = ArgumentCaptor.forClass(Mensagem.class);
        verify(repository, times(2)).save(captor.capture());

        List<Mensagem> salvas = captor.getAllValues();
        assertThat(salvas.get(0).getRole()).isEqualTo(RoleEnum.user);
        assertThat(salvas.get(0).getConteudo()).isEqualTo("Pergunta");
        assertThat(salvas.get(1).getRole()).isEqualTo(RoleEnum.assistant);
        assertThat(salvas.get(1).getConteudo()).isEqualTo("Resposta");
        assertThat(salvas).allMatch(m -> m.getSessaoId().equals("sessao-123"));
    }

    @Test
    void deveLancarExcecaoQuandoRespostaForNula() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(null);

        assertThrows(OpenAiException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste"));
    }

    @Test
    void deveLancarExcecaoQuandoChoicesEstiverVazio() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(new OpenAiResponse(List.of()));

        assertThrows(OpenAiException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste"));
    }

    @Test
    void naoDeveSalvarRespostaQuandoOpenAiFalhar() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(null);

        assertThrows(OpenAiException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste"));

        // só a mensagem do usuário foi salva
        verify(repository, times(1)).save(any(Mensagem.class));
    }

    @Test
    void deveBuscarHistoricoApenasDaSessaoInformada() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("ok"));

        chatService.enviarMensagem("sessao-A", "Meu nome é Tampieri");
        chatService.enviarMensagem("sessao-B", "Qual meu nome?");

        verify(repository).findBySessaoIdOrderByCriadoEmDescIdDesc(eq("sessao-A"), any(Pageable.class));
        verify(repository).findBySessaoIdOrderByCriadoEmDescIdDesc(eq("sessao-B"), any(Pageable.class));
    }
}