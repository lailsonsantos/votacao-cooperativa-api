package br.com.cooperativa.votacao.infrastructure.persistence;

import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.repository.PautaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Adaptador JPA da porta {@link PautaRepository}.
 *
 * <p>A interface estende a porta e {@code JpaRepository} ao mesmo tempo, e os metodos da porta sao
 * {@code default} delegando ao Spring Data. Isso entrega a inversao de dependencia sem escrever
 * nenhuma classe adaptadora: o Spring Data implementa a interface em tempo de execucao e o dominio
 * segue conhecendo apenas a porta.
 *
 * <p>A alternativa &mdash; uma classe adaptadora por repositorio &mdash; daria o mesmo resultado ao
 * custo de tres classes cujo corpo inteiro seria delegacao.
 */
@Repository
public interface PautaJpaRepository extends PautaRepository, JpaRepository<Pauta, UUID> {

    /** {@inheritDoc} */
    @Override
    default Pauta salvar(Pauta pauta) {
        return save(pauta);
    }

    /** {@inheritDoc} */
    @Override
    default Optional<Pauta> buscarPorId(UUID id) {
        return findById(id);
    }

    /** {@inheritDoc} */
    @Override
    default List<Pauta> listarMaisRecentes(int pagina, int tamanho) {
        var ordenacao = Sort.by(Sort.Direction.DESC, "criadaEm");
        return findAll(PageRequest.of(pagina, tamanho, ordenacao)).getContent();
    }

    /** {@inheritDoc} */
    @Override
    default long contar() {
        return count();
    }
}
