package br.com.cooperativa.votacao.domain.exception;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import lombok.Getter;

@Getter
public abstract class NegocioException extends RuntimeException {

    private final TipoErro tipo;

    private final String codigo;

    protected NegocioException(TipoErro tipo, String codigo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    protected NegocioException(TipoErro tipo, String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    public abstract String getTitulo();
}
