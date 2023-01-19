group = "mx.com.inftel.codegen"
version = "2.0.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.7.21"
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    //withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

val kotlinJavadoc by tasks.registering(Jar::class) {
    archiveBaseName.set("codegen2-apt")
    archiveClassifier.set("javadoc")
    from(file("$projectDir/javadoc/README"))
}

publishing {
    repositories {
        maven {
            setUrl(file("$projectDir/build/repo"))
        }
    }

    publications {
        create<MavenPublication>("codegen2Apt") {
            artifact(kotlinJavadoc)
            from(components["java"])
        }
    }

    publications.withType<MavenPublication> {
        pom {
            name.set("${project.group}:${project.name}")
            description.set("Codegen2 APT")
            url.set("https://github.com/santoszv/codegen2-apt")
            inceptionYear.set("2022")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                }
            }
            developers {
                developer {
                    id.set("santoszv")
                    name.set("Santos Zatarain Vera")
                    email.set("santoszv@inftel.com.mx")
                    url.set("https://www.inftel.com.mx")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/santoszv/codegen2-apt")
                developerConnection.set("scm:git:https://github.com/santoszv/codegen2-apt")
                url.set("https://github.com/santoszv/codegen2-apt")
            }
        }
        signing.sign(this)
    }
}

signing {
    useGpgCmd()
}