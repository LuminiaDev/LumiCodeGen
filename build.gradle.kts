plugins {
    id("java")
    id("application")
}

group = "com.luminiadev.lumi.codegen"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.luminiadev.com/snapshots")
}

dependencies {
    implementation("com.koshakmine:Lumi:1.1.0-SNAPSHOT")
    implementation("com.palantir.javapoet:javapoet:0.7.0")
    implementation("com.google.code.gson:gson:2.13.1")
    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}

application {
    mainClass.set("com.luminiadev.lumi.codegen.LumiCodeGen")
}