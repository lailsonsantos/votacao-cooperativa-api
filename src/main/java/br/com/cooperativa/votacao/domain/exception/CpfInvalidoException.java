package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.model.Cpf;
import org.springframework.http.HttpStatus;

/**
 * Lancada quando o CPF informado nao passa na validacao dos digitos verificadores.
 *
 * <p>A verificacao acontece antes de qualquer chamada remota: um numero que nao
 * pode existir nao merece uma ida a rede, e o cliente recebe {@code 400}
 * imediatamente.
 */
public class CpfInvalidoException extends NegocioException {
    /**
     * Cria a excecao.
     *
     * @param valor CPF recusado, mascarado na mensagem devolvida
     */
    public CpfInvalidoException(String valor) {
        super(
                HttpStatus.BAD_REQUEST,
                "cpf-invalido",
                "O CPF informado (%s) nao e valido.".formatted(Cpf.mascarar(valor)));
    }

    /** {@inheritDoc} */
    @Override
    public String getTitulo() {
        return "CPF invalido";
    }
}
