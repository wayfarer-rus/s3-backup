package org.s3.backup.lib.validators

import software.amazon.awssdk.services.s3.internal.BucketUtils

object S3BucketValidators {
    fun isValidName(bucketName: String) = BucketUtils.isValidDnsBucketName(bucketName, false)
}
