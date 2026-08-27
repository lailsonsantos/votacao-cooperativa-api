package br.com.cooperativa.votacao.api.ui.builder;

import br.com.cooperativa.votacao.api.ui.dto.Botao;
import br.com.cooperativa.votacao.api.ui.dto.ItemSelecao;
import br.com.cooperativa.votacao.api.ui.dto.ItemTela;
import br.com.cooperativa.votacao.api.ui.dto.Tela;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Traduz o estado do dominio nas telas do Anexo 1.
 *
 * <p>Esta e a unica classe que conhece, ao mesmo tempo, o dominio e o vocabulario
 * de telas do cliente. Ela nao contem regra de negocio: apenas decide qual tela
 * representa cada estado. Manter a traducao concentrada aqui e o que permite
 * alterar a experiencia do cliente sem tocar em servico, dominio ou banco
 * &mdash; que e exatamente o beneficio que o padrao Server-Driven UI existe para
 * entregar.
 */
@Component
@RequiredArgsConstructor
public class TelaFactory {
    /** Chave do campo de duracao no formulario de abertura de sessao. */
    public static final String CAMPO_DURACAO = "duracaoMinutos";

    /** Chave do campo de CPF no formulario de identificacao. */
    public static final String CAMPO_CPF = "cpf";

    /** Chave do campo de titulo no formulario de cadastro de pauta. */
    public static final String CAMPO_TITULO = "titulo";

    /** Chave do campo de descricao no formulario de cadastro de pauta. */
    public static final String CAMPO_DESCRICAO = "descricao";

    private final UrlTelaFactory urls;

    /**
     * Menu inicial da aplicacao.
     *
     * @return tela SELECAO com as acoes disponiveis
     */
    public Tela menu() {
        return Tela.selecao(
                "Assembleia Cooperativa",
                List.of(
                        ItemSelecao.navegacao("Pautas em deliberacao", urls.listaPautas()),
                        ItemSelecao.navegacao("Cadastrar nova pauta", urls.novaPauta())));
    }

    /**
     * Formulario de cadastro de pauta.
     *
     * @return tela FORMULARIO com os campos de titulo e descricao
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
     * Lista de pautas como tela de selecao.
     *
     * @param pautas pagina de pautas a exibir
     * @return tela SELECAO com uma opcao por pauta
     */
    public Tela listaPautas(Page<Pauta> pautas) {
        if (pautas.isEmpty()) {
            // Uma SELECAO vazia deixaria o usuario numa tela sem saida. Um
            // FORMULARIO com texto e botao de volta comunica o estado e oferece
            // um caminho adiante.
            return Tela.formulario(
                    "Pautas em deliberacao",
                    List.of(ItemTela.texto("Nenhuma pauta cadastrada ate o momento.")),
                    Botao.navegacao("Cadastrar nova pauta", urls.novaPauta()),
                    Botao.navegacao("Voltar", urls.menu()));
        }

        var itens =
                pautas.getContent().stream()
                        .map(p -> ItemSelecao.navegacao(p.getTitulo(), urls.pauta(p.getId())))
                        .toList();

        return Tela.selecao("Pautas em deliberacao", itens);
    }

    /**
     * Tela de uma pauta que ainda nao teve sessao aberta.
     *
     * @param pauta                pauta em questao
     * @param duracaoPadraoMinutos duracao sugerida no campo, vinda de configuracao
     * @return tela FORMULARIO que permite abrir a sessao
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
     * Tela de identificacao do associado, para uma pauta com sessao aberta.
     *
     * <p>A votacao acontece em dois passos. A tela FORMULARIO oferece apenas
     * {@code botaoOk} e {@code botaoCancelar}, e mapear "Nao" em "Cancelar" seria
     * semanticamente errado: cancelar nao pode registrar voto. Coletar o CPF aqui
     * e oferecer as opcoes em uma tela SELECAO respeita o vocabulario do Anexo 1
     * e ainda permite validar o CPF antes de apresentar as opcoes.
     *
     * @param sessao sessao aberta
     * @param agora  instante de referencia, para calcular o tempo restante
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
     * @param pauta pauta em votacao
     * @param cpf   CPF ja validado, propagado no corpo de cada opcao
     * @return tela SELECAO com as opcoes Sim e Nao
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
     * Tela de resultado da apuracao.
     *
     * @param resultado resultado apurado
     * @return tela FORMULARIO com os numeros e o desfecho
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
                        ItemTela.texto("Resultado: " + descrever(resultado)));

        return Tela.formulario(
                resultado.titulo(),
                itens,
                Botao.navegacao("Atualizar", urls.resultado(resultado.pautaId())),
                Botao.navegacao("Voltar", urls.listaPautas()));
    }

    /**
     * Tela usada para comunicar um erro de negocio ao cliente.
     *
     * <p>Um {@code 409} cru quebraria a experiencia: o cliente do Anexo 1 sabe
     * renderizar telas, nao codigos de status. O erro vira uma tela legivel com
     * caminho de volta, enquanto a API REST continua devolvendo o status correto
     * para quem a consome como API.
     *
     * @param titulo   titulo da tela de erro
     * @param mensagem explicacao apresentada ao usuario
     * @return tela FORMULARIO com a mensagem e um botao de retorno
     */
    public Tela erro(String titulo, String mensagem) {
        return Tela.formulario(
                titulo,
                List.of(ItemTela.texto(mensagem)),
                Botao.navegacao("Voltar", urls.listaPautas()),
                null);
    }

    /**
     * Acrescenta a descricao da pauta, quando houver.
     *
     * @param itens lista em construcao
     * @param pauta pauta de origem
     */
    private void adicionarDescricao(List<ItemTela> itens, Pauta pauta) {
        if (pauta.getDescricao() != null && !pauta.getDescricao().isBlank()) {
            itens.add(ItemTela.texto(pauta.getDescricao()));
        }
    }

    /**
     * Descreve o desfecho da apuracao em linguagem natural.
     *
     * @param resultado resultado apurado
     * @return frase correspondente ao desfecho
     */
    private String descrever(ResultadoVotacao resultado) {
        return switch (resultado.resultado()) {
            case APROVADA -> "APROVADA";
            case REPROVADA -> "REPROVADA";
            case EMPATE -> "EMPATE";
            case SEM_VOTOS -> "Nenhum voto registrado";
        };
    }

    /**
     * Formata o tempo restante de forma legivel para o associado.
     *
     * @param restante duracao restante da sessao
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
