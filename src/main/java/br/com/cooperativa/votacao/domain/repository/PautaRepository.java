package br.com.cooperativa.votacao.domain.repository;

import br.com.cooperativa.votacao.domain.model.Pauta;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso as pautas cadastradas.
 *
 * <p>A listagem herda {@code findAll(Pageable)} do Spring Data: a paginacao e
 * obrigatoria na API para que uma cooperativa com milhares de pautas nunca
 * produza uma resposta ilimitada.
 */
public interface PautaRepository extends JpaRepository<Pauta, UUID> {}
