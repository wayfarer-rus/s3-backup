package org.s3.backup.lib.zip.model

@kotlinx.serialization.Serializable
class ZipLfhLocation(
    var offset: Long = 0,
    var length: Long = 0
) {
    fun toRangeString(): String = "bytes=$offset-${offset + length - 1}"
}
