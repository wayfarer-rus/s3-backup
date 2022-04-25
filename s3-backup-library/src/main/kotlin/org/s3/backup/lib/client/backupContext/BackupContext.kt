package org.s3.backup.lib.client.backupContext

import org.s3.backup.lib.utilities.S3BackupUtility
import org.s3.backup.lib.validators.BackupValidators
import org.s3.backup.lib.validators.S3BucketValidators

class BackupContext(
    val bucketName: String,
    val backupKey: String
) {
    val backupMetadata = S3BackupUtility.downloadBackupMetadata(bucketName, backupKey)
        ?: error("Backup with key '$backupKey' wasn't found in bucket '$bucketName'")

    class Builder {
        var backupKey: String = ""
        var bucketName: String = ""

        fun build(): BackupContext {
            if (!S3BucketValidators.isValidName(bucketName)) {
                invalidBucketNameError(bucketName)
            }

            if (!BackupValidators.isValidKey(backupKey)) {
                throw IllegalArgumentException("Invalid backup key value: $backupKey. Expected timestamp in millis.")
            }

            return BackupContext(
                bucketName = bucketName,
                backupKey = backupKey
            )
        }
    }

    override fun toString(): String {
        return "bucket '$bucketName' and backup key '$backupKey'"
    }

    companion object {
        fun invalidBucketNameError(bucketName: String) {
            throw IllegalArgumentException("Invalid S3 bucket name: $bucketName.")
        }
    }
}
