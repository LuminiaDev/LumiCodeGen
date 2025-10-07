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
    maven {
        url = uri("https://repo.opencollab.dev/maven-releases/")
        mavenContent { releasesOnly() }
    }
    maven {
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    implementation("com.koshakmine:Lumi:1.2.0-SNAPSHOT")
    implementation("org.cloudburstmc:nbt:3.0.0.Final")
    implementation("com.palantir.javapoet:javapoet:0.7.0")
    implementation("com.google.code.gson:gson:2.13.1")
    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}

application {
    mainClass.set("com.luminiadev.lumi.codegen.LumiCodeGen")
}