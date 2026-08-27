package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.Pauta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
