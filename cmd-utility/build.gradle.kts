plugins {
    kotlin("jvm") version "1.6.10"
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":s3-backup-library"))
    implementation(kotlin("stdlib"))
}
