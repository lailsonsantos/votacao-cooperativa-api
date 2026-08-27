package br.com.cooperativa.votacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do servico externo de verificacao de CPF (Tarefa Bonus 1).
 *
 * <p>O endpoint indicado no enunciado ({@code https://user-info.herokuapp.com/users/{cpf}}) esta
 * indisponivel desde o encerramento do plano gratuito da Heroku, em novembro de 2022. A integracao
 * e implementada exatamente como especificada, mas cercada por estas chaves para que a
 * indisponibilidade do terceiro nao impeca a execucao da aplicacao.
 *
 * @param baseUrl raiz do servico de consulta de associados
 * @param enabled quando {@code false}, a consulta remota e ignorada e qualquer CPF sintaticamente
 *     valido pode votar
 * @param fallbackPermiteVoto comportamento quando o servico esta indisponivel apos retry e circuito
 *     aberto. {@code true} privilegia a disponibilidade da assembleia; {@code false} privilegia o
 *     rigor da verificacao. E uma decisao de negocio, por isso explicita em configuracao.
 * @param connectTimeoutMs tempo maximo para estabelecer a conexao
 * @param readTimeoutMs tempo maximo de espera pela resposta
 */
@ConfigurationProperties(prefix = "app.user-info")
public record UserInfoProperties(
        String baseUrl,
        boolean enabled,
        boolean fallbackPermiteVoto,
        int connectTimeoutMs,
        int readTimeoutMs) {}
