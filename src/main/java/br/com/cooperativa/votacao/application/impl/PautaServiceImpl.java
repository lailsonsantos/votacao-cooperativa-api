package br.com.cooperativa.votacao.application.impl;

import br.com.cooperativa.votacao.application.PautaService;
import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.model.Pagina;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.repository.PautaRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacao dos casos de uso de pautas.
 *
 * <p>O contrato, com {@code @param}, {@code @return} e {@code @throws}, vive na
 * interface {@link PautaService}. Aqui documenta-se apenas <em>como</em> e
 * <em>por que</em> cada operacao e feita desta forma &mdash; a duplicacao do
 * contrato nos dois arquivos so criaria duas versoes para divergir.
 *
 * <p>{@code @Transactional} fica na implementacao, e nao na interface: a
 * demarcacao transacional e uma decisao de infraestrutura, nao parte do contrato
 * que a camada de API enxerga.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PautaServiceImpl implements PautaService {

    private final PautaRepository pautaRepository;
    private final Clock clock;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Pauta criar(String titulo, String descricao) {
        var pauta = pautaRepository.salvar(Pauta.criar(titulo, descricao, clock.instant()));
        log.info("Pauta criada. id={} titulo='{}'", pauta.getId(), pauta.getTitulo());
        return pauta;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Pagina<Pauta> listar(int pagina, int tamanho) {
        // A contagem total acompanha a fatia porque o envelope de paginacao da API
        // a expoe; sem ela o cliente nao saberia quantas paginas existem.
        return new Pagina<>(
                pautaRepository.listarMaisRecentes(pagina, tamanho),
                pagina,
                tamanho,
                pautaRepository.contar());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Pauta buscar(UUID id) {
        return pautaRepository
                .buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta", id));
    }
}
