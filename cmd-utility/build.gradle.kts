plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.6.10"
}

version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":s3-backup-library"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation(kotlin("test-junit"))
}
