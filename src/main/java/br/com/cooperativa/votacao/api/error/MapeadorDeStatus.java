package br.com.cooperativa.votacao.api.error;

import br.com.cooperativa.votacao.domain.enums.TipoErro;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

final class MapeadorDeStatus {

    private static final Map<TipoErro, HttpStatus> STATUS = new EnumMap<>(TipoErro.class);

    static {
        STATUS.put(TipoErro.ENTRADA_INVALIDA, HttpStatus.BAD_REQUEST);

        STATUS.put(TipoErro.NAO_ENCONTRADO, HttpStatus.NOT_FOUND);

        STATUS.put(TipoErro.CONFLITO, HttpStatus.CONFLICT);

        STATUS.put(TipoErro.REGRA_VIOLADA, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /** Classe utilitaria: nao deve ser instanciada. */
    private MapeadorDeStatus() {}

    /**
     * Devolve o status HTTP correspondente a natureza da falha.
     *
     * @param tipo natureza da falha declarada pelo dominio
     * @return o status HTTP correspondente
     */
    static HttpStatus de(TipoErro tipo) {
        // Todas as naturezas estao mapeadas; o default existe para que a inclusao
        // de um valor novo no enum degrade para 500 em vez de lancar NPE.
        return STATUS.getOrDefault(tipo, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
