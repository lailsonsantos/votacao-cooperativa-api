package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Acesso as sessoes de votacao.
 */
public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, UUID> {
    /**
     * Busca a sessao de uma pauta.
     *
     * <p>O {@code join fetch} traz a pauta na mesma consulta porque praticamente
     * todo consumidor do resultado precisa do titulo dela; sem isso, cada
     * chamada dispararia um segundo {@code SELECT} pelo carregamento tardio.
     *
     * @param pautaId identificador da pauta
     * @return a sessao, se a pauta ja tiver tido uma aberta
     */
    @Query("select s from SessaoVotacao s join fetch s.pauta p where p.id = :pautaId")
    Optional<SessaoVotacao> findByPautaId(UUID pautaId);

    /**
     * Indica se a pauta ja possui sessao.
     *
     * @param pautaId identificador da pauta
     * @return {@code true} se ja existir uma sessao para a pauta
     */
    @Query("select count(s) > 0 from SessaoVotacao s where s.pauta.id = :pautaId")
    boolean existsByPautaId(UUID pautaId);
}
