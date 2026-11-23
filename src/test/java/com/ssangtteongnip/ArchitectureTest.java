package com.ssangtteongnip;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// 계층 의존 방향: web -> application -> domain <- infrastructure. 도메인은 JPA 어노테이션까지만 허용하고 Spring은 쓰지 않는다.
@AnalyzeClasses(packages = "com.ssangtteongnip", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "com.ssangtteongnip.domain..";
    private static final String APPLICATION = "com.ssangtteongnip.application..";
    private static final String WEB = "com.ssangtteongnip.web..";
    private static final String INFRASTRUCTURE = "com.ssangtteongnip.infrastructure..";

    @ArchTest
    static final ArchRule 도메인은_Spring에_의존하지_않는다 =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule 도메인은_바깥_계층에_의존하지_않는다 =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, WEB, INFRASTRUCTURE);

    @ArchTest
    static final ArchRule 애플리케이션은_웹과_인프라에_의존하지_않는다 =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(WEB, INFRASTRUCTURE);

    @ArchTest
    static final ArchRule 웹은_인프라에_의존하지_않는다 =
            noClasses().that().resideInAPackage(WEB)
                    .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE);
}
