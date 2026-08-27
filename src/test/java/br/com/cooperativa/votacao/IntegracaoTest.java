package br.com.cooperativa.votacao;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("integracao")
public abstract class IntegracaoTest {

    /** Instância única de PostgreSQL compartilhada por toda a suite. */
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("votacao")
                    .withUsername("votacao")
                    .withPassword("votacao")
                    .withReuse(true);

    static {
        // O Docker 29 recusa a versão de API que o docker-java negocia por padrão.
        // Fica aqui, e não no Failsafe, pra funcionar também rodando pela IDE.
        // Precisa vir antes do start(), que é quando o cliente é criado.
        System.setProperty("api.version", "1.44");
        POSTGRES.start();
    }

    /**
     * Aponta a aplicação para o container antes da criação do contexto.
     *
     * @param registry registro de propriedades dinâmicas do Spring Test
     */
    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
