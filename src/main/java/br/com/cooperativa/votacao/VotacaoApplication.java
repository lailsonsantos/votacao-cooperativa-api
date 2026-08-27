package br.com.cooperativa.votacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Ponto de entrada da API de votacao em assembleias cooperativas.
 *
 * <p>A aplicacao expoe duas superficies HTTP sobre o mesmo nucleo de dominio:
 *
 * <ul>
 *   <li>{@code /api/v1/**} &mdash; API REST orientada a recursos, consumida por
 *       integracoes e pelo painel administrativo web;
 *   <li>{@code /api/v1/telas/**} &mdash; camada <em>Server-Driven UI</em> que
 *       devolve descricoes de tela no formato do Anexo 1 do enunciado, para que o
 *       cliente as renderize sem conhecer o dominio.
 * </ul>
 *
 * <p>Nenhuma regra de negocio vive nos controladores: ambas as superficies
 * delegam para os mesmos servicos de aplicacao.
 *
 * @author Lailson Santos
 */
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
