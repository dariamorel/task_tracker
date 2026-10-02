plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    application
}

group = "ru.hse"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ru.hse.tracker.AppKt")
}

tasks.register<JavaExec>("migrate") {
    group = "application"
    description = "Applies database migrations (one-off admin process)"
    mainClass.set("ru.hse.tracker.MigrateKt")
    classpath = sourceSets["main"].runtimeClasspath
}
