plugins {
  `java-library`
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation(platform("com.test.platform:platform-bom:0.1.0-SNAPSHOT"))
  implementation("com.test.platform:platform-kernel")

  testImplementation(platform("com.test.platform:platform-bom:0.1.0-SNAPSHOT"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}
