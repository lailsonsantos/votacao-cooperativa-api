package br.com.cooperativa.votacao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    /** Origens autorizadas a consumir a API a partir do navegador. */
    private final String[] origensPermitidas;

    /**
     * Cria a configuracao a partir das origens declaradas.
     *
     * @param origensPermitidas lista de origens separada por virgula
     */
    public WebConfig(@Value("${app.cors.allowed-origins}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas.clone();
    }

    /**
     * Libera os metodos usados pela API para as origens configuradas.
     *
     * @param registry registro de configuracao de CORS do Spring MVC
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origensPermitidas.clone())
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(CorrelationIdFilter.HEADER)
                .maxAge(3600);
    }
}
