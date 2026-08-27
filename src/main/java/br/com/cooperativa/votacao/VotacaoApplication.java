package br.com.cooperativa.votacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
public class VotacaoApplication {
    /**
     * Sobe o contexto Spring.
     *
     * @param args argumentos de linha de comando repassados ao Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(VotacaoApplication.class, args);
    }
}
