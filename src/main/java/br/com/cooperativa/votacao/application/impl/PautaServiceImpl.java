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

@Service
@RequiredArgsConstructor
@Slf4j
public class PautaServiceImpl implements PautaService {

    private final PautaRepository pautaRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Pauta criar(String titulo, String descricao) {
        var pauta = pautaRepository.salvar(Pauta.criar(titulo, descricao, clock.instant()));
        log.info("Pauta criada. id={} título='{}'", pauta.getId(), pauta.getTitulo());
        return pauta;
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Pauta> listar(int pagina, int tamanho) {
        // A contagem total acompanha a fatia porque o envelope de paginação da API
        // a expoe; sem ela o cliente não saberia quantas páginas existem.
        return new Pagina<>(
                pautaRepository.listarMaisRecentes(pagina, tamanho),
                pagina,
                tamanho,
                pautaRepository.contar());
    }

    @Override
    @Transactional(readOnly = true)
    public Pauta buscar(UUID id) {
        return pautaRepository
                .buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta", id));
    }
}
