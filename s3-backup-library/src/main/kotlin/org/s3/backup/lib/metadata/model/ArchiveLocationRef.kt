package org.s3.backup.lib.metadata.model

@kotlinx.serialization.Serializable
class ArchiveLocationRef(
    var archiveName: String = "",
    var fileLocation: FileLocationInArchive = FileLocationInArchive()
) {
    fun toRangeString() = fileLocation.toRangeString()
    override fun toString(): String {
        return "ArchiveLocationRef(archiveName='$archiveName', fileLocation=$fileLocation)"
    }
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

    fun isInvalid(): Boolean {
        return offset < 0 || length < 0
    }

    override fun toString(): String {
        return "FileLocationInArchive(offset=$offset, length=$length)"
    }
}
