package com.whatiread.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Expand/contract migrations: add nullable columns first, backfill, then enforce NOT NULL and drop old columns in a later release — never rename/drop
 * in the same deploy as code changes.
 */
@AnalyzeClasses(
        packages = "com.whatiread",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ModularMonolithArchitectureTest {

    private static final String[] BASE_FOREIGN_REPOSITORIES = {
            "..catalog.repository..",
            "..identity.repository..",
            "..library.repository..",
            "..shelf.repository..",
            "..messaging.repository..",
            "..social.repository..",
            "..comment.repository..",
            "..goal.repository..",
            "..recommendation.repository..",
            "..importexport.repository..",
            "..instance.repository.."
    };

    @ArchTest
    static final ArchRule goalMustNotUseForeignRepositories = noCrossModuleRepoAccess(
            "..goal..",
            "..goal.repository.."
    );

    @ArchTest
    static final ArchRule commentMustNotUseForeignRepositories = noCrossModuleRepoAccess(
            "..comment..",
            "..comment.repository.."
    );

    @ArchTest
    static final ArchRule messagingMustNotUseForeignRepositories = noCrossModuleRepoAccess(
            "..messaging..",
            "..messaging.repository.."
    );

    @ArchTest
    static final ArchRule importexportMustNotUseForeignRepositories = noCrossModuleRepoAccess(
            "..importexport..",
            "..importexport.repository.."
    );

    @ArchTest
    static final ArchRule recommendationMustNotUseForeignRepositories = noCrossModuleRepoAccess(
            "..recommendation..",
            "..recommendation.repository.."
    );

    private static ArchRule noCrossModuleRepoAccess(String modulePackage, String allowedRepositoryPackage) {
        String[] forbidden = java.util.Arrays.stream(BASE_FOREIGN_REPOSITORIES)
                .filter(pkg -> !pkg.equals(allowedRepositoryPackage))
                .toArray(String[]::new);
        return noClasses()
                .that().resideInAPackage(modulePackage)
                .and().resideOutsideOfPackage("..integration..")
                .and().resideOutsideOfPackage(allowedRepositoryPackage)
                .should().dependOnClassesThat().resideInAnyPackage(forbidden)
                .allowEmptyShould(true);
    }
}
