package br.com.cooperativa.votacao.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cooperativa.votacao.api.ui.dto.TipoItem;
import br.com.cooperativa.votacao.api.ui.dto.TipoTela;
import br.com.cooperativa.votacao.infrastructure.integration.userinfo.StatusAssociado;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Enums")
class EnumsTest {

    static Stream<Arguments> todosOsEnums() {
        return Stream.of(
                Arguments.of("OpcaoVoto", OpcaoVoto.values()),
                Arguments.of("StatusSessao", StatusSessao.values()),
                Arguments.of("ResultadoApuracao", ResultadoApuracao.values()),
                Arguments.of("TipoErro", TipoErro.values()),
                Arguments.of("StatusAssociado", StatusAssociado.values()),
                Arguments.of("TipoTela", TipoTela.values()),
                Arguments.of("TipoItem", TipoItem.values()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todosOsEnums")
    @DisplayName("todo valor tem id unico e descricao preenchida")
    void idUnicoEDescricaoPreenchida(String nome, Object[] valores) {
        var ids = Arrays.stream(valores).map(v -> chamar(v, "getId")).toList();
        var descricoes =
                Arrays.stream(valores).map(v -> (String) chamar(v, "getDescricao")).toList();

        assertThat(ids).doesNotHaveDuplicates().doesNotContainNull();
        assertThat(descricoes).allSatisfy(d -> assertThat(d).isNotBlank());
    }

    @Test
    @DisplayName("porId devolve o valor correspondente")
    void porId() {
        assertThat(OpcaoVoto.porId(1)).isEqualTo(OpcaoVoto.SIM);
        assertThat(OpcaoVoto.porId(2)).isEqualTo(OpcaoVoto.NAO);
        assertThat(StatusSessao.porId(1)).isEqualTo(StatusSessao.ABERTA);
        assertThat(ResultadoApuracao.porId(3)).isEqualTo(ResultadoApuracao.EMPATE);
        assertThat(TipoErro.porId(2)).isEqualTo(TipoErro.NAO_ENCONTRADO);
        assertThat(StatusAssociado.porId(1)).isEqualTo(StatusAssociado.ABLE_TO_VOTE);
        assertThat(TipoTela.porId(2)).isEqualTo(TipoTela.SELECAO);
        assertThat(TipoItem.porId(4)).isEqualTo(TipoItem.INPUT_DATA);
    }

    @Test
    @DisplayName("porId com id inexistente falha com mensagem clara")
    void porIdInvalido() {
        assertThatThrownBy(() -> OpcaoVoto.porId(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
        assertThatThrownBy(() -> StatusSessao.porId(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResultadoApuracao.porId(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TipoErro.porId(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StatusAssociado.porId(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TipoTela.porId(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TipoItem.porId(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a descricao do resultado e a que aparece na tela")
    void descricaoDoResultado() {
        assertThat(ResultadoApuracao.APROVADA.getDescricao()).isEqualTo("APROVADA");
        assertThat(ResultadoApuracao.SEM_VOTOS.getDescricao()).isEqualTo("Nenhum voto registrado");
    }

    private static Object chamar(Object alvo, String metodo) {
        try {
            return alvo.getClass().getMethod(metodo).invoke(alvo);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
