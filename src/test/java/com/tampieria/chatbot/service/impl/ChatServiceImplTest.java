package com.tampieria.chatbot.service.impl;

import com.tampieria.chatbot.dto.openai.OpenAiChoice;
import com.tampieria.chatbot.dto.openai.OpenAiMessage;
import com.tampieria.chatbot.dto.openai.OpenAiRequest;
import com.tampieria.chatbot.dto.openai.OpenAiResponse;
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
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // setup compartilhado stuba a cadeia do RestClient, que nem todo teste usa.
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

    private OpenAiRequest requestEnviado;

    @BeforeEach
    void setup() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(OpenAiRequest.class))).thenAnswer(invocation -> {
            requestEnviado = invocation.getArgument(0);
            return requestBodySpec;
        });
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(repository.findBySessaoIdOrderByCriadoEmDescIdDesc(anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        chatService = new ChatServiceImpl(restClient, repository, "gpt-4o-mini", 20, 8000);
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
        assertThat(salvas.get(0).getRole()).isEqualTo(RoleEnum.USER);
        assertThat(salvas.get(0).getConteudo()).isEqualTo("Pergunta");
        assertThat(salvas.get(1).getRole()).isEqualTo(RoleEnum.ASSISTANT);
        assertThat(salvas.get(1).getConteudo()).isEqualTo("Resposta");
        assertThat(salvas).allMatch(m -> m.getSessaoId().equals("sessao-123"));
    }

    @Test
    void deveLancarExcecaoQuandoChoicesEstiverVazio() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(new OpenAiResponse(List.of()));

        assertThrows(OpenAiException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste"));
    }

    @Test
    void deveLancarExcecaoESalvarApenasMensagemDoUsuarioQuandoRespostaForNula() {
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

    @Test
    void deveMontarPayloadComSystemEHistoricoEmOrdemCronologica() {
        Mensagem m2 = Mensagem.builder().sessaoId("s").role(RoleEnum.ASSISTANT).conteudo("resposta antiga").build();
        Mensagem m1 = Mensagem.builder().sessaoId("s").role(RoleEnum.USER).conteudo("pergunta antiga").build();
        // o repository devolve em ordem DECRESCENTE (mais recente primeiro)
        when(repository.findBySessaoIdOrderByCriadoEmDescIdDesc(anyString(), any(Pageable.class)))
                .thenReturn(List.of(m2, m1));
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("ok"));

        chatService.enviarMensagem("s", "nova");

        List<OpenAiMessage> msgs = requestEnviado.messages();
        assertThat(msgs).extracting(OpenAiMessage::role)
                .containsExactly("system", "user", "assistant");
        assertThat(msgs.get(1).content()).isEqualTo("pergunta antiga");
    }

    @Test
    void deveLancarExcecaoQuandoConteudoForVazio() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("  "));

        assertThrows(OpenAiException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste"));
    }

    @Test
    void deveConverterRestClientExceptionEmOpenAiException() {
        when(responseSpec.body(OpenAiResponse.class))
                .thenThrow(new RestClientException("timeout"));

        assertThrows(OpenAiException.class,
                () -> chatService.enviarMensagem("sessao-123", "teste"));
    }

    @Test
    void deveUsarLimiteDeHistoricoConfigurado() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("ok"));

        chatService.enviarMensagem("s", "oi");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findBySessaoIdOrderByCriadoEmDescIdDesc(eq("s"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void deveUsarLimiteDeHistoricoConfiguradoLimite5() {
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("ok"));

        ChatServiceImpl servicoComLimite5 = new ChatServiceImpl(restClient, repository, "gpt-4o-mini", 5, 8000);
        servicoComLimite5.enviarMensagem("s", "oi");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findBySessaoIdOrderByCriadoEmDescIdDesc(eq("s"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void deveLimitarPayloadPorCaracteres() {
        ChatServiceImpl servicoComLimitePequeno =
                new ChatServiceImpl(restClient, repository, "gpt-4o-mini", 20, 30);

        Mensagem antiga = Mensagem.builder().sessaoId("s").role(RoleEnum.USER)
                .conteudo("a".repeat(40)).build();
        Mensagem recente = Mensagem.builder().sessaoId("s").role(RoleEnum.USER)
                .conteudo("pergunta curta").build();
        // repository devolve em ordem decrescente
        when(repository.findBySessaoIdOrderByCriadoEmDescIdDesc(anyString(), any(Pageable.class)))
                .thenReturn(List.of(recente, antiga));
        when(responseSpec.body(OpenAiResponse.class)).thenReturn(respostaCom("ok"));

        servicoComLimitePequeno.enviarMensagem("s", "pergunta curta");

        assertThat(requestEnviado.messages())
                .extracting(OpenAiMessage::content)
                .doesNotContain("a".repeat(40));
    }
}