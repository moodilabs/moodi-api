package com.moodi;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.Architectures;

@AnalyzeClasses(
        packages = "com.moodi",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
class LayeredArchitectureTest {

    @ArchTest
    void layeredArchitecture(JavaClasses classes) {
        Architectures.layeredArchitecture()
                .consideringAllDependencies()
                .withOptionalLayers(true)
                .layer("presentation").definedBy("..presentation..")
                .layer("application").definedBy("..application..")
                .layer("domain").definedBy("..domain..")
                .layer("infrastructure").definedBy("..infrastructure..")
                .whereLayer("presentation").mayNotBeAccessedByAnyLayer()
                .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("application").mayOnlyBeAccessedByLayers("presentation", "infrastructure")
                .whereLayer("domain").mayOnlyBeAccessedByLayers("presentation", "application", "infrastructure")
                .check(classes);
    }
}
