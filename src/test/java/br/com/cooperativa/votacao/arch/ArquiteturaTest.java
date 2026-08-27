package br.com.cooperativa.votacao.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Regras de arquitetura verificadas automaticamente.
 *
 * <p>Documento nao impede ninguem de importar a classe errada; teste impede.
 * Estas regras fazem o build falhar quando a direcao das dependencias e
 * invertida, que e a forma mais barata de manter a arquitetura viva depois que o
 * projeto cresce.
 *
 * <p>Elas tambem servem de contraprova do que o README afirma: qualquer pessoa
 * pode conferir que a independencia do dominio e um fato verificado, e nao uma
 * intencao declarada.
 */
@AnalyzeClasses(
        packages = "br.com.cooperativa.votacao",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    /**
     * As camadas so podem ser acessadas de fora para dentro.
     *
     * <p>A infraestrutura nao e acessada por ninguem: ela <em>implementa</em>
     * portas declaradas nas camadas internas, e o Spring faz a ligacao em tempo
     * de execucao. E esse detalhe que caracteriza a inversao de dependencia
     * &mdash; sem ele, a aplicacao chamaria a integracao concreta e a seta
     * apontaria para o lado errado.
     *
     * <p>A camada de aplicacao aparece como acessivel pela infraestrutura
     * justamente por isso: o adaptador precisa enxergar a porta que implementa.
     * A regra {@link #infraestruturaSoEnxergaPortas} restringe esse acesso ao
     * pacote de portas, impedindo que um adaptador chame um caso de uso.
     */
    @ArchTest
    static final ArchRule camadas =
            Architectures.layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Api").definedBy("..api..")
                    .layer("Aplicacao").definedBy("..application..")
                    .layer("Dominio").definedBy("..domain..")
                    .layer("Infraestrutura").definedBy("..infrastructure..")
                    .whereLayer("Api").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Infraestrutura").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Aplicacao").mayOnlyBeAccessedByLayers("Api", "Infraestrutura");

    /**
     * O dominio nao depende de nenhum framework de aplicacao.
     *
     * <p>Inclui {@code org.springframework.http}: HTTP e transporte, e uma regra
     * de negocio exposta por mensageria continuaria valendo sem status algum.
     * Foi exatamente por essa brecha que {@code HttpStatus} chegou a viver nas
     * excecoes de dominio; a regra existe para que nao volte.
     *
     * <p>As anotacoes de persistencia e de validacao seguem permitidas: sao
     * especificacoes declarativas, sem modelo de programacao proprio, e a
     * decisao esta registrada em {@code docs/adr/0001}.
     */
    @ArchTest
    static final ArchRule dominioIndependenteDeFramework =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.servlet..",
                            "io.swagger..",
                            "com.fasterxml.jackson..")
                    .because(
                            "o dominio precisa poder ser exposto por outro transporte e"
                                    + " persistido de outra forma sem alteracao");

    /**
     * A camada de aplicacao nao conhece implementacoes de integracao.
     *
     * <p>Ela depende de portas declaradas em {@code application.port}, nunca do
     * cliente HTTP que hoje as satisfaz. Substituir o servico externo por um
     * cadastro proprio nao pode exigir mudanca em regra de negocio.
     */
    @ArchTest
    static final ArchRule aplicacaoDependeDeAbstracoes =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("a dependencia deve apontar para a porta, nao para o adaptador");

    /**
     * A infraestrutura enxerga apenas as portas, nunca os casos de uso.
     *
     * <p>Sem esta regra, o acesso liberado no teste de camadas permitiria que um
     * adaptador chamasse um servico de aplicacao, criando um caminho de volta
     * que anularia a inversao que as portas existem para garantir.
     */
    @ArchTest
    static final ArchRule infraestruturaSoEnxergaPortas =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("br.com.cooperativa.votacao.application")
                    .because("o adaptador implementa a porta; nao consome caso de uso");

    /**
     * Nenhuma regra de negocio mora em controlador.
     *
     * <p>E o que permite que as duas superficies HTTP compartilhem o mesmo
     * nucleo, e o que tornaria barata a criacao de uma {@code /api/v2}: apenas a
     * camada de API seria duplicada.
     */
    @ArchTest
    static final ArchRule controladoresNaoAcessamRepositorios =
            noClasses()
                    .that()
                    .resideInAPackage("..api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..domain.repository..")
                    .because("o acesso a dados pertence a camada de aplicacao");

    /** Nenhum ciclo entre os pacotes de primeiro nivel. */
    @ArchTest
    static final ArchRule semCiclos =
            SlicesRuleDefinition.slices()
                    .matching("br.com.cooperativa.votacao.(*)..")
                    .should()
                    .beFreeOfCycles();

    /**
     * Log e feito por SLF4J, nunca por {@code System.out}.
     *
     * <p>{@code System.out} escapa da configuracao de nivel, do formato JSON e do
     * mascaramento de CPF &mdash; ou seja, escapa de tudo que faz o log ser util
     * e seguro em producao.
     */
    @ArchTest
    static final ArchRule semSystemOut =
            noClasses()
                    .should()
                    .accessField(System.class, "out")
                    .orShould()
                    .accessField(System.class, "err")
                    .because("o log deve passar por SLF4J e pelo mascaramento de CPF");
}
