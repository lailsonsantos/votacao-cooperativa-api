package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.Voto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Acesso aos votos registrados.
 */
public interface VotoRepository extends JpaRepository<Voto, UUID> {

    /**
     * Conta os votos de uma sessao agrupados por opcao.
     *
     * <p>Esta e a unica consulta usada na apuracao. Ela e servida integralmente
     * pelo indice {@code ix_voto_sessao_opcao}, entao o custo cresce com o numero
     * de opcoes (duas) e nao com o numero de votos.
     *
     * @param sessaoId identificador da sessao
     * @return uma linha por opcao efetivamente votada
     */
    @Query(
            """
            select v.opcao as opcao, count(v) as total
              from Voto v
             where v.sessao.id = :sessaoId
             group by v.opcao
            """)
    List<ContagemVotos> contarPorOpcao(UUID sessaoId);

    /**
     * Indica se o associado ja votou na sessao.
     *
     * <p>Usado apenas para leitura, nas telas, de modo a nao oferecer a opcao de
     * voto a quem ja votou. <strong>Nao</strong> e usado como guarda antes do
     * {@code INSERT}: a unicidade continua sendo garantida pela constraint, que e
     * o unico mecanismo correto sob concorrencia.
     *
     * @param sessaoId    identificador da sessao
     * @param associadoId CPF do associado
     * @return {@code true} se ja existir voto do associado na sessao
     */
    boolean existsBySessaoIdAndAssociadoId(UUID sessaoId, String associadoId);
}
