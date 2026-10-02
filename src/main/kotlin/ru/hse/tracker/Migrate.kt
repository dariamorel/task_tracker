package ru.hse.tracker

import org.flywaydb.core.Flyway

fun main() {
    fun env(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: error("Environment variable $name is required")
    val result = Flyway.configure()
        .dataSource(env("DATABASE_URL"), env("DATABASE_USER"), env("DATABASE_PASSWORD"))
        .locations("classpath:db/migration")
        .load()
        .migrate()
    println("Applied ${result.migrationsExecuted} migration(s), schema version: ${result.targetSchemaVersion ?: "none"}")
}
