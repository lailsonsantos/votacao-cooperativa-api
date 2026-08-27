package br.com.cooperativa.votacao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracao de CORS para o cliente web.
 *
 * <p>O frontend roda em outra origem (outro host e outra porta), entao o navegador exige CORS. As
 * origens permitidas vem de configuracao em vez de ficarem fixas no codigo, pelo mesmo motivo das
 * URLs de callback: o mesmo artefato precisa servir ambiente local, rede interna e nuvem.
 *
 * <p>O enunciado permite abstrair a seguranca das interfaces, entao nao ha autenticacao a proteger
 * &mdash; ainda assim, restringir origens e o padrao correto e custa uma linha de configuracao.
 *
 * <p>O construtor e explicito, e nao gerado pelo Lombok, para que o array de origens seja copiado
 * na entrada. Arrays sao mutaveis: sem a copia, quem forneceu o valor continuaria podendo altera-lo
 * depois, mudando a politica de CORS em tempo de execucao.
 */
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
