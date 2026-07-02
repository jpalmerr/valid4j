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
    withSourcesJar()
    withJavadocJar()
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

tasks.register("verifyZeroDependencies") {
    doLast {
        val runtimeClasspath = configurations.runtimeClasspath.get()
        if (runtimeClasspath.dependencies.isNotEmpty()) {
            throw GradleException("Zero-dependency constraint violated. Found runtime dependencies: ${runtimeClasspath.dependencies.map { it.name }}")
        }
        println("✓ Zero-dependency constraint verified: no runtime dependencies")
    }
}

tasks.build {
    dependsOn("verifyZeroDependencies")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("valid4j")
                description.set("Zero-dependency Java 21 library for typed error accumulation via applicative validation")
                url.set("https://github.com/jpalmerr/valid4j")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/jpalmerr/valid4j")
                    connection.set("scm:git:https://github.com/jpalmerr/valid4j.git")
                    developerConnection.set("scm:git:https://github.com/jpalmerr/valid4j.git")
                }
                developers {
                    developer {
                        name.set("James Palmer")
                        email.set("james.palmer@conduktor.io")
                    }
                }
            }
        }
    }
}
