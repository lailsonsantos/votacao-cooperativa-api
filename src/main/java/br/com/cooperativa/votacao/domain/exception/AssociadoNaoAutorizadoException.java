package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;

public class AssociadoNaoAutorizadoException extends NegocioException {

    public AssociadoNaoAutorizadoException(String mensagem) {
        super(TipoErro.REGRA_VIOLADA, "associado-nao-autorizado", mensagem);
    }

    @Override
    public String getTitulo() {
        return "Associado nao autorizado a votar";
    }
}
