package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.infrastructure.integration.userinfo.UserInfoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Decide se um associado esta habilitado a votar (Tarefa Bonus 1).
 *
 * <p>Isolar a decisao aqui mantem o {@code VotoService} indiferente a existencia
 * de um servico externo: se amanha a cooperativa passar a consultar um cadastro
 * proprio, apenas esta classe muda.
 */
@Component
public class AssociadoValidator {

    private static final Logger log = LoggerFactory.getLogger(AssociadoValidator.class);

    private final UserInfoClient userInfoClient;
    private final UserInfoProperties properties;

    /**
     * Cria o validador.
     *
     * @param userInfoClient cliente do servico externo de CPF
     * @param properties     configuracao da integracao
     */
    public AssociadoValidator(UserInfoClient userInfoClient, UserInfoProperties properties) {
        this.userInfoClient = userInfoClient;
        this.properties = properties;
    }

    /**
     * Garante que o associado pode votar, ou interrompe o fluxo.
     *
     * <p>Quando a integracao esta desligada por configuracao, qualquer CPF
     * sintaticamente valido e aceito. Isso permite executar e avaliar a aplicacao
     * mesmo com o servico externo fora do ar, sem alterar codigo.
     *
     * @param cpf CPF do associado, ja validado nos digitos verificadores
     * @throws AssociadoNaoAutorizadoException se o CPF for desconhecido ou estiver impedido
     */
    public void validarPodeVotar(Cpf cpf) {
        if (!properties.enabled()) {
            log.debug(
                    "Verificacao externa de CPF desabilitada; liberando o voto de {}.",
                    cpf.mascarado());
            return;
        }

        var resposta =
                userInfoClient
                        .consultar(cpf)
                        .orElseThrow(
                                () ->
                                        new AssociadoNaoAutorizadoException(
                                                "CPF nao encontrado na base de associados."));

        if (!resposta.podeVotar()) {
            log.warn("Associado {} esta impedido de votar.", cpf.mascarado());
            throw new AssociadoNaoAutorizadoException(
                    "O associado nao esta habilitado a votar nesta assembleia.");
        }
    }
}
