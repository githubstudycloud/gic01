package com.test.platform.kernel;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.test.platform.kernel")
class KernelArchitectureTest {
	@ArchTest
	static final ArchRule mustNotDependOnSpring = noClasses().should().dependOnClassesThat()
			.resideInAnyPackage("org.springframework..");
}
