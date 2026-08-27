package br.com.cooperativa.votacao.api.error;

import br.com.cooperativa.votacao.domain.exception.TipoErro;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Traduz a natureza de uma falha de negocio para um status HTTP.
 *
 * <p>Esta e a unica classe da aplicacao que conhece, ao mesmo tempo, o vocabulario do dominio e o
 * do protocolo. O dominio declara <em>o que</em> aconteceu ({@link TipoErro}); a camada de API
 * decide <em>como</em> isso se expressa em HTTP.
 *
 * <p>Concentrar a traducao aqui e o que permite expor o mesmo dominio por outro transporte &mdash;
 * mensageria, gRPC &mdash; sem tocar em nenhuma regra. E e o que mantem o principio aberto-fechado:
 * uma regra nova reutiliza uma natureza existente e esta classe nao muda.
 */
final class MapeadorDeStatus {

    /**
     * Correspondencia entre natureza da falha e status HTTP.
     *
     * <p>{@code EnumMap} em vez de {@code switch}: a tabela e um dado, nao um fluxo, e fica legivel
     * de uma so vez.
     */
    private static final Map<TipoErro, HttpStatus> STATUS = new EnumMap<>(TipoErro.class);

    static {
        // O cliente enviou algo que nao satisfaz o formato exigido.
        STATUS.put(TipoErro.ENTRADA_INVALIDA, HttpStatus.BAD_REQUEST);
        // O recurso referenciado nao existe.
        STATUS.put(TipoErro.NAO_ENCONTRADO, HttpStatus.NOT_FOUND);
        // A operacao conflita com o estado atual do recurso.
        STATUS.put(TipoErro.CONFLITO, HttpStatus.CONFLICT);
        // A requisicao esta bem formada, mas uma regra impede o processamento —
        // que e exatamente a semantica de 422, e nao de 400.
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
