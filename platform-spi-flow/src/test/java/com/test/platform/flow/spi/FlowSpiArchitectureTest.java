package com.test.platform.flow.spi;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.test.platform.flow.spi")
class FlowSpiArchitectureTest {
	@ArchTest
	static final ArchRule mustNotDependOnSpringOrJakarta = noClasses().should().dependOnClassesThat()
			.resideInAnyPackage("org.springframework..", "jakarta..", "javax..");
}
