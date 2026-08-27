package br.com.cooperativa.votacao.infrastructure.persistence;

import br.com.cooperativa.votacao.domain.model.Voto;
import br.com.cooperativa.votacao.domain.repository.ContagemVotos;
import br.com.cooperativa.votacao.domain.repository.VotoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Adaptador JPA da porta {@link VotoRepository}. */
@Repository
public interface VotoJpaRepository extends VotoRepository, JpaRepository<Voto, UUID> {

    /** {@inheritDoc} */
    @Override
    default Voto salvarEConfirmar(Voto voto) {
        return saveAndFlush(voto);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Esta e a unica consulta usada na apuracao. Ela e servida integralmente pelo indice {@code
     * ix_voto_sessao_opcao}, entao o custo cresce com o numero de opcoes (duas) e nao com o numero
     * de votos &mdash; que e o que permite apurar centenas de milhares de votos sem carregar nenhum
     * deles em memoria.
     */
    @Override
    @Query(
            """
            select v.opcao as opcao, count(v) as total
              from Voto v
             where v.sessao.id = :sessaoId
             group by v.opcao
            """)
    List<ContagemVotos> contarPorOpcao(UUID sessaoId);

    /** {@inheritDoc} */
    @Override
    @Query(
            """
            select count(v) > 0
              from Voto v
             where v.sessao.id = :sessaoId
               and v.associadoId = :associadoId
            """)
    boolean existeVotoDoAssociado(UUID sessaoId, String associadoId);
}
