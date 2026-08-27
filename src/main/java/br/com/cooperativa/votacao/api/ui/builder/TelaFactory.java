package br.com.cooperativa.votacao.api.ui.builder;

import br.com.cooperativa.votacao.api.ui.dto.Botao;
import br.com.cooperativa.votacao.api.ui.dto.ItemSelecao;
import br.com.cooperativa.votacao.api.ui.dto.ItemTela;
import br.com.cooperativa.votacao.api.ui.dto.Tela;
import br.com.cooperativa.votacao.domain.model.Pagina;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelaFactory {

    public static final String CAMPO_DURACAO = "duracaoMinutos";

    public static final String CAMPO_CPF = "cpf";

    public static final String CAMPO_TITULO = "titulo";

    public static final String CAMPO_DESCRICAO = "descricao";

    private final UrlTelaFactory urls;

    /**
     * Menu inicial da aplicação.
     *
     * @return tela SELECAO com as ações disponíveis
     */
    public Tela menu() {
        return Tela.selecao(
                "Assembleia Cooperativa",
                List.of(
                        ItemSelecao.navegacao("Pautas em deliberacao", urls.listaPautas()),
                        ItemSelecao.navegacao("Cadastrar nova pauta", urls.novaPauta())));
    }

    /**
     * Formulário de cadastro de pauta.
     *
     * @return tela FORMULARIO com os campos de título e descrição
     */
    public Tela novaPauta() {
        return Tela.formulario(
                "Nova pauta",
                List.of(
                        ItemTela.texto("Informe o assunto que sera submetido a assembleia."),
                        ItemTela.inputTexto(CAMPO_TITULO, "Titulo da pauta", null),
                        ItemTela.inputTexto(CAMPO_DESCRICAO, "Descricao", null)),
                Botao.de("Cadastrar", urls.criarPauta(), Map.of()),
                Botao.navegacao("Cancelar", urls.menu()));
    }

    /**
     * Lista de pautas como tela de seleção.
     *
     * @param pautas pagina de pautas a exibir
     * @return tela SELECAO com uma opção por pauta
     */
    public Tela listaPautas(Pagina<Pauta> pautas) {
        if (pautas.vazia()) {
            // SELECAO vazia deixaria o usuário sem saída. Uso FORMULARIO pra ter botão.
            return Tela.formulario(
                    "Pautas em deliberacao",
                    List.of(ItemTela.texto("Nenhuma pauta cadastrada ate o momento.")),
                    Botao.navegacao("Cadastrar nova pauta", urls.novaPauta()),
                    Botao.navegacao("Voltar", urls.menu()));
        }

        var itens =
                pautas.conteudo().stream()
                        .map(p -> ItemSelecao.navegacao(p.getTitulo(), urls.pauta(p.getId())))
                        .toList();

        return Tela.selecao("Pautas em deliberacao", itens);
    }

    /**
     * Tela de uma pauta que ainda não teve sessão aberta.
     *
     * @param pauta pauta em questão
     * @param duracaoPadraoMinutos duração sugerida no campo, vinda de configuração
     * @return tela FORMULARIO que permite abrir a sessão
     */
    public Tela pautaSemSessao(Pauta pauta, int duracaoPadraoMinutos) {
        var itens = new ArrayList<ItemTela>();
        adicionarDescricao(itens, pauta);
        itens.add(ItemTela.texto("Situacao: nenhuma sessao de votacao foi aberta."));
        itens.add(
                ItemTela.inputNumero(
                        CAMPO_DURACAO, "Duracao da sessao (minutos)", duracaoPadraoMinutos));

        return Tela.formulario(
                pauta.getTitulo(),
                itens,
                Botao.de("Abrir sessao", urls.abrirSessao(pauta.getId()), Map.of()),
                Botao.navegacao("Voltar", urls.listaPautas()));
    }

    /**
     * Tela de identificação do associado, para uma pauta com sessão aberta.
     *
     * @param sessão sessão aberta
     * @param agora instante de referência, para calcular o tempo restante
     * @return tela FORMULARIO que coleta o CPF
     */
    public Tela identificacao(SessaoVotacao sessao, Instant agora) {
        var pauta = sessao.getPauta();
        var itens = new ArrayList<ItemTela>();
        adicionarDescricao(itens, pauta);
        itens.add(ItemTela.texto("Sessao aberta. " + tempoRestante(sessao.tempoRestante(agora))));
        itens.add(ItemTela.inputTexto(CAMPO_CPF, "Seu CPF", null));

        return Tela.formulario(
                pauta.getTitulo(),
                itens,
                Botao.de("Continuar", urls.identificacao(pauta.getId()), Map.of()),
                Botao.navegacao("Voltar", urls.listaPautas()));
    }

    /**
     * Tela de escolha do voto.
     *
     * @param pauta pauta em votação
     * @param cpf CPF já validado, propagado no corpo de cada opção
     * @return tela SELECAO com as opções Sim e Não
     */
    public Tela opcoesDeVoto(Pauta pauta, String cpf) {
        var url = urls.votar(pauta.getId());
        return Tela.selecao(
                pauta.getTitulo(),
                List.of(
                        ItemSelecao.de("Sim", url, Map.of(CAMPO_CPF, cpf, "opcao", "SIM")),
                        ItemSelecao.de("Nao", url, Map.of(CAMPO_CPF, cpf, "opcao", "NAO"))));
    }

    /**
     * Tela de resultado da apuração.
     *
     * @param resultado resultado apurado
     * @return tela FORMULARIO com os números e o desfecho
     */
    public Tela resultado(ResultadoVotacao resultado) {
        var itens =
                List.of(
                        ItemTela.texto(
                                resultado.parcial()
                                        ? "Apuracao parcial: a sessao ainda esta aberta."
                                        : "Sessao encerrada. Resultado final."),
                        ItemTela.texto("Sim: %d voto(s)".formatted(resultado.votosSim())),
                        ItemTela.texto("Nao: %d voto(s)".formatted(resultado.votosNao())),
                        ItemTela.texto("Total: %d voto(s)".formatted(resultado.totalVotos())),
                        ItemTela.texto("Resultado: " + resultado.resultado().getDescricao()));

        return Tela.formulario(
                resultado.titulo(),
                itens,
                Botao.navegacao("Atualizar", urls.resultado(resultado.pautaId())),
                Botao.navegacao("Voltar", urls.listaPautas()));
    }

    /**
     * Tela usada para comunicar um erro de negócio ao cliente.
     *
     * @param titulo titulo da tela de erro
     * @param mensagem explicação apresentada ao usuário
     * @return tela FORMULARIO com a mensagem e um botão de retorno
     */
    public Tela erro(String titulo, String mensagem) {
        return Tela.formulario(
                titulo,
                List.of(ItemTela.texto(mensagem)),
                Botao.navegacao("Voltar", urls.listaPautas()),
                null);
    }

    /**
     * Acrescenta a descrição da pauta, quando houver.
     *
     * @param itens lista em construção
     * @param pauta pauta de origem
     */
    private void adicionarDescricao(List<ItemTela> itens, Pauta pauta) {
        if (pauta.getDescricao() != null && !pauta.getDescricao().isBlank()) {
            itens.add(ItemTela.texto(pauta.getDescricao()));
        }
    }

    /**
     * Formata o tempo restante de forma legível para o associado.
     *
     * @param restante duração restante da sessão
     * @return frase com minutos e segundos restantes
     */
    private String tempoRestante(Duration restante) {
        if (restante.isZero()) {
            return "A votacao foi encerrada.";
        }
        var minutos = restante.toMinutes();
        var segundos = restante.toSecondsPart();
        return minutos > 0
                ? "Restam %d min %d s para votar.".formatted(minutos, segundos)
                : "Restam %d s para votar.".formatted(segundos);
    }
}
