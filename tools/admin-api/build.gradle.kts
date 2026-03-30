plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.vecturai.tools.admin.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.json)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Reuse the existing nav-preprocessor for draft generation
    implementation(project(":tools:nav-preprocessor"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
