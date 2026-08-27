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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    public static final String MDC_KEY = "correlationId";

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
            // Fora do formato: gera um novo em vez de recusar. Nao vale negar
            // um voto por causa de rastreabilidade.
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Precisa limpar: a thread volta pro pool e o id vazaria pra proxima
            // requisicao.
            MDC.remove(MDC_KEY);
        }
    }

    private static boolean aceitavel(String valor) {
        return StringUtils.hasText(valor) && FORMATO_ACEITO.matcher(valor).matches();
    }

    public static String atual() {
        return MDC.get(MDC_KEY);
    }
}
