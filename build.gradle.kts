plugins {
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "7.0.2"
}

group = "io.github.jpalmerr"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("net.jqwik:jqwik:1.9.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

sourceSets {
    create("example") {
        java.srcDir("src/example/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.named<JavaCompile>("compileExampleJava") {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.register<JavaExec>("runExample") {
    mainClass = "io.github.jpalmerr.valid4j.example.UserRegistrationExample"
    classpath = sourceSets["example"].runtimeClasspath
}

tasks.register<JavaExec>("runAsyncExample") {
    mainClass = "io.github.jpalmerr.valid4j.example.AsyncValidationExample"
    classpath = sourceSets["example"].runtimeClasspath
}


tasks.compileTestJava {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "io.github.jpalmerr"
            artifactId = "valid4j"
            version = "1.0.0"
        }
    }
}
