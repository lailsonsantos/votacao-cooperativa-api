package br.com.cooperativa.votacao.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura o cache em memoria usado na apuracao de resultados.
 *
 * <p>Apenas o resultado de sessoes <strong>ja encerradas</strong> e cacheado: uma vez fechada, a
 * contagem de uma sessao e imutavel, entao nao existe risco de servir dado desatualizado.
 * Resultados de sessoes abertas nunca entram no cache &mdash; ver {@code ResultadoService}.
 */
@Configuration
public class CacheConfig {
    /** Nome do cache que guarda o resultado de sessoes encerradas. */
    public static final String CACHE_RESULTADO = "resultadoVotacao";

    /**
     * Gerenciador de cache baseado em Caffeine.
     *
     * <p>O limite de entradas evita crescimento ilimitado de memoria em uma cooperativa com muitas
     * pautas; o TTL existe apenas como rede de seguranca, ja que o dado cacheado e imutavel por
     * natureza.
     *
     * @return o gerenciador de cache da aplicacao
     */
    @Bean
    public CacheManager cacheManager() {
        var cacheManager = new CaffeineCacheManager(CACHE_RESULTADO);
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofHours(6))
                        .recordStats());
        return cacheManager;
    }
}
