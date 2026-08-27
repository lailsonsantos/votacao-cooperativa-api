package br.com.cooperativa.votacao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracao de CORS para o cliente web.
 *
 * <p>O frontend React roda em outra origem (outro host e outra porta), entao o
 * navegador exige CORS. As origens permitidas vem de configuracao em vez de
 * ficarem fixas no codigo, pelo mesmo motivo das URLs de callback: o mesmo
 * artefato precisa servir ambiente local, homologacao e nuvem.
 *
 * <p>O enunciado permite abstrair a seguranca das interfaces, entao nao ha
 * autenticacao a proteger &mdash; ainda assim, restringir origens e o padrao
 * correto e custa uma linha de configuracao.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Origens autorizadas a consumir a API a partir do navegador. */
    private final String[] origensPermitidas;

    /**
     * Cria a configuracao a partir das origens declaradas em {@code app.cors.allowed-origins}.
     *
     * @param origensPermitidas lista de origens separada por virgula
     */
    public WebConfig(@Value("${app.cors.allowed-origins}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    /**
     * Libera os metodos usados pela API para as origens configuradas.
     *
     * @param registry registro de configuracao de CORS do Spring MVC
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origensPermitidas)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(CorrelationIdFilter.HEADER)
                .maxAge(3600);
    }
}
