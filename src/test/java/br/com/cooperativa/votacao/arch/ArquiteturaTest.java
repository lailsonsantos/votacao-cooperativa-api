package br.com.cooperativa.votacao.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Regras de arquitetura verificadas automaticamente.
 *
 * <p>Documento nao impede ninguem de importar a classe errada; teste impede.
 * Estas regras fazem o build falhar quando a direcao das dependencias e
 * invertida, o que e a forma mais barata de manter a arquitetura viva depois que
 * o projeto cresce.
 */
@AnalyzeClasses(
        packages = "br.com.cooperativa.votacao",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    /** As camadas so podem ser acessadas de cima para baixo. */
    @ArchTest
    static final ArchRule camadas =
            Architectures.layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Api").definedBy("..api..")
                    .layer("Aplicacao").definedBy("..application..")
                    .layer("Dominio").definedBy("..domain..")
                    .layer("Infraestrutura").definedBy("..infrastructure..")
                    .whereLayer("Api").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Aplicacao").mayOnlyBeAccessedByLayers("Api")
                    .whereLayer("Infraestrutura").mayOnlyBeAccessedByLayers("Aplicacao", "Api");

    /**
     * O dominio nao pode depender do framework web.
     *
     * <p>Se uma regra de negocio precisar de {@code HttpServletRequest} para
     * funcionar, ela deixou de ser uma regra de negocio.
     */
    @ArchTest
    static final ArchRule dominioIndependenteDeWeb =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.web..",
                            "jakarta.servlet..",
                            "org.springframework.stereotype..");

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
                    .callMethod(System.class, "currentTimeMillis")
                    .orShould()
                    .accessField(System.class, "out")
                    .orShould()
                    .accessField(System.class, "err")
                    .because("o log deve passar por SLF4J e pelo mascaramento de CPF");
}
