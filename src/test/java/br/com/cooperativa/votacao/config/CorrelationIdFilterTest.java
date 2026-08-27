package br.com.cooperativa.votacao.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @Test
    @DisplayName("reaproveita o identificador do cliente quando o formato e valido")
    void reaproveitaIdentificadorValido() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123_XYZ");

        filtro.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123_XYZ");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // Divisão de resposta: injetaria um cabecalho novo.
                "abc\r\nSet-Cookie: sessao=roubada",
                // Injeção de log: forjaria uma linha inteira.
                "abc\nINFO Voto registrado indevidamente",
                "abc def",
                "abc;drop",
                "<script>",
                // Acima do limite de 64 caracteres.
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            })
    @DisplayName("descarta identificador fora do formato e gera um proprio")
    void descartaIdentificadorPerigoso(String valor) throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER, valor);

        filtro.doFilter(request, response, new MockFilterChain());

        var devolvido = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(devolvido).isNotEqualTo(valor).doesNotContain("\r", "\n", " ");
        // O substituto é um UUID, então a rastreabilidade não se perde.
        assertThat(devolvido).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("gera identificador quando o cliente nao envia nenhum")
    void geraQuandoAusente() throws Exception {
        var response = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("limpa o MDC ao final, para nao vazar entre requisicoes")
    void limpaMdc() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123");

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // Threads são reaproveitadas pelo pool do servidor: sem a limpeza, o
        // identificador apareceria no log da requisição seguinte.
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("mantem o identificador no MDC durante a cadeia de filtros")
    void disponivelDuranteACadeia() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "durante-a-cadeia");
        var capturado = new String[1];
        FilterChain cadeia = (req, res) -> capturado[0] = MDC.get(CorrelationIdFilter.MDC_KEY);

        filtro.doFilter(request, new MockHttpServletResponse(), cadeia);

        assertThat(capturado[0]).isEqualTo("durante-a-cadeia");
    }
}
