package br.com.cooperativa.votacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.user-info")
public record UserInfoProperties(
        String baseUrl,
        boolean enabled,
        boolean fallbackPermiteVoto,
        int connectTimeoutMs,
        int readTimeoutMs) {}
