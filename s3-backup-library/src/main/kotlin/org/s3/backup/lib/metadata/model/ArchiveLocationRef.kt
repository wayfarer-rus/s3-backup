package org.s3.backup.lib.metadata.model

@kotlinx.serialization.Serializable
class ArchiveLocationRef(
    var archiveName: String = "",
    var fileLocation: FileLocationInArchive = FileLocationInArchive()
) {
    fun toRangeString() = fileLocation.toRangeString()
}

@kotlinx.serialization.Serializable
class FileLocationInArchive(
    var offset: Long = 0,
    var length: Long = 0
) {
    fun toRangeString(): String {
        if (length == 0L) error("0-size file handling exception")
        // bytes ranges are inclusive, therefore -1
        return "bytes=$offset-${offset + length - 1}"
    }
}
