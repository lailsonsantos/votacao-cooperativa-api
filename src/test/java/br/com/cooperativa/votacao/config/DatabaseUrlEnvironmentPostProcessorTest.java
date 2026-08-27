package br.com.cooperativa.votacao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

/**
 * Testes da conversao de {@code DATABASE_URL} para propriedades do Spring.
 *
 * <p>Um defeito aqui so apareceria no deploy, com a aplicacao falhando ao subir e sem banco para
 * consultar &mdash; o pior momento possivel para descobrir. Daí a cobertura detalhada de um trecho
 * de codigo tao curto.
 */
@DisplayName("DatabaseUrlEnvironmentPostProcessor")
class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor =
            new DatabaseUrlEnvironmentPostProcessor();

    @ParameterizedTest
    @ValueSource(strings = {"postgres", "postgresql"})
    @DisplayName("converte a URI da plataforma para o formato JDBC")
    void converteUri(String esquema) {
        var propriedades =
                DatabaseUrlEnvironmentPostProcessor.converter(
                        esquema + "://joao:segredo@db.render.com:5432/votacao");

        assertThat(propriedades)
                .containsEntry(
                        "spring.datasource.url", "jdbc:postgresql://db.render.com:5432/votacao")
                .containsEntry("spring.datasource.username", "joao")
                .containsEntry("spring.datasource.password", "segredo");
    }

    @Test
    @DisplayName("preserva a query string, sem a qual bancos gerenciados recusam a conexao")
    void preservaQueryString() {
        var propriedades =
                DatabaseUrlEnvironmentPostProcessor.converter(
                        "postgresql://u:p@ep-neon.aws.neon.tech/votacao?sslmode=require");

        // Perder o sslmode faria o banco recusar a conexao com um erro que nao
        // aponta para a causa.
        assertThat(propriedades.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-neon.aws.neon.tech:5432/votacao?sslmode=require");
    }

    @Test
    @DisplayName("aplica a porta padrao do PostgreSQL quando a URI a omite")
    void portaPadrao() {
        var propriedades =
                DatabaseUrlEnvironmentPostProcessor.converter("postgres://u:p@host/banco");

        assertThat(propriedades.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/banco");
    }

    @Test
    @DisplayName("aceita senha contendo dois-pontos")
    void senhaComDoisPontos() {
        var propriedades =
                DatabaseUrlEnvironmentPostProcessor.converter("postgres://u:se:nha@host:5432/b");

        // O split limitado a duas partes evita truncar a senha no primeiro ':'.
        assertThat(propriedades)
                .containsEntry("spring.datasource.username", "u")
                .containsEntry("spring.datasource.password", "se:nha");
    }

    @Test
    @DisplayName("repassa sem alteracao uma URL que ja esta em formato JDBC")
    void urlJaEmJdbc() {
        var propriedades =
                DatabaseUrlEnvironmentPostProcessor.converter("jdbc:postgresql://host:5432/b");

        assertThat(propriedades)
                .containsExactly(entry("spring.datasource.url", "jdbc:postgresql://host:5432/b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mysql://u:p@host/b", "nao-e-uma-uri", "http://exemplo.com"})
    @DisplayName("ignora valores que nao sao conexoes PostgreSQL")
    void ignoraValoresIrreconheciveis(String valor) {
        assertThat(DatabaseUrlEnvironmentPostProcessor.converter(valor)).isEmpty();
    }

    @Test
    @DisplayName("ignora URI sintaticamente quebrada em vez de estourar")
    void uriQuebrada() {
        // Um valor mal formado na variavel de ambiente nao pode impedir a
        // aplicacao de subir com a configuracao explicita.
        assertThat(DatabaseUrlEnvironmentPostProcessor.converter("postgres://[quebrado")).isEmpty();
        assertThat(DatabaseUrlEnvironmentPostProcessor.converter("postgres://")).isEmpty();
    }

    @Test
    @DisplayName("ignora URI sem host")
    void semHost() {
        // postgres:///banco e sintaticamente valido mas nao aponta para lugar
        // nenhum; converter isso produziria uma URL JDBC invalida.
        assertThat(DatabaseUrlEnvironmentPostProcessor.converter("postgres:///banco")).isEmpty();
    }

    @Test
    @DisplayName("aceita URI sem credenciais")
    void semCredenciais() {
        var propriedades =
                DatabaseUrlEnvironmentPostProcessor.converter("postgres://host:5432/banco");

        assertThat(propriedades).containsKey("spring.datasource.url");
        assertThat(propriedades).doesNotContainKey("spring.datasource.username");
    }

    @Test
    @DisplayName("aceita usuario sem senha")
    void usuarioSemSenha() {
        var propriedades =
                DatabaseUrlEnvironmentPostProcessor.converter("postgres://usuario@host/banco");

        assertThat(propriedades)
                .containsEntry("spring.datasource.username", "usuario")
                .doesNotContainKey("spring.datasource.password");
    }

    @Test
    @DisplayName("acrescenta as propriedades ao ambiente quando DATABASE_URL existe")
    void acrescentaAoAmbiente() {
        var environment = new MockEnvironment();
        environment.setProperty(
                DatabaseUrlEnvironmentPostProcessor.DATABASE_URL,
                "postgres://u:p@host:5432/votacao");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(
                        environment
                                .getPropertySources()
                                .contains(DatabaseUrlEnvironmentPostProcessor.FONTE))
                .isTrue();
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://host:5432/votacao");
    }

    @Test
    @DisplayName("nao faz nada quando DATABASE_URL nao esta definida")
    void semDatabaseUrl() {
        var environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(
                        environment
                                .getPropertySources()
                                .contains(DatabaseUrlEnvironmentPostProcessor.FONTE))
                .isFalse();
    }

    @Test
    @DisplayName("nao altera o ambiente quando DATABASE_URL existe mas e irreconhecivel")
    void databaseUrlIrreconhecivel() {
        var environment = new MockEnvironment();
        environment.setProperty(
                DatabaseUrlEnvironmentPostProcessor.DATABASE_URL, "mysql://u:p@host/banco");

        processor.postProcessEnvironment(environment, new SpringApplication());

        // Um banco de outro fornecedor na variavel nao pode ser convertido em URL
        // JDBC de PostgreSQL; melhor nao mexer e deixar a configuracao explicita
        // decidir do que produzir uma URL silenciosamente errada.
        assertThat(
                        environment
                                .getPropertySources()
                                .contains(DatabaseUrlEnvironmentPostProcessor.FONTE))
                .isFalse();
    }

    @Test
    @DisplayName("respeita SPRING_DATASOURCE_URL definida explicitamente")
    void respeitaConfiguracaoExplicita() {
        var environment = new MockEnvironment();
        environment.setProperty(
                DatabaseUrlEnvironmentPostProcessor.DATABASE_URL, "postgres://u:p@host/plataforma");
        environment.setProperty(
                DatabaseUrlEnvironmentPostProcessor.URL_EXPLICITA, "jdbc:postgresql://outro/banco");

        processor.postProcessEnvironment(environment, new SpringApplication());

        // Quem definiu a URL a mao quis apontar para outro banco; sobrescrever
        // isso seria surpreendente e dificil de diagnosticar.
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://outro/banco");
    }
}
