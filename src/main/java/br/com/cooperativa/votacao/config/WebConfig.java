package br.com.cooperativa.votacao.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {

    static final String PADRAO_ROTAS = "/api/**";

    private final String[] origensPermitidas;

    public WebConfig(@Value("${app.cors.allowed-origins}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas.clone();
    }

    /**
     * Publica o CORS como filtro, e não pelo {@code addCorsMappings} do MVC.
     *
     * <p>A configuração do MVC só é aplicada depois que o <em>handler mapping</em> resolve um
     * controlador. Um {@code GET} em rota que só aceita {@code POST} falha na resolução, e a
     * resposta montada pelo tratador de exceções sai <strong>sem</strong> {@code
     * Access-Control-Allow-Origin} — o navegador então bloqueia uma resposta que o servidor
     * considerou bem-sucedida, e o cliente vê erro de rede em vez da tela de erro.
     *
     * <p>Como filtro, o CORS roda antes do {@code DispatcherServlet} e vale para toda resposta,
     * inclusive as de erro de roteamento.
     *
     * @return o filtro registrado antes de qualquer outro
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        var configuracao = new CorsConfiguration();
        configuracao.setAllowedOriginPatterns(Arrays.asList(origensPermitidas));
        configuracao.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setExposedHeaders(List.of(CorrelationIdFilter.HEADER));
        configuracao.setMaxAge(3600L);

        var fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration(PADRAO_ROTAS, configuracao);

        var registro = new FilterRegistrationBean<>(new CorsFilter(fonte));
        // Antes do CorrelationIdFilter: uma origem recusada não deve sequer abrir
        // um contexto de log.
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }
}
