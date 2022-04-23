package org.s3.backup.lib.metadata.model

import org.s3.backup.lib.zip.model.ZipLfhLocation

@kotlinx.serialization.Serializable
class ArchiveLocationRef(
    var archiveName: String = "",
    var zipLfhLocation: ZipLfhLocation? = null
) {
    fun toRangeString() = zipLfhLocation!!.toRangeString()
}
