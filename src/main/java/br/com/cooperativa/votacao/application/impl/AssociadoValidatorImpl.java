package br.com.cooperativa.votacao.application.impl;

import br.com.cooperativa.votacao.application.AssociadoValidator;
import br.com.cooperativa.votacao.application.port.ConsultaAptidaoParaVotar;
import br.com.cooperativa.votacao.config.UserInfoProperties;
import br.com.cooperativa.votacao.domain.exception.AssociadoNaoAutorizadoException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssociadoValidatorImpl implements AssociadoValidator {

    private final ConsultaAptidaoParaVotar consultaAptidao;
    private final UserInfoProperties properties;

    @Override
    public void validarPodeVotar(Cpf cpf) {
        // Integracao desligada: qualquer CPF valido vota. Serve pra rodar a
        // aplicacao sem depender do servico externo.
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
