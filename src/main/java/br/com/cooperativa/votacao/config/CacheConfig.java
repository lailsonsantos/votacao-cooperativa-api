package br.com.cooperativa.votacao.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    /** Nome do cache que guarda o resultado de sessoes encerradas. */
    public static final String CACHE_RESULTADO = "resultadoVotacao";

    /**
     * Gerenciador de cache baseado em Caffeine.
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
