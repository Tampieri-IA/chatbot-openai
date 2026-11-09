package com.tampieria.chatbot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tampieria.chatbot.entity.Mensagem;
import com.tampieria.chatbot.entity.enums.RoleEnum;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class MensagemRepositoryTest {

    @Autowired
    private MensagemRepository repository;

    private Mensagem msg(String sessao, RoleEnum role, String texto, LocalDateTime criadoEm) {
        return Mensagem.builder()
                .sessaoId(sessao)
                .role(role)
                .conteudo(texto)
                .criadoEm(criadoEm)
                .build();
    }

    @Test
    void deveRetornarMensagensDaSessaoEmOrdemCronologica() {
        LocalDateTime agora = LocalDateTime.now();
        // salva fora de ordem de propósito
        repository.save(msg("s1", RoleEnum.ASSISTANT, "Oi!", agora));
        repository.save(msg("s1", RoleEnum.USER, "Olá", agora.minusSeconds(10)));

        List<Mensagem> resultado = repository.findBySessaoIdOrderByCriadoEmAsc("s1");

        assertThat(resultado).extracting(Mensagem::getConteudo)
                .containsExactly("Olá", "Oi!");
    }

    @Test
    void deveIsolarMensagensEntreSessoes() {
        repository.save(msg("A", RoleEnum.USER, "Segredo A", null));
        repository.save(msg("B", RoleEnum.USER, "Outra coisa", null));

        assertThat(repository.findBySessaoIdOrderByCriadoEmAsc("B"))
                .extracting(Mensagem::getConteudo)
                .containsExactly("Outra coisa");
    }

    @Test
    void devePreencherCriadoEmAutomaticamente() {
        Mensagem salva = repository.saveAndFlush(msg("s1", RoleEnum.USER, "Teste", null));

        assertThat(salva.getCriadoEm()).isNotNull();
    }

    @Test
    void deveRetornarApenasAsUltimasMensagensEmOrdemDecrescente() {
        LocalDateTime base = LocalDateTime.now().minusMinutes(1);
        for (int i = 1; i <= 25; i++) {
            repository.save(msg("s1", RoleEnum.USER, "m" + i, base.plusSeconds(i)));
        }

        List<Mensagem> ultimas = repository
                .findBySessaoIdOrderByCriadoEmDescIdDesc("s1", PageRequest.of(0, 20));

        assertThat(ultimas).hasSize(20);
        assertThat(ultimas.get(0).getConteudo()).isEqualTo("m25");
        assertThat(ultimas.get(19).getConteudo()).isEqualTo("m6");
    }

    @Test
    void deveApagarSomenteMensagensDaSessao() {
        repository.save(msg("A", RoleEnum.USER, "a", null));
        repository.save(msg("B", RoleEnum.USER, "b", null));

        repository.deleteBySessaoId("A");

        assertThat(repository.findBySessaoIdOrderByCriadoEmAsc("A")).isEmpty();
        assertThat(repository.findBySessaoIdOrderByCriadoEmAsc("B")).hasSize(1);
    }
}
