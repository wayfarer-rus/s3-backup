plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.17.169"))
    implementation("software.amazon.awssdk:s3")

    implementation(kotlin("stdlib"))
}
