package br.com.cooperativa.votacao.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cooperativa.votacao.domain.model.ResultadoApuracao;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import br.com.cooperativa.votacao.domain.model.StatusSessao;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Testes da apuracao do desfecho de uma pauta. */
@DisplayName("ResultadoVotacao")
class ResultadoVotacaoTest {

    private static final UUID PAUTA = UUID.randomUUID();

    @ParameterizedTest(name = "sim={0} nao={1} resulta em {2}")
    @CsvSource({
        "10, 3,  APROVADA",
        "3,  10, REPROVADA",
        "7,  7,  EMPATE",
        "0,  0,  SEM_VOTOS",
        "1,  0,  APROVADA",
        "0,  1,  REPROVADA"
    })
    @DisplayName("classifica o desfecho a partir da contagem")
    void classificaDesfecho(long sim, long nao, ResultadoApuracao esperado) {
        var resultado = ResultadoVotacao.de(PAUTA, "Pauta", StatusSessao.FECHADA, sim, nao);

        assertThat(resultado.resultado()).isEqualTo(esperado);
        assertThat(resultado.totalVotos()).isEqualTo(sim + nao);
    }

    @Test
    @DisplayName("marca como parcial enquanto a sessao esta aberta")
    void marcaParcial() {
        assertThat(ResultadoVotacao.de(PAUTA, "Pauta", StatusSessao.ABERTA, 5, 2).parcial())
                .isTrue();
        assertThat(ResultadoVotacao.de(PAUTA, "Pauta", StatusSessao.FECHADA, 5, 2).parcial())
                .isFalse();
    }

    @Test
    @DisplayName("distingue empate de ausencia de votos")
    void distingueEmpateDeSemVotos() {
        // Tratar "0 a 0" como empate esconderia do consumidor que ninguem votou.
        assertThat(ResultadoVotacao.de(PAUTA, "P", StatusSessao.FECHADA, 0, 0).resultado())
                .isEqualTo(ResultadoApuracao.SEM_VOTOS);
        assertThat(ResultadoVotacao.de(PAUTA, "P", StatusSessao.FECHADA, 1, 1).resultado())
                .isEqualTo(ResultadoApuracao.EMPATE);
    }
}
