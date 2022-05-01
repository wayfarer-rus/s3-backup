plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.6.10"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(platform("software.amazon.awssdk:bom:2.17.169"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:s3-transfer-manager:2.17.169-PREVIEW")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation(kotlin("test-junit"))
}
