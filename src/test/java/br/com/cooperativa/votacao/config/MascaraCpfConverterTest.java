package br.com.cooperativa.votacao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("MascaraCpfConverter")
class MascaraCpfConverterTest {

    private final MascaraCpfConverter converter = new MascaraCpfConverter();

    /**
     * Constroi um evento de log com a mensagem informada.
     *
     * @param mensagem texto já formatado
     * @return o evento pronto para conversão
     */
    private ILoggingEvent evento(String mensagem) {
        var e = mock(ILoggingEvent.class);
        when(e.getFormattedMessage()).thenReturn(mensagem);
        return e;
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "'Voto de 19839091069 registrado',        'Voto de 198******69 registrado'",
        "'CPF 198.390.910-69 recusado',           'CPF 198******69 recusado'",
        "'valor (19839091069) duplicado',         'valor (198******69) duplicado'"
    })
    @DisplayName("mascara CPF com e sem pontuação")
    void mascara(String entrada, String esperado) {
        assertThat(converter.convert(evento(entrada))).isEqualTo(esperado);
    }

    @Test
    @DisplayName("mascara todas as ocorrencias da mesma mensagem")
    void mascaraMultiplas() {
        var texto = "de 19839091069 para 62289608068";
        assertThat(converter.convert(evento(texto))).isEqualTo("de 198******69 para 622******68");
    }

    @Test
    @DisplayName("não mascara os primeiros digitos de um número maior")
    void naoMascaraNumeroMaior() {
        // Sem os lookarounds, os 11 primeiros dígitos de um identificador de 15
        // seriam substituidos, corrompendo o log em vez de proteger dado pessoal.
        var texto = "identificador 123456789012345 processado";
        assertThat(converter.convert(evento(texto))).isEqualTo(texto);
    }

    @Test
    @DisplayName("preserva mensagens sem CPF")
    void semCpf() {
        var texto = "Sessão aberta. pautaId=3a7b duracaoMinutos=5";
        assertThat(converter.convert(evento(texto))).isEqualTo(texto);
    }

    @Test
    @DisplayName("não quebra com mensagem vazia ou nula")
    void mensagemVaziaOuNula() {
        assertThat(converter.convert(evento(""))).isEmpty();
        assertThat(converter.convert(evento(null))).isNull();
    }
}
