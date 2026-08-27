package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.model.Cpf;

public class CpfInvalidoException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param valor CPF recusado, mascarado na mensagem devolvida
     */
    public CpfInvalidoException(String valor) {
        super(
                TipoErro.ENTRADA_INVALIDA,
                "cpf-invalido",
                "O CPF informado (%s) nao e valido.".formatted(Cpf.mascarar(valor)));
    }

    @Override
    public String getTitulo() {
        return "CPF invalido";
    }
}
