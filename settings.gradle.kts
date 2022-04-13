dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "s3-backup"
include("s3-backup-library")
include("cmd-utility")
