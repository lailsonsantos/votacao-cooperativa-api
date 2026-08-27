package br.com.cooperativa.votacao.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/** Regras de arquitetura verificadas automaticamente. */
@AnalyzeClasses(
        packages = "br.com.cooperativa.votacao",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    /** As camadas só podem ser acessadas de fora para dentro. */
    @ArchTest
    static final ArchRule camadas =
            Architectures.layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Api")
                    .definedBy("..api..")
                    .layer("Aplicacao")
                    .definedBy("..application..")
                    .layer("Dominio")
                    .definedBy("..domain..")
                    .layer("Infraestrutura")
                    .definedBy("..infrastructure..")
                    .whereLayer("Api")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Infraestrutura")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Aplicacao")
                    .mayOnlyBeAccessedByLayers("Api", "Infraestrutura");

    /** O domínio não depende de nenhum framework de aplicação. */
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

    /** A camada de aplicação não conhece implementações de integração. */
    @ArchTest
    static final ArchRule aplicacaoDependeDeAbstracoes =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("a dependencia deve apontar para a porta, nao para o adaptador");

    /** A infraestrutura enxerga apenas as portas, nunca os casos de uso. */
    @ArchTest
    static final ArchRule infraestruturaSoEnxergaPortas =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("br.com.cooperativa.votacao.application")
                    .because("o adaptador implementa a porta; nao consome caso de uso");

    /** Ninguém depende de uma implementação de caso de uso. */
    @ArchTest
    static final ArchRule ninguemDependeDeImplementacao =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..application.impl..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..application.impl..")
                    .because("o consumidor depende da abstracao, nao da implementacao");

    /** Nenhuma regra de negócio mora em controlador. */
    @ArchTest
    static final ArchRule controladoresNaoAcessamRepositorios =
            noClasses()
                    .that()
                    .resideInAPackage("..api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..domain.repository..")
                    .because("o acesso a dados pertence a camada de aplicacao");

    /** Nenhum ciclo entre os pacotes de primeiro nível. */
    @ArchTest
    static final ArchRule semCiclos =
            SlicesRuleDefinition.slices()
                    .matching("br.com.cooperativa.votacao.(*)..")
                    .should()
                    .beFreeOfCycles();

    /** Log e feito por SLF4J, nunca por {@code System.out}. */
    @ArchTest
    static final ArchRule semSystemOut =
            noClasses()
                    .should()
                    .accessField(System.class, "out")
                    .orShould()
                    .accessField(System.class, "err")
                    .because("o log deve passar por SLF4J e pelo mascaramento de CPF");
}
