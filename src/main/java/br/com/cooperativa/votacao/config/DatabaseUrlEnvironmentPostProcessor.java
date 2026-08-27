package br.com.cooperativa.votacao.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String FONTE = "databaseUrlConvertida";

    static final String DATABASE_URL = "DATABASE_URL";

    static final String URL_EXPLICITA = "spring.datasource.url";

    /**
     * Converte {@code DATABASE_URL} em propriedades de datasource, se aplicável.
     *
     * @param environment ambiente em construção
     * @param application aplicação sendo inicializada
     */
    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        var databaseUrl = environment.getProperty(DATABASE_URL);
        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        // Quem definiu SPRING_DATASOURCE_URL a mão quis outro banco. Não sobrescrevo.
        if (StringUtils.hasText(environment.getProperty(URL_EXPLICITA))) {
            return;
        }

        var propriedades = converter(databaseUrl);
        if (!propriedades.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(FONTE, propriedades));
        }
    }

    /**
     * Traduz uma URI de conexão Postgres para propriedades do Spring.
     *
     * @param databaseUrl URI no formato {@code postgres[ql]://usuario:senha@host:porta/banco}
     * @return as propriedades de datasource, ou um mapa vazio se a URI não for reconhecida
     */
    static Map<String, Object> converter(String databaseUrl) {
        Map<String, Object> propriedades = new HashMap<>();

        // Uma URL que já esteja em formato JDBC não precisa de conversão: a
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

        // Porta ausente na URI significa a porta padrão do PostgreSQL.
        var porta = uri.getPort() > 0 ? uri.getPort() : 5432;
        // Com o prefixo postgres:// a URI é hierárquica, então getPath() nunca vem nulo.
        var banco = uri.getPath();

        // A query string carrega parametros que não podem ser perdidos: o
        // sslmode=require de bancos gerenciados é o mais importante deles, sem o
        // qual a conexão é recusada.
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
