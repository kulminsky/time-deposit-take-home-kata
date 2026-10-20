package org.ikigaidigital;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Hexagonal architecture rules for the production code.
 * <ul>
 *     <li>domain: the legacy {@link TimeDeposit} and {@link TimeDepositCalculator} (they stay in the root package)
 *     and {@code org.ikigaidigital.domain..}</li>
 *     <li>application: use cases (port.in), ports to the outside (port.out) and services</li>
 *     <li>adapters: web (adapter.in.web) and persistence (adapter.out.persistence)</li>
 *     <li>config and the Spring Boot main class wire everything together</li>
 * </ul>
 */
@AnalyzeClasses(packages = "org.ikigaidigital", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final DescribedPredicate<JavaClass> DOMAIN =
        resideInAPackage("org.ikigaidigital.domain..")
            .or(belongToAnyOf(TimeDeposit.class, TimeDepositCalculator.class))
            .as("domain classes");

    @ArchTest
    static final ArchRule domainDependsOnlyOnTheJdk =
        classes().that(DOMAIN)
            .should().onlyDependOnClassesThat(resideInAPackage("java..").or(DOMAIN));

    @ArchTest
    static final ArchRule applicationDependsOnlyOnTheDomainAndTransactionAnnotations =
        classes().that().resideInAPackage("org.ikigaidigital.application..")
            .should().onlyDependOnClassesThat(
                resideInAnyPackage("java..", "org.ikigaidigital.application..", "org.springframework.transaction.annotation..")
                    .or(DOMAIN));

    @ArchTest
    static final ArchRule nothingOutsideTheAdaptersDependsOnThem =
        noClasses().that().resideOutsideOfPackage("org.ikigaidigital.adapter..")
            .should().dependOnClassesThat().resideInAPackage("org.ikigaidigital.adapter..");

    @ArchTest
    static final ArchRule adaptersDoNotDependOnEachOther =
        slices().matching("org.ikigaidigital.adapter.(*).(*)..")
            .should().notDependOnEachOther();

    @ArchTest
    static final ArchRule webAdapterOnlyUsesUseCases =
        noClasses().that().resideInAPackage("org.ikigaidigital.adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.ikigaidigital.application.service..", "org.ikigaidigital.application.port.out..");

    @ArchTest
    static final ArchRule persistenceAdapterOnlyImplementsOutboundPorts =
        noClasses().that().resideInAPackage("org.ikigaidigital.adapter.out..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.ikigaidigital.application.service..", "org.ikigaidigital.application.port.in..");

    @ArchTest
    static final ArchRule jpaOnlyInThePersistenceAdapter =
        noClasses().that().resideOutsideOfPackage("org.ikigaidigital.adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..");

    @ArchTest
    static final ArchRule springWebOnlyInTheWebAdapter =
        noClasses().that().resideOutsideOfPackage("org.ikigaidigital.adapter.in.web..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "org.springframework.http..");
}
