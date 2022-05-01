package org.s3.backup.lib.metadata.model

@kotlinx.serialization.Serializable
class ArchiveLocationRef(
    var archiveName: String = "",
    var zipLfhLocation: ZipLfhLocation? = null
) {
    fun toRangeString() = zipLfhLocation!!.toRangeString()
}

@kotlinx.serialization.Serializable
class ZipLfhLocation(
    var offset: Long = 0,
    var length: Long = 0
) {
    fun toRangeString(): String {
        return "bytes=$offset-${offset + length}"
    }
}
