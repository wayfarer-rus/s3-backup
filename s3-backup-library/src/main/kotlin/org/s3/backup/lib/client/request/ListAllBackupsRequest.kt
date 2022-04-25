package org.s3.backup.lib.client.request

import org.s3.backup.lib.client.backupContext.BackupContext
import org.s3.backup.lib.validators.S3BucketValidators

class ListAllBackupsRequest(val bucketName: String) {
    class Builder {
        var bucketName: String = ""

        fun build(): ListAllBackupsRequest {
            if (!S3BucketValidators.isValidName(bucketName)) {
                BackupContext.invalidBucketNameError(bucketName)
            }

            return ListAllBackupsRequest(bucketName)
        }
    }
}

fun listAllBackupsRequest(init: ListAllBackupsRequest.Builder.() -> Unit): ListAllBackupsRequest {
    return ListAllBackupsRequest.Builder().apply(init).build()
}
