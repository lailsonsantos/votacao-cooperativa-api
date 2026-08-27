package br.com.cooperativa.votacao.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Converte a variavel {@code DATABASE_URL} para o formato que o Spring espera.
 *
 * <p>Praticamente todas as plataformas de nuvem gerenciada &mdash; Render, Railway, Fly.io, Neon,
 * Supabase &mdash; injetam a conexao do banco como uma URI no formato:
 *
 * <pre>postgresql://usuario:senha@host:5432/banco?sslmode=require</pre>
 *
 * <p>O Spring, por sua vez, exige a URL no formato JDBC e as credenciais em propriedades separadas:
 *
 * <pre>
 * spring.datasource.url      = jdbc:postgresql://host:5432/banco?sslmode=require
 * spring.datasource.username = usuario
 * spring.datasource.password = senha
 * </pre>
 *
 * <p>Esta classe faz a ponte. A alternativa seria montar a URL JDBC na configuracao da plataforma,
 * mas nem todas expoem host e porta como variaveis separadas &mdash; o blueprint do Render, por
 * exemplo, so oferece a URI completa. Resolver aqui torna a mesma imagem implantavel em qualquer
 * uma dessas plataformas sem alteracao.
 *
 * <p>A conversao roda como {@link EnvironmentPostProcessor} porque precisa acontecer
 * <strong>antes</strong> da criacao do {@code DataSource}, ou seja, antes de qualquer bean existir.
 *
 * <p><strong>Precedencia:</strong> se {@code SPRING_DATASOURCE_URL} estiver definida
 * explicitamente, ela vence e a conversao nao acontece. Isso preserva a capacidade de apontar para
 * outro banco sem mexer na variavel da plataforma.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {
    /** Nome da fonte de propriedades acrescentada ao ambiente. */
    static final String FONTE = "databaseUrlConvertida";

    /** Variavel injetada pelas plataformas de nuvem. */
    static final String DATABASE_URL = "DATABASE_URL";

    /** Propriedade que, se ja definida, tem precedencia sobre a conversao. */
    static final String URL_EXPLICITA = "spring.datasource.url";

    /**
     * Converte {@code DATABASE_URL} em propriedades de datasource, se aplicavel.
     *
     * @param environment ambiente em construcao
     * @param application aplicacao sendo inicializada
     */
    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        var databaseUrl = environment.getProperty(DATABASE_URL);
        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        // Configuracao explicita vence: quem definiu SPRING_DATASOURCE_URL a mao
        // quis apontar para outro banco, e sobrescrever isso seria surpreendente.
        if (StringUtils.hasText(environment.getProperty(URL_EXPLICITA))) {
            return;
        }

        var propriedades = converter(databaseUrl);
        if (!propriedades.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(FONTE, propriedades));
        }
    }

    /**
     * Traduz uma URI de conexao Postgres para propriedades do Spring.
     *
     * @param databaseUrl URI no formato {@code postgres[ql]://usuario:senha@host:porta/banco}
     * @return as propriedades de datasource, ou um mapa vazio se a URI nao for reconhecida
     */
    static Map<String, Object> converter(String databaseUrl) {
        Map<String, Object> propriedades = new HashMap<>();

        // Uma URL que ja esteja em formato JDBC nao precisa de conversao: a
        // plataforma pode te-la fornecido pronta.
        if (databaseUrl.startsWith("jdbc:")) {
            propriedades.put(URL_EXPLICITA, databaseUrl);
            return propriedades;
        }

        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return propriedades;
        }

        final URI uri;
        try {
            uri = URI.create(databaseUrl);
        } catch (IllegalArgumentException e) {
            return propriedades;
        }

        var host = uri.getHost();
        if (host == null) {
            return propriedades;
        }

        // Porta ausente na URI significa a porta padrao do PostgreSQL.
        var porta = uri.getPort() > 0 ? uri.getPort() : 5432;
        var banco = uri.getPath() == null ? "" : uri.getPath();

        // A query string carrega parametros que nao podem ser perdidos: o
        // sslmode=require de bancos gerenciados e o mais importante deles, sem o
        // qual a conexao e recusada.
        var query = StringUtils.hasText(uri.getQuery()) ? "?" + uri.getQuery() : "";

        propriedades.put(
                URL_EXPLICITA, "jdbc:postgresql://%s:%d%s%s".formatted(host, porta, banco, query));

        var credenciais = uri.getUserInfo();
        if (StringUtils.hasText(credenciais)) {
            var partes = credenciais.split(":", 2);
            propriedades.put("spring.datasource.username", partes[0]);
            if (partes.length > 1) {
                propriedades.put("spring.datasource.password", partes[1]);
            }
        }

        return propriedades;
    }
}
