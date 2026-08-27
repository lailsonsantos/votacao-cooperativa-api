package br.com.cooperativa.votacao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    /**
     * Descreve a API para o springdoc.
     *
     * @return o documento OpenAPI base, completado em tempo de execução pelos controladores
     */
    @Bean
    public OpenAPI votacaoOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("API de Votação Cooperativa")
                                .version("v1")
                                .description(
                                        """
                                        Gerenciamento de pautas, sessões de votação e apuração de \
                                        resultados em assembleias cooperativas.

                                        A API expõe duas superfícies:

                                        - `/api/v1/**` — REST orientada a recursos.
                                        - `/api/v1/telas/**` — Server-Driven UI no formato do \
                                        Anexo 1, consumida pelo cliente.

                                        Versionamento por URI. Mudanças compatíveis não sobem a \
                                        versão; mudanças incompatíveis criam `/api/v2` e a versão \
                                        anterior é depreciada por cabeçalhos `Deprecation` e \
                                        `Sunset`, com janela mínima de 6 meses.
                                        """)
                                .contact(new Contact().name("Lailson Santos"))
                                .license(new License().name("MIT")));
    }
}
