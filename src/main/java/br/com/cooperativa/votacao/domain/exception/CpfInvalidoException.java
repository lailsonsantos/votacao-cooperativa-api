package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import br.com.cooperativa.votacao.domain.model.Cpf;

public class CpfInvalidoException extends NegocioException {

    public CpfInvalidoException(String valor) {
        super(
                TipoErro.ENTRADA_INVALIDA,
                "cpf-invalido",
                "O CPF informado (%s) não é válido.".formatted(Cpf.mascarar(valor)));
    }

    @Override
    public String getTitulo() {
        return "CPF inválido";
    }
}
