package com.test.platform.lock.adapter.local;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.test.platform.lock.adapter.local")
class LocalLockArchitectureTest {
  @ArchTest
  static final ArchRule mustNotDependOnSpring =
      noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
}

