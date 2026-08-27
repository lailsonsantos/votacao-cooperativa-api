package br.com.cooperativa.votacao.infrastructure.integration.userinfo;

import br.com.cooperativa.votacao.application.port.ConsultaAptidaoParaVotar;
import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.model.Cpf;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserInfoClient implements ConsultaAptidaoParaVotar {
    private static final String INSTANCIA = "userInfo";

    private final RestClient restClient;
    private final UserInfoProperties properties;

    /**
     * Consulta a situação de um CPF no serviço externo.
     *
     * @param cpf CPF já validado nos dígitos verificadores
     * @return a resposta do serviço, ou vazio se o CPF for desconhecido
     * @throws RestClientException se o serviço estiver indisponível após as tentativas
     */
    @Override
    @Retry(name = INSTANCIA)
    @CircuitBreaker(name = INSTANCIA, fallbackMethod = "consultarFallback")
    public Optional<AptidaoParaVotar> consultar(Cpf cpf) {
        log.debug("Consultando servico externo para o CPF {}", cpf.mascarado());

        var resposta =
                restClient
                        .get()
                        .uri("/users/{cpf}", cpf.numero())
                        .retrieve()
                        .onStatus(
                                status -> status.value() == 404,
                                (request, response) -> {
                                    // Não lança: CPF desconhecido é resposta válida do
                                    // contrato e será representado por Optional vazio.
                                })
                        .body(UserInfoResponse.class);

        // O enum do fornecedor não atravessa a fronteira: a porta expressa apenas
        // a decisão que o domínio precisa tomar.
        return Optional.ofNullable(resposta).map(r -> new AptidaoParaVotar(r.podeVotar()));
    }

    /**
     * Fallback acionado quando o serviço externo está indisponível.
     *
     * @param cpf CPF consultado
     * @param erro falha que disparou o fallback
     * @return uma resposta sintetica coerente com a configuração vigente
     */
    @SuppressWarnings("unused")
    private Optional<AptidaoParaVotar> consultarFallback(Cpf cpf, Throwable erro) {
        log.warn(
                "Servico de verificacao de CPF indisponivel para {}. Aplicando fallback "
                        + "(permite voto = {}). Causa: {}",
                cpf.mascarado(),
                properties.fallbackPermiteVoto(),
                erro.toString());

        return Optional.of(new AptidaoParaVotar(properties.fallbackPermiteVoto()));
    }
}
