package br.com.cooperativa.votacao.api.ui;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cooperativa.votacao.api.ui.builder.TelaFactory;
import br.com.cooperativa.votacao.api.ui.builder.UrlTelaFactory;
import br.com.cooperativa.votacao.api.ui.dto.ItemSelecao;
import br.com.cooperativa.votacao.api.ui.dto.ItemTela;
import br.com.cooperativa.votacao.api.ui.dto.TipoItem;
import br.com.cooperativa.votacao.api.ui.dto.TipoTela;
import br.com.cooperativa.votacao.config.AppProperties;
import br.com.cooperativa.votacao.domain.model.Pagina;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.model.ResultadoVotacao;
import br.com.cooperativa.votacao.domain.model.SessaoVotacao;
import br.com.cooperativa.votacao.domain.model.StatusSessao;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TelaFactory")
class TelaFactoryTest {

    private static final Instant AGORA = Instant.parse("2026-08-27T14:00:00Z");
    private static final String BASE = "http://localhost:8080";

    private final TelaFactory telas =
            new TelaFactory(
                    new UrlTelaFactory(
                            new AppProperties(
                                    new AppProperties.Callback(BASE),
                                    new AppProperties.Sessao(1))));

    private final Pauta pauta = Pauta.criar("Reforma do estatuto", "Artigos 12 a 18.", AGORA);

    @Nested
    @DisplayName("navegacao")
    class Navegacao {

        @Test
        @DisplayName("menu oferece as duas acoes iniciais, com URLs absolutas")
        void menu() {
            var tela = telas.menu();

            assertThat(tela.tipo()).isEqualTo(TipoTela.SELECAO);
            assertThat(tela.itens()).hasSize(2);
            assertThat(tela.itens())
                    .allSatisfy(i -> assertThat(((ItemSelecao) i).url()).startsWith(BASE));
            // SELECAO nao carrega botoes: campos nulos somem na serializacao.
            assertThat(tela.botaoOk()).isNull();
        }

        @Test
        @DisplayName("formulario de nova pauta pede titulo e descricao")
        void novaPauta() {
            var tela = telas.novaPauta();

            assertThat(tela.tipo()).isEqualTo(TipoTela.FORMULARIO);
            assertThat(tela.itens())
                    .filteredOn(i -> ((ItemTela) i).tipo() == TipoItem.INPUT_TEXTO)
                    .extracting(i -> ((ItemTela) i).id())
                    .containsExactly(TelaFactory.CAMPO_TITULO, TelaFactory.CAMPO_DESCRICAO);
            assertThat(tela.botaoOk().body()).isEmpty();
            assertThat(tela.botaoCancelar().texto()).isEqualTo("Cancelar");
        }
    }

    @Nested
    @DisplayName("lista de pautas")
    class Lista {

        @Test
        @DisplayName("uma opcao por pauta, cada uma levando ao proprio detalhe")
        void comPautas() {
            var outra = Pauta.criar("Aquisicao de sede", null, AGORA);
            var tela = telas.listaPautas(new Pagina<>(List.of(pauta, outra), 0, 20, 2));

            assertThat(tela.tipo()).isEqualTo(TipoTela.SELECAO);
            assertThat(tela.itens())
                    .extracting(i -> ((ItemSelecao) i).texto())
                    .containsExactly("Reforma do estatuto", "Aquisicao de sede");
            // Item sem body e navegacao: o cliente faz GET, nao POST.
            assertThat(tela.itens()).allSatisfy(i -> assertThat(((ItemSelecao) i).body()).isNull());
        }

        @Test
        @DisplayName("lista vazia vira FORMULARIO com saida, e nao SELECAO sem itens")
        void semPautas() {
            var tela = telas.listaPautas(new Pagina<>(List.of(), 0, 20, 0));

            // Uma SELECAO vazia deixaria o usuario numa tela sem nenhuma acao.
            assertThat(tela.tipo()).isEqualTo(TipoTela.FORMULARIO);
            assertThat(tela.botaoOk().texto()).isEqualTo("Cadastrar nova pauta");
            assertThat(tela.botaoCancelar()).isNotNull();
        }
    }

    @Nested
    @DisplayName("estados da pauta")
    class Estados {

        @Test
        @DisplayName("sem sessao, sugere a duracao padrao vinda de configuracao")
        void semSessao() {
            var tela = telas.pautaSemSessao(pauta, 1);

            assertThat(tela.titulo()).isEqualTo("Reforma do estatuto");
            assertThat(tela.itens())
                    .filteredOn(i -> ((ItemTela) i).tipo() == TipoItem.INPUT_NUMERO)
                    .singleElement()
                    .satisfies(
                            i -> {
                                assertThat(((ItemTela) i).id())
                                        .isEqualTo(TelaFactory.CAMPO_DURACAO);
                                assertThat(((ItemTela) i).valor()).isEqualTo(1);
                            });
            assertThat(tela.botaoOk().url()).endsWith("/sessao");
        }

        @Test
        @DisplayName("pauta sem descricao nao gera item de texto vazio")
        void semDescricao() {
            var semTexto = Pauta.criar("So o titulo", null, AGORA);

            var tela = telas.pautaSemSessao(semTexto, 1);

            assertThat(tela.itens())
                    .filteredOn(i -> ((ItemTela) i).tipo() == TipoItem.TEXTO)
                    .hasSize(1)
                    .allSatisfy(i -> assertThat(((ItemTela) i).texto()).contains("Situacao"));
        }

        @Test
        @DisplayName("descricao em branco nao vira item de texto vazio")
        void descricaoEmBranco() {
            // Nulo e "   " precisam ter o mesmo efeito: um item TEXTO vazio
            // apareceria na tela do associado como uma linha em branco.
            var comEspacos = Pauta.criar("So o titulo", "   ", AGORA);

            var tela = telas.pautaSemSessao(comEspacos, 1);

            assertThat(tela.itens())
                    .filteredOn(i -> ((ItemTela) i).tipo() == TipoItem.TEXTO)
                    .hasSize(1)
                    .allSatisfy(i -> assertThat(((ItemTela) i).texto()).contains("Situacao"));
        }

        @Test
        @DisplayName("sessao aberta coleta o CPF e informa o tempo restante")
        void identificacao() {
            var sessao = SessaoVotacao.abrir(pauta, AGORA, Duration.ofMinutes(5));

            var tela = telas.identificacao(sessao, AGORA.plusSeconds(90));

            assertThat(tela.tipo()).isEqualTo(TipoTela.FORMULARIO);
            assertThat(tela.itens())
                    .filteredOn(i -> ((ItemTela) i).tipo() == TipoItem.INPUT_TEXTO)
                    .singleElement()
                    .satisfies(
                            i -> assertThat(((ItemTela) i).id()).isEqualTo(TelaFactory.CAMPO_CPF));
            assertThat(tela.itens())
                    .extracting(i -> ((ItemTela) i).texto())
                    .anySatisfy(t -> assertThat(t).contains("Restam 3 min 30 s"));
        }

        @Test
        @DisplayName("abaixo de um minuto, informa apenas os segundos")
        void identificacaoUltimosSegundos() {
            var sessao = SessaoVotacao.abrir(pauta, AGORA, Duration.ofMinutes(1));

            var tela = telas.identificacao(sessao, AGORA.plusSeconds(40));

            assertThat(tela.itens())
                    .extracting(i -> ((ItemTela) i).texto())
                    .anySatisfy(t -> assertThat(t).contains("Restam 20 s"));
        }

        @Test
        @DisplayName("com a sessao ja encerrada, informa o encerramento")
        void identificacaoAposFechamento() {
            var sessao = SessaoVotacao.abrir(pauta, AGORA, Duration.ofMinutes(1));

            var tela = telas.identificacao(sessao, AGORA.plusSeconds(120));

            assertThat(tela.itens())
                    .extracting(i -> ((ItemTela) i).texto())
                    .anySatisfy(t -> assertThat(t).contains("encerrada"));
        }
    }

    @Nested
    @DisplayName("voto e resultado")
    class VotoResultado {

        @Test
        @DisplayName("opcoes de voto levam o CPF no corpo de cada item")
        void opcoesDeVoto() {
            var tela = telas.opcoesDeVoto(pauta, "19839091069");

            assertThat(tela.tipo()).isEqualTo(TipoTela.SELECAO);
            assertThat(tela.itens())
                    .extracting(i -> ((ItemSelecao) i).texto())
                    .containsExactly("Sim", "Nao");
            // O cliente apenas reenvia o body; e o servidor quem define o conteudo.
            assertThat(tela.itens())
                    .allSatisfy(
                            i ->
                                    assertThat(((ItemSelecao) i).body())
                                            .containsEntry(TelaFactory.CAMPO_CPF, "19839091069"));
        }

        @Test
        @DisplayName("resultado parcial avisa que a sessao segue aberta")
        void resultadoParcial() {
            var r =
                    ResultadoVotacao.de(
                            pauta.getId(), "Reforma do estatuto", StatusSessao.ABERTA, 3, 1);

            var tela = telas.resultado(r);

            assertThat(tela.itens())
                    .extracting(i -> ((ItemTela) i).texto())
                    .anySatisfy(t -> assertThat(t).contains("parcial"))
                    .anySatisfy(t -> assertThat(t).contains("Sim: 3"))
                    .anySatisfy(t -> assertThat(t).contains("APROVADA"));
            // Botao sem body: atualizar e navegacao, nao acao.
            assertThat(tela.botaoOk().body()).isNull();
        }

        @Test
        @DisplayName("descreve cada desfecho possivel")
        void desfechos() {
            record Caso(long sim, long nao, String esperado) {}
            List.of(
                            new Caso(3, 1, "APROVADA"),
                            new Caso(1, 3, "REPROVADA"),
                            new Caso(2, 2, "EMPATE"),
                            new Caso(0, 0, "Nenhum voto registrado"))
                    .forEach(
                            caso -> {
                                var r =
                                        ResultadoVotacao.de(
                                                pauta.getId(),
                                                "P",
                                                StatusSessao.FECHADA,
                                                caso.sim(),
                                                caso.nao());
                                assertThat(telas.resultado(r).itens())
                                        .extracting(i -> ((ItemTela) i).texto())
                                        .anySatisfy(t -> assertThat(t).contains(caso.esperado()));
                            });
        }

        @Test
        @DisplayName("tela de erro tem mensagem e caminho de volta")
        void erro() {
            var tela = telas.erro("Voto duplicado", "Voce ja votou nesta pauta.");

            assertThat(tela.tipo()).isEqualTo(TipoTela.FORMULARIO);
            assertThat(tela.titulo()).isEqualTo("Voto duplicado");
            assertThat(tela.botaoOk().texto()).isEqualTo("Voltar");
            // Sem body: voltar nunca envia dados.
            assertThat(tela.botaoOk().body()).isNull();
            assertThat(tela.botaoCancelar()).isNull();
        }
    }

    @Test
    @DisplayName("tela sem itens nao quebra na copia defensiva")
    void telaSemItens() {
        // List.copyOf recusa nulo, entao o caso e tratado antes. O campo nulo e
        // significativo: ele some da serializacao, como o Anexo 1 exige.
        var tela =
                new br.com.cooperativa.votacao.api.ui.dto.Tela(
                        TipoTela.SELECAO, "Sem itens", null, null, null);

        assertThat(tela.itens()).isNull();
    }

    @Test
    @DisplayName("itens da tela sao imutaveis")
    void itensImutaveis() {
        var tela = telas.menu();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> ((java.util.List<Object>) tela.itens()).add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("normaliza barra final na URL base, evitando barra dupla")
    void baseUrlComBarraFinal() {
        var comBarra =
                new TelaFactory(
                        new UrlTelaFactory(
                                new AppProperties(
                                        new AppProperties.Callback("http://host:8080/"),
                                        new AppProperties.Sessao(1))));

        assertThat(((ItemSelecao) comBarra.menu().itens().get(0)).url())
                .doesNotContain("//api")
                .startsWith("http://host:8080/api/v1/telas");
    }
}
