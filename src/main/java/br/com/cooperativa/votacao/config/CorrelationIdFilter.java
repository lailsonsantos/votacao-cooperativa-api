package br.com.cooperativa.votacao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Associa um identificador de correlacao a cada requisicao HTTP.
 *
 * <p>O identificador entra no {@link MDC}, portanto aparece em toda linha de log
 * emitida durante a requisicao, e volta ao cliente tanto no cabecalho
 * {@code X-Correlation-Id} quanto no corpo das respostas de erro. Isso fecha o
 * ciclo de diagnostico: a partir do erro devolvido ao usuario e possivel
 * recuperar o rastro completo da requisicao no log.
 *
 * <p>Se o cliente ja enviar um identificador, ele e preservado, permitindo
 * rastrear uma operacao que atravessa varios servicos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Cabecalho HTTP que transporta o identificador de correlacao. */
    public static final String HEADER = "X-Correlation-Id";

    /** Chave usada no contexto de diagnostico do SLF4J. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Injeta o identificador no MDC antes de seguir a cadeia de filtros.
     *
     * @param request     requisicao HTTP recebida
     * @param response    resposta HTTP em construcao
     * @param filterChain cadeia de filtros a prosseguir
     * @throws ServletException se a cadeia de filtros falhar
     * @throws IOException      se a escrita da resposta falhar
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var correlationId = request.getHeader(HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpar o MDC e obrigatorio: threads sao reaproveitadas pelo pool do
            // servidor e o identificador vazaria para a proxima requisicao.
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Recupera o identificador de correlacao da requisicao em andamento.
     *
     * @return o identificador atual, ou {@code null} fora do escopo de uma requisicao
     */
    public static String atual() {
        return MDC.get(MDC_KEY);
    }
}
