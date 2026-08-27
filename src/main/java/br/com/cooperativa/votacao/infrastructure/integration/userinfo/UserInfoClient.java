package br.com.cooperativa.votacao.infrastructure.integration.userinfo;

import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.model.Cpf;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente do servico externo que verifica se um CPF pode votar (Tarefa Bonus 1).
 *
 * <p>O endpoint indicado no enunciado esta fora do ar desde o encerramento do
 * plano gratuito da Heroku. A integracao e implementada exatamente como
 * especificada, e a resiliencia em torno dela existe para que a indisponibilidade
 * de um terceiro nao impeca a assembleia de acontecer:
 *
 * <ul>
 *   <li><strong>Timeouts curtos</strong> &mdash; sem eles, threads do servidor
 *       ficariam presas ate esgotar o pool durante uma votacao;
 *   <li><strong>Retry</strong> apenas para falhas transientes;
 *   <li><strong>Circuit breaker</strong> &mdash; apos uma sequencia de falhas,
 *       para de tentar por um intervalo em vez de castigar um servico que ja
 *       esta em dificuldade;
 *   <li><strong>Fallback configuravel</strong> &mdash; o que fazer quando nao ha
 *       resposta e uma decisao de negocio, nao um acidente, e por isso vive em
 *       {@code app.user-info.fallback-permite-voto}.
 * </ul>
 */
@Component
public class UserInfoClient {

    /** Nome da instancia de resiliencia declarada em {@code application.yml}. */
    private static final String INSTANCIA = "userInfo";

    private static final Logger log = LoggerFactory.getLogger(UserInfoClient.class);

    private final RestClient restClient;
    private final UserInfoProperties properties;

    /**
     * Cria o cliente.
     *
     * @param userInfoRestClient cliente HTTP ja configurado com URL base e timeouts
     * @param properties         configuracao do servico externo
     */
    public UserInfoClient(RestClient userInfoRestClient, UserInfoProperties properties) {
        this.restClient = userInfoRestClient;
        this.properties = properties;
    }

    /**
     * Consulta a situacao de um CPF no servico externo.
     *
     * <p>Um {@code 404} do servico significa CPF desconhecido e e traduzido em
     * {@link Optional#empty()}, e nao em erro: e uma resposta legitima do
     * contrato, nao uma falha de comunicacao. Ja uma falha de rede propaga a
     * excecao, para que o retry e o circuit breaker possam agir.
     *
     * @param cpf CPF ja validado nos digitos verificadores
     * @return a resposta do servico, ou vazio se o CPF for desconhecido
     * @throws RestClientException se o servico estiver indisponivel apos as tentativas
     */
    @Retry(name = INSTANCIA)
    @CircuitBreaker(name = INSTANCIA, fallbackMethod = "consultarFallback")
    public Optional<UserInfoResponse> consultar(Cpf cpf) {
        log.debug("Consultando servico externo para o CPF {}", cpf.mascarado());

        var resposta =
                restClient
                        .get()
                        .uri("/users/{cpf}", cpf.numero())
                        .retrieve()
                        .onStatus(
                                status -> status.value() == 404,
                                (request, response) -> {
                                    // Nao lanca: CPF desconhecido e resposta valida do
                                    // contrato e sera representado por Optional vazio.
                                })
                        .body(UserInfoResponse.class);

        return Optional.ofNullable(resposta);
    }

    /**
     * Fallback acionado quando o servico externo esta indisponivel.
     *
     * <p>A decisao entre liberar ou bloquear o voto e de negocio: liberar
     * privilegia a realizacao da assembleia, bloquear privilegia o rigor da
     * verificacao. O padrao e liberar, porque o enunciado descreve o servico como
     * instavel por natureza e uma cooperativa nao pode ter sua assembleia
     * interrompida por indisponibilidade de terceiro.
     *
     * @param cpf   CPF consultado
     * @param erro  falha que disparou o fallback
     * @return uma resposta sintetica coerente com a configuracao vigente
     */
    @SuppressWarnings("unused")
    private Optional<UserInfoResponse> consultarFallback(Cpf cpf, Throwable erro) {
        log.warn(
                "Servico de verificacao de CPF indisponivel para {}. Aplicando fallback "
                        + "(permite voto = {}). Causa: {}",
                cpf.mascarado(),
                properties.fallbackPermiteVoto(),
                erro.toString());

        var status =
                properties.fallbackPermiteVoto()
                        ? StatusAssociado.ABLE_TO_VOTE
                        : StatusAssociado.UNABLE_TO_VOTE;

        return Optional.of(new UserInfoResponse(status));
    }
}
