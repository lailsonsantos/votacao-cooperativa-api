package br.com.cooperativa.votacao.application.impl;

import br.com.cooperativa.votacao.application.AssociadoValidator;
import br.com.cooperativa.votacao.application.port.ConsultaAptidaoParaVotar;
import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Verificacao do direito de voto apoiada na porta de consulta de aptidao.
 *
 * <p>Esta classe nao conhece HTTP, nem o servico externo, nem o formato da
 * resposta dele: pede a aptidao a porta e traduz a negativa em erro de negocio.
 * Toda a mecanica de integracao vive no adaptador.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssociadoValidatorImpl implements AssociadoValidator {

    private final ConsultaAptidaoParaVotar consultaAptidao;
    private final UserInfoProperties properties;

    /** {@inheritDoc} */
    @Override
    public void validarPodeVotar(Cpf cpf) {
        // Com a integracao desligada, qualquer CPF valido pode votar. E o caminho
        // que permite executar e avaliar a aplicacao com o servico do enunciado
        // fora do ar, sem alterar codigo.
        if (!properties.enabled()) {
            log.debug(
                    "Verificacao externa de aptidao desabilitada; liberando o voto de {}.",
                    cpf.mascarado());
            return;
        }

        var aptidao =
                consultaAptidao
                        .consultar(cpf)
                        .orElseThrow(
                                () ->
                                        new AssociadoNaoAutorizadoException(
                                                "CPF nao encontrado na base de associados."));

        if (!aptidao.podeVotar()) {
            log.warn("Associado {} esta impedido de votar.", cpf.mascarado());
            throw new AssociadoNaoAutorizadoException(
                    "O associado nao esta habilitado a votar nesta assembleia.");
        }
    }
}
