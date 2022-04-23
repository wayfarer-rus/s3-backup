package org.s3.backup.lib.zip.model

data class ZipLfhLocation(
    val offset: Long,
    var length: Long = 0
)
