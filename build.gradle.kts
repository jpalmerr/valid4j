plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "8.9.0"
}

group = "io.github.jpalmerr"
version = "0.1.0"

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
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("net.jqwik:jqwik:1.10.1")
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
        // allDependencies, not dependencies: `dependencies` ignores superconfigurations, and
        // runtimeClasspath never has dependencies declared directly on it — they arrive via
        // implementation/api/runtimeOnly, which it extends.
        val declared = configurations.runtimeClasspath.get().allDependencies.map { it.name }
        if (declared.isNotEmpty()) {
            throw GradleException("Zero-dependency constraint violated. Found runtime dependencies: $declared")
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
                        id.set("jpalmerr")
                        name.set("James Palmer")
                        url.set("https://github.com/jpalmerr")
                    }
                }
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("SIGNING_KEY").orNull

    // Signing is opt-in rather than required: README tells users to run publishToMavenLocal, which
    // would fail for anyone without the private key. release.yml asserts the key is present, so a
    // real release cannot slip through unsigned.
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, providers.environmentVariable("SIGNING_PASSPHRASE").orNull)
        sign(publishing.publications["maven"])
    }
}
