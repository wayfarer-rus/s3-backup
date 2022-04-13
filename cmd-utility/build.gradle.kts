plugins {
    kotlin("jvm")
}

version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":s3-backup-library"))
    implementation(kotlin("stdlib"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
}
