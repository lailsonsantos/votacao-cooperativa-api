package br.com.cooperativa.votacao;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes de integracao, executados sobre PostgreSQL real.
 *
 * <p>O container e {@code static}: uma unica instancia atende a todas as classes que herdam desta,
 * em vez de subir um banco por classe. Isso mantem a suite rapida sem abrir mao de testar contra o
 * banco de producao de verdade.
 *
 * <p>Testar apuracao e unicidade em H2 daria falsa seguranca: sao exatamente os pontos em que a
 * semantica de constraint e de tipos diverge entre bancos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("integracao")
public abstract class IntegracaoTest {

    /** Instancia unica de PostgreSQL compartilhada por toda a suite. */
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("votacao")
                    .withUsername("votacao")
                    .withPassword("votacao")
                    .withReuse(true);

    static {
        // O Docker Engine 29 elevou a versao minima da API aceita e recusa com
        // HTTP 400 a versao 1.32 que o docker-java negocia por padrao. Definir a
        // propriedade aqui, e nao apenas na configuracao do Failsafe, faz a suite
        // funcionar tambem quando um teste e executado direto pela IDE — que e
        // como se depura um teste que falhou.
        //
        // O docker-java le esta propriedade na primeira criacao de cliente, que
        // acontece em start(); por isso a atribuicao precede a chamada.
        System.setProperty("api.version", "1.44");
        POSTGRES.start();
    }

    /**
     * Aponta a aplicacao para o container antes da criacao do contexto.
     *
     * @param registry registro de propriedades dinamicas do Spring Test
     */
    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
