package br.com.cooperativa.votacao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Associa um identificador de correlacao a cada requisicao HTTP.
 *
 * <p>O identificador entra no {@link MDC}, portanto aparece em toda linha de log emitida durante a
 * requisicao, e volta ao cliente tanto no cabecalho {@code X-Correlation-Id} quanto no corpo das
 * respostas de erro. Isso fecha o ciclo de diagnostico: a partir do erro devolvido ao usuario e
 * possivel recuperar o rastro completo da requisicao no log.
 *
 * <p>Se o cliente ja enviar um identificador, ele e preservado, permitindo rastrear uma operacao
 * que atravessa varios servicos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    /** Cabecalho HTTP que transporta o identificador de correlacao. */
    public static final String HEADER = "X-Correlation-Id";

    /** Chave usada no contexto de diagnostico do SLF4J. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Formato aceito para um identificador vindo do cliente.
     *
     * <p>O valor recebido era devolvido no cabecalho da resposta e gravado no log sem nenhuma
     * verificacao, o que abre dois caminhos de ataque:
     *
     * <ul>
     *   <li><strong>Divisao de resposta HTTP:</strong> um valor contendo CR ou LF permite injetar
     *       cabecalhos adicionais na resposta;
     *   <li><strong>Injecao de log:</strong> uma quebra de linha no valor permite forjar linhas de
     *       log inteiras, contaminando justamente o rastro usado para investigar incidentes.
     * </ul>
     *
     * <p>O container costuma recusar CR e LF em cabecalho, mas depender disso e confiar a seguranca
     * a um detalhe de implementacao do servidor. Aceitar apenas o formato esperado resolve os dois
     * casos na origem.
     */
    private static final Pattern FORMATO_ACEITO = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    /**
     * Injeta o identificador no MDC antes de seguir a cadeia de filtros.
     *
     * @param request requisicao HTTP recebida
     * @param response resposta HTTP em construcao
     * @param filterChain cadeia de filtros a prosseguir
     * @throws ServletException se a cadeia de filtros falhar
     * @throws IOException se a escrita da resposta falhar
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var correlationId = request.getHeader(HEADER);
        if (!aceitavel(correlationId)) {
            // Valor ausente ou fora do formato: gera um proprio em vez de
            // recusar a requisicao. Rastreabilidade nao deve ser motivo para
            // negar um voto.
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
     * Verifica se o identificador recebido do cliente pode ser reaproveitado.
     *
     * @param valor conteudo do cabecalho, possivelmente nulo
     * @return {@code true} se o valor existir e obedecer ao formato aceito
     */
    private static boolean aceitavel(String valor) {
        return StringUtils.hasText(valor) && FORMATO_ACEITO.matcher(valor).matches();
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
