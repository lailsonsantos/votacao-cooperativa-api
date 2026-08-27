package br.com.cooperativa.votacao.application;

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
 * Casos de uso de cadastro e consulta de pautas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PautaService {
    private final PautaRepository pautaRepository;
    private final Clock clock;

    /**
     * Cadastra uma nova pauta.
     *
     * @param titulo    titulo da pauta, ja validado na borda
     * @param descricao descricao opcional
     * @return a pauta persistida
     */
    @Transactional
    public Pauta criar(String titulo, String descricao) {
        var pauta = pautaRepository.salvar(Pauta.criar(titulo, descricao, clock.instant()));
        log.info("Pauta criada. id={} titulo='{}'", pauta.getId(), pauta.getTitulo());
        return pauta;
    }

    /**
     * Lista as pautas de forma paginada.
     *
     * <p>A paginacao e obrigatoria por contrato: sem ela, uma cooperativa com
     * milhares de pautas produziria uma resposta ilimitada.
     *
     * @param pagina  indice da pagina, iniciando em zero
     * @param tamanho quantidade de itens por pagina
     * @return a pagina de pautas
     */
    @Transactional(readOnly = true)
    public Pagina<Pauta> listar(int pagina, int tamanho) {
        return new Pagina<>(
                pautaRepository.listarMaisRecentes(pagina, tamanho),
                pagina,
                tamanho,
                pautaRepository.contar());
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
                .buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta", id));
    }
}
