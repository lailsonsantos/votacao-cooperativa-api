package br.com.cooperativa.votacao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cooperativa.votacao.domain.enums.StatusSessao;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SessaoVotacao")
class SessaoVotacaoTest {

    private static final Instant ABERTURA = Instant.parse("2026-08-27T14:00:00Z");

    private final Pauta pauta = Pauta.criar("Reforma do estatuto", null, ABERTURA);

    @Test
    @DisplayName("permanece aberta durante a janela de votação")
    void abertaDuranteAJanela() {
        var sessao = SessaoVotacao.abrir(pauta, ABERTURA, Duration.ofMinutes(5));

        assertThat(sessao.estaAberta(ABERTURA)).isTrue();
        assertThat(sessao.estaAberta(ABERTURA.plusSeconds(299))).isTrue();
        assertThat(sessao.status(ABERTURA.plusSeconds(299))).isEqualTo(StatusSessao.ABERTA);
    }

    @Test
    @DisplayName("fecha exatamente no instante de fechamento, não depois")
    void fechaNoInstanteDeFechamento() {
        var sessao = SessaoVotacao.abrir(pauta, ABERTURA, Duration.ofMinutes(5));

        // Limite exclusivo: no instante do fechamento já não aceita voto. Sem esse
        // caso, trocar isBefore por isAfter passaria batido.
        assertThat(sessao.estaAberta(ABERTURA.plusSeconds(300))).isFalse();
        assertThat(sessao.status(ABERTURA.plusSeconds(300))).isEqualTo(StatusSessao.FECHADA);
    }

    @Test
    @DisplayName("calcula o tempo restante sem devolver valor negativo")
    void tempoRestanteNuncaNegativo() {
        var sessao = SessaoVotacao.abrir(pauta, ABERTURA, Duration.ofMinutes(1));

        assertThat(sessao.tempoRestante(ABERTURA)).isEqualTo(Duration.ofMinutes(1));
        assertThat(sessao.tempoRestante(ABERTURA.plusSeconds(30)))
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(sessao.tempoRestante(ABERTURA.plusSeconds(600))).isZero();
    }

    @Test
    @DisplayName("aplica a duração informada ao instante de fechamento")
    void aplicaDuracao() {
        var sessao = SessaoVotacao.abrir(pauta, ABERTURA, Duration.ofMinutes(3));

        assertThat(sessao.getAberturaEm()).isEqualTo(ABERTURA);
        assertThat(sessao.getFechamentoEm()).isEqualTo(ABERTURA.plus(Duration.ofMinutes(3)));
    }
}
