package br.com.cooperativa.votacao.infrastructure.persistence;

import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.repository.SessaoVotacaoRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link SessaoVotacaoRepository}. */
@Repository
public interface SessaoVotacaoJpaRepository
        extends SessaoVotacaoRepository, JpaRepository<SessaoVotacao, UUID> {

    @Override
    default SessaoVotacao salvarEConfirmar(SessaoVotacao sessao) {
        return saveAndFlush(sessao);
    }

    @Override
    @Query("select s from SessaoVotacao s join fetch s.pauta p where p.id = :pautaId")
    Optional<SessaoVotacao> buscarPorPauta(UUID pautaId);

    @Override
    @Query("select count(s) > 0 from SessaoVotacao s where s.pauta.id = :pautaId")
    boolean existePorPauta(UUID pautaId);
}
