/*
 * This file was generated by the Gradle 'init' task.
 *
 * This is a general purpose Gradle build.
 * Learn more about Gradle by exploring our Samples at https://docs.gradle.org/8.12.1/samples
 */

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.project"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

intellij {
    version.set("2024.2")
    type.set("IC")
    plugins.set(listOf("java"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("233.*")
    }
}
