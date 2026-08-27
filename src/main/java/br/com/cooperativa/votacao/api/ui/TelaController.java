package br.com.cooperativa.votacao.api.ui;

import br.com.cooperativa.votacao.api.ui.builder.TelaFactory;
import br.com.cooperativa.votacao.api.ui.dto.AcaoTelaRequest;
import br.com.cooperativa.votacao.api.ui.dto.Tela;
import br.com.cooperativa.votacao.application.PautaService;
import br.com.cooperativa.votacao.application.ResultadoService;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import br.com.cooperativa.votacao.application.VotoService;
import br.com.cooperativa.votacao.config.AppProperties;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import br.com.cooperativa.votacao.domain.model.OpcaoVoto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Superficie Server-Driven UI: devolve as telas do Anexo 1 ao cliente.
 *
 * <p>E o foco declarado da avaliacao. O cliente nao conhece o dominio: recebe
 * uma descricao de tela, renderiza, e ao acionar um botao envia {@code POST}
 * para a URL indicada. Cada acao <strong>executa a operacao e devolve a proxima
 * tela</strong>, de modo que a navegacao inteira e dirigida pelo servidor.
 *
 * <p>Este controlador e uma casca fina: toda regra vive nos mesmos servicos de
 * aplicacao usados pela API REST {@code /api/v1}. Nao ha logica de negocio
 * duplicada entre as duas superficies.
 */
@RestController
@RequestMapping("/api/v1/telas")
@Tag(
        name = "Telas (Server-Driven UI)",
        description =
                "Descricoes de tela no formato do Anexo 1. Cada POST executa a acao e devolve"
                        + " a proxima tela.")
@RequiredArgsConstructor
public class TelaController {
    private final PautaService pautaService;
    private final SessaoVotacaoService sessaoService;
    private final VotoService votoService;
    private final ResultadoService resultadoService;
    private final TelaFactory telas;
    private final AppProperties appProperties;
    private final Clock clock;

    /**
     * Menu inicial.
     *
     * @return tela SELECAO com as acoes disponiveis
     */
    @GetMapping
    @Operation(summary = "Menu inicial da aplicacao")
    public Tela menu() {
        return telas.menu();
    }

    /**
     * Lista as pautas cadastradas.
     *
     * @param pageable pagina solicitada
     * @return tela SELECAO com uma opcao por pauta
     */
    @GetMapping("/pautas")
    @Operation(summary = "Lista as pautas como tela de selecao")
    public Tela listarPautas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return telas.listaPautas(pautaService.listar(page, size));
    }

    /**
     * Formulario de cadastro de pauta.
     *
     * @return tela FORMULARIO com os campos de titulo e descricao
     */
    @GetMapping("/pautas/nova")
    @Operation(summary = "Formulario de cadastro de pauta")
    public Tela formularioNovaPauta() {
        return telas.novaPauta();
    }

    /**
     * Cadastra a pauta e devolve a tela da pauta recem-criada.
     *
     * @param acao campos digitados no formulario
     * @return tela FORMULARIO da pauta criada, pronta para abrir a sessao
     */
    @PostMapping("/pautas")
    @Operation(summary = "Cadastra a pauta e devolve a tela da pauta criada")
    public Tela criarPauta(@RequestBody AcaoTelaRequest acao) {
        var pauta =
                pautaService.criar(
                        acao.texto(TelaFactory.CAMPO_TITULO),
                        acao.texto(TelaFactory.CAMPO_DESCRICAO));

        return telas.pautaSemSessao(pauta, appProperties.sessao().duracaoPadraoMinutos());
    }

    /**
     * Tela de uma pauta, cujo conteudo depende do estado da sessao.
     *
     * <p>Sem sessao, oferece a abertura; com sessao aberta, coleta o CPF para
     * votar; com sessao encerrada, mostra o resultado. Essa decisao vive no
     * servidor, e nao no cliente &mdash; que e justamente o objetivo do padrao.
     *
     * @param id identificador da pauta
     * @return a tela correspondente ao estado atual da pauta
     */
    @GetMapping("/pautas/{id}")
    @Operation(summary = "Tela da pauta, variando conforme o estado da sessao")
    public Tela pauta(@PathVariable UUID id) {
        var pauta = pautaService.buscar(id);
        var sessao = sessaoService.buscar(id);

        if (sessao.isEmpty()) {
            return telas.pautaSemSessao(pauta, appProperties.sessao().duracaoPadraoMinutos());
        }

        var agora = clock.instant();
        if (sessao.get().estaAberta(agora)) {
            return telas.identificacao(sessao.get(), agora);
        }

        return telas.resultado(resultadoService.apurar(id));
    }

    /**
     * Abre a sessao de votacao e devolve a tela de identificacao.
     *
     * @param id   identificador da pauta
     * @param acao campos digitados, incluindo a duracao escolhida
     * @return tela FORMULARIO que coleta o CPF do associado
     */
    @PostMapping("/pautas/{id}/sessao")
    @Operation(summary = "Abre a sessao e devolve a tela de votacao")
    public Tela abrirSessao(@PathVariable UUID id, @RequestBody AcaoTelaRequest acao) {
        var sessao = sessaoService.abrir(id, acao.inteiro(TelaFactory.CAMPO_DURACAO));
        return telas.identificacao(sessao, clock.instant());
    }

    /**
     * Valida o CPF e devolve as opcoes de voto.
     *
     * <p>A validacao acontece aqui, antes de mostrar "Sim" e "Nao", para que o
     * associado impedido descubra o problema no passo da identificacao e nao
     * depois de ja ter escolhido seu voto.
     *
     * @param id   identificador da pauta
     * @param acao campos digitados, incluindo o CPF
     * @return tela SELECAO com as opcoes Sim e Nao
     */
    @PostMapping("/pautas/{id}/votos/identificacao")
    @Operation(summary = "Valida o CPF e devolve as opcoes de voto")
    public Tela identificar(@PathVariable UUID id, @RequestBody AcaoTelaRequest acao) {
        var pauta = pautaService.buscar(id);
        var sessao = sessaoService.buscarObrigatoria(id);

        if (!sessao.estaAberta(clock.instant())) {
            throw new SessaoEncerradaException(id);
        }

        var cpf = Cpf.de(acao.texto(TelaFactory.CAMPO_CPF));

        if (votoService.jaVotou(id, cpf)) {
            return telas.erro(
                    pauta.getTitulo(), "Voce ja registrou seu voto nesta pauta.");
        }

        return telas.opcoesDeVoto(pauta, cpf.numero());
    }

    /**
     * Registra o voto e devolve a tela de resultado.
     *
     * @param id   identificador da pauta
     * @param acao corpo da opcao acionada, contendo CPF e opcao
     * @return tela FORMULARIO com a apuracao
     */
    @PostMapping("/pautas/{id}/votos")
    @Operation(summary = "Registra o voto e devolve a tela de resultado")
    public Tela votar(@PathVariable UUID id, @RequestBody AcaoTelaRequest acao) {
        var cpf = Cpf.de(acao.texto(TelaFactory.CAMPO_CPF));
        var opcao = OpcaoVoto.valueOf(acao.texto("opcao").toUpperCase());

        votoService.registrar(id, cpf, opcao);
        return telas.resultado(resultadoService.apurar(id));
    }

    /**
     * Tela de resultado da apuracao.
     *
     * @param id identificador da pauta
     * @return tela FORMULARIO com os numeros e o desfecho
     */
    @GetMapping("/pautas/{id}/resultado")
    @Operation(summary = "Tela de resultado da apuracao")
    public Tela resultado(@PathVariable UUID id) {
        return telas.resultado(resultadoService.apurar(id));
    }
}
