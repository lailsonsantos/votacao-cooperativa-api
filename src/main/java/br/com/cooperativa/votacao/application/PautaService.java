package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.model.Pagina;
import br.com.cooperativa.votacao.domain.model.Pauta;
import java.util.UUID;

public interface PautaService {

    /**
     * Cadastra uma nova pauta.
     *
     * @param titulo titulo da pauta, já validado na borda
     * @param descrição descrição opcional
     * @return a pauta persistida
     */
    Pauta criar(String titulo, String descricao);

    /**
     * Lista as pautas da mais recente para a mais antiga.
     *
     * @param pagina indice da pagina, iniciando em zero
     * @param tamanho quantidade de itens por pagina
     * @return a pagina de pautas
     */
    Pagina<Pauta> listar(int pagina, int tamanho);

    /**
     * Busca uma pauta pelo identificador.
     *
     * @param id identificador da pauta
     * @return a pauta encontrada
     * @throws RecursoNaoEncontradoException se não existir pauta com o identificador
     */
    Pauta buscar(UUID id);
}
