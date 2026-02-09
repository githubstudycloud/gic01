# platform-example-gradle-consumer

Minimal Gradle consumer example for `platform-bom` + `platform-kernel`.

Prereq (publish platform to mavenLocal):

```bash
mvn -q -DskipTests install
```

Then:

```bash
./gradlew test
```

