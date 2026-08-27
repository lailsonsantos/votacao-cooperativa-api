package br.com.cooperativa.votacao.application;

import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.model.Pagina;
import br.com.cooperativa.votacao.domain.model.Pauta;
import java.util.UUID;

/**
 * Casos de uso de cadastro e consulta de pautas.
 *
 * <p>Porta de entrada da aplicacao. Enquanto as portas de saida
 * ({@code domain.repository}, {@code application.port}) declaram o que a
 * aplicacao precisa do mundo externo, esta interface declara o que o mundo
 * externo pode pedir a aplicacao.
 *
 * <p>A camada de API depende desta abstracao, e nao da implementacao. E o que
 * permite que as duas superficies HTTP &mdash; REST e telas do Anexo 1 &mdash;
 * compartilhem o mesmo contrato sem enxergar detalhe de implementacao, e o que
 * torna trivial substituir o comportamento em teste.
 *
 * @see br.com.cooperativa.votacao.application.impl.PautaServiceImpl
 */
public interface PautaService {

    /**
     * Cadastra uma nova pauta.
     *
     * @param titulo    titulo da pauta, ja validado na borda
     * @param descricao descricao opcional
     * @return a pauta persistida
     */
    Pauta criar(String titulo, String descricao);

    /**
     * Lista as pautas da mais recente para a mais antiga.
     *
     * <p>A paginacao e obrigatoria por contrato: sem ela, uma cooperativa com
     * milhares de pautas produziria uma resposta ilimitada.
     *
     * @param pagina  indice da pagina, iniciando em zero
     * @param tamanho quantidade de itens por pagina
     * @return a pagina de pautas
     */
    Pagina<Pauta> listar(int pagina, int tamanho);

    /**
     * Busca uma pauta pelo identificador.
     *
     * @param id identificador da pauta
     * @return a pauta encontrada
     * @throws RecursoNaoEncontradoException se nao existir pauta com o identificador
     */
    Pauta buscar(UUID id);
}
