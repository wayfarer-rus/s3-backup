plugins {
    kotlin("jvm")
    application
}

version = "0.1.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.s3.backup.cmd.utility.MainKt")
}

dependencies {
    implementation(project(":s3-backup-library"))
    implementation(kotlin("stdlib"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation(kotlin("test-junit"))
}
