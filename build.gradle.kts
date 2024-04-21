plugins {
    kotlin("jvm") version "1.9.23"
}

group = "hiencao.me"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.7")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.h2database:h2:2.1.210")
    implementation("com.h2database:h2:2.2.224")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}