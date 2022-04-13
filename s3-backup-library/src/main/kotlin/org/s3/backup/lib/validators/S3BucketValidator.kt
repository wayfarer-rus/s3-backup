package org.s3.backup.lib.validators

import software.amazon.awssdk.services.s3.internal.BucketUtils

class S3BucketValidator {
    companion object {
        fun isValidName(bucketName: String) = BucketUtils.isValidDnsBucketName(bucketName, false)
    }
}
