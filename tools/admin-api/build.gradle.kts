plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.Vectura AI.tools.admin.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.json)

    // Ktor Client (for Anthropic API calls from the server)
    implementation(libs.ktor.client.java)
    implementation(libs.ktor.client.content.negotiation)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Database — PostgreSQL + Exposed ORM
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql)
    implementation(libs.hikari)

    // Auth — BCrypt password hashing
    implementation(libs.bcrypt)

    // QR code generation (ZXing)
    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)
    implementation(libs.dotenv)

    // Legacy: nav-preprocessor for backward-compatible draft pipeline
    implementation(project(":tools:nav-preprocessor"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
