package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.Pauta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida para a persistencia de pautas.
 *
 * <p>Interface declarada pelo dominio e implementada pela infraestrutura: e o dominio quem define
 * de que operacoes precisa, nao a biblioteca de persistencia quem define o que ele pode fazer.
 *
 * <p>A porta declara apenas os quatro metodos efetivamente usados. Herdar {@code JpaRepository}
 * traria mais de vinte metodos que ninguem chama &mdash; incluindo {@code deleteAll()}, que jamais
 * deveria estar ao alcance de um caso de uso desta aplicacao.
 */
public interface PautaRepository {

    /**
     * Persiste uma pauta.
     *
     * @param pauta pauta a persistir
     * @return a pauta persistida
     */
    Pauta salvar(Pauta pauta);

    /**
     * Busca uma pauta pelo identificador.
     *
     * @param id identificador da pauta
     * @return a pauta, se existir
     */
    Optional<Pauta> buscarPorId(UUID id);

    /**
     * Lista as pautas da mais recente para a mais antiga.
     *
     * @param pagina indice da pagina, iniciando em zero
     * @param tamanho quantidade de itens por pagina
     * @return os itens da pagina solicitada
     */
    List<Pauta> listarMaisRecentes(int pagina, int tamanho);

    /**
     * Conta o total de pautas cadastradas.
     *
     * @return a quantidade de pautas
     */
    long contar();
}
