plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "invcaptive"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("26.3.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}