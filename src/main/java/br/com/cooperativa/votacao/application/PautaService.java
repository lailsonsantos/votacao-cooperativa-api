package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.repository.PautaRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de cadastro e consulta de pautas.
 */
@Service
public class PautaService {

    private static final Logger log = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository pautaRepository;
    private final Clock clock;

    /**
     * Cria o servico.
     *
     * @param pautaRepository acesso as pautas
     * @param clock           relogio injetado, para testes deterministicos
     */
    public PautaService(PautaRepository pautaRepository, Clock clock) {
        this.pautaRepository = pautaRepository;
        this.clock = clock;
    }

    /**
     * Cadastra uma nova pauta.
     *
     * @param titulo    titulo da pauta, ja validado na borda
     * @param descricao descricao opcional
     * @return a pauta persistida
     */
    @Transactional
    public Pauta criar(String titulo, String descricao) {
        var pauta = pautaRepository.save(Pauta.criar(titulo, descricao, clock.instant()));
        log.info("Pauta criada. id={} titulo='{}'", pauta.getId(), pauta.getTitulo());
        return pauta;
    }

    /**
     * Lista as pautas de forma paginada.
     *
     * <p>A paginacao e obrigatoria por contrato: sem ela, uma cooperativa com
     * milhares de pautas produziria uma resposta ilimitada.
     *
     * @param pageable pagina e ordenacao solicitadas
     * @return a pagina de pautas
     */
    @Transactional(readOnly = true)
    public Page<Pauta> listar(Pageable pageable) {
        return pautaRepository.findAll(pageable);
    }

    /**
     * Busca uma pauta pelo identificador.
     *
     * @param id identificador da pauta
     * @return a pauta encontrada
     * @throws RecursoNaoEncontradoException se nao existir pauta com o identificador
     */
    @Transactional(readOnly = true)
    public Pauta buscar(UUID id) {
        return pautaRepository
                .findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta", id));
    }
}
