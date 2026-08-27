package br.com.cooperativa.votacao.api.ui;

import br.com.cooperativa.votacao.api.ui.builder.TelaFactory;
import br.com.cooperativa.votacao.api.ui.dto.AcaoTelaRequest;
import br.com.cooperativa.votacao.api.ui.dto.Tela;
import br.com.cooperativa.votacao.application.PautaService;
import br.com.cooperativa.votacao.application.ResultadoService;
import br.com.cooperativa.votacao.application.SessaoVotacaoService;
import br.com.cooperativa.votacao.application.VotoService;
import br.com.cooperativa.votacao.config.AppProperties;
import br.com.cooperativa.votacao.domain.enums.OpcaoVoto;
import br.com.cooperativa.votacao.domain.exception.SessaoEncerradaException;
import br.com.cooperativa.votacao.domain.model.Cpf;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telas")
@Validated
@Tag(
        name = "Telas (Server-Driven UI)",
        description =
                "Descrições de tela no formato do Anexo 1. Cada POST executa a ação e devolve"
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

    @GetMapping
    @Operation(summary = "Menu inicial da aplicação")
    public Tela menu() {
        return telas.menu();
    }

    @GetMapping("/pautas")
    @Operation(summary = "Lista as pautas como tela de seleção")
    public Tela listarPautas(
            @RequestParam(defaultValue = "0")
                    @Min(value = 0, message = "A página não pode ser negativa.")
                    int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "O tamanho da página deve ser ao menos 1.")
                    @Max(value = 100, message = "O tamanho da página não pode passar de 100.")
                    int size) {
        return telas.listaPautas(pautaService.listar(page, size));
    }

    @GetMapping("/pautas/nova")
    @Operation(summary = "Formulário de cadastro de pauta")
    public Tela formularioNovaPauta() {
        return telas.novaPauta();
    }

    @PostMapping("/pautas")
    @Operation(summary = "Cadastra a pauta e devolve a tela da pauta criada")
    public Tela criarPauta(@RequestBody AcaoTelaRequest acao) {
        var pauta =
                pautaService.criar(
                        acao.texto(TelaFactory.CAMPO_TITULO),
                        acao.texto(TelaFactory.CAMPO_DESCRICAO));

        return telas.pautaSemSessao(pauta, appProperties.sessao().duracaoPadraoMinutos());
    }

    @GetMapping("/pautas/{id}")
    @Operation(summary = "Tela da pauta, variando conforme o estado da sessão")
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

    @PostMapping("/pautas/{id}/sessao")
    @Operation(summary = "Abre a sessão e devolve a tela de votação")
    public Tela abrirSessao(@PathVariable UUID id, @RequestBody AcaoTelaRequest acao) {
        var sessao = sessaoService.abrir(id, acao.inteiro(TelaFactory.CAMPO_DURACAO));
        return telas.identificacao(sessao, clock.instant());
    }

    @PostMapping("/pautas/{id}/votos/identificacao")
    @Operation(summary = "Valida o CPF e devolve as opções de voto")
    public Tela identificar(@PathVariable UUID id, @RequestBody AcaoTelaRequest acao) {
        var pauta = pautaService.buscar(id);
        var sessao = sessaoService.buscarObrigatoria(id);

        if (!sessao.estaAberta(clock.instant())) {
            throw new SessaoEncerradaException(id);
        }

        var cpf = Cpf.de(acao.texto(TelaFactory.CAMPO_CPF));

        if (votoService.jaVotou(id, cpf)) {
            return telas.erro(pauta.getTitulo(), "Você já registrou seu voto nesta pauta.");
        }

        return telas.opcoesDeVoto(pauta, cpf.numero());
    }

    @PostMapping("/pautas/{id}/votos")
    @Operation(summary = "Registra o voto e devolve a tela de resultado")
    public Tela votar(@PathVariable UUID id, @RequestBody AcaoTelaRequest acao) {
        var cpf = Cpf.de(acao.texto(TelaFactory.CAMPO_CPF));
        var opcao = OpcaoVoto.valueOf(acao.texto("opcao").toUpperCase());

        votoService.registrar(id, cpf, opcao);
        return telas.resultado(resultadoService.apurar(id));
    }

    @GetMapping("/pautas/{id}/resultado")
    @Operation(summary = "Tela de resultado da apuração")
    public Tela resultado(@PathVariable UUID id) {
        return telas.resultado(resultadoService.apurar(id));
    }
}
