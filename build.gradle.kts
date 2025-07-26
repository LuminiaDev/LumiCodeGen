plugins {
    id("java")
}

group = "com.luminiadev.lumi.codegen"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.palantir.javapoet:javapoet:0.7.0")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}