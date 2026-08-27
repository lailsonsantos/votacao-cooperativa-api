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

@Repository
public interface PautaJpaRepository extends PautaRepository, JpaRepository<Pauta, UUID> {

    @Override
    default Pauta salvar(Pauta pauta) {
        return save(pauta);
    }

    @Override
    default Optional<Pauta> buscarPorId(UUID id) {
        return findById(id);
    }

    @Override
    default List<Pauta> listarMaisRecentes(int pagina, int tamanho) {
        var ordenacao = Sort.by(Sort.Direction.DESC, "criadaEm");
        return findAll(PageRequest.of(pagina, tamanho, ordenacao)).getContent();
    }

    @Override
    default long contar() {
        return count();
    }
}
