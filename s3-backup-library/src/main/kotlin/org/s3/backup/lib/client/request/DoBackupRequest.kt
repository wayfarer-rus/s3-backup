package org.s3.backup.lib.client.request

import org.s3.backup.lib.client.backupContext.BackupContext
import org.s3.backup.lib.validators.FileValidators
import org.s3.backup.lib.validators.S3BucketValidators

class DoBackupRequest(
    val bucketName: String,
    val directoryToBackup: String,
    val isDryRun: Boolean
) {
    class Builder {
        var directoryToBackup: String = ""
        var bucketName: String = ""
        var dryRun: Boolean = false

        fun build(): DoBackupRequest {
            if (!S3BucketValidators.isValidName(bucketName)) {
                BackupContext.invalidBucketNameError(bucketName)
            }

            if (!FileValidators.isValidInputDir(directoryToBackup)) {
                throw IllegalArgumentException("Given directory $directoryToBackup is either not a directory or not accessible")
            }

            return DoBackupRequest(
                bucketName = bucketName,
                directoryToBackup = directoryToBackup,
                dryRun
            )
        }
    }
}

fun doBackupRequest(init: DoBackupRequest.Builder.() -> Unit): DoBackupRequest {
    return DoBackupRequest.Builder().apply(init).build()
}
