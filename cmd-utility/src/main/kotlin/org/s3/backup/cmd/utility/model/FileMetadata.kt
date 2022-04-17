package org.s3.backup.cmd.utility.model

import java.io.File

@kotlinx.serialization.Serializable
class FileMetadata(
    override val name: String,
    override val path: String,
    override val lastModified: Long,
    val checksum: String,
) : MetadataNode() {
    var archiveLocationRef: String = ""

    @kotlinx.serialization.Transient
    var localFileRef: File? = null

    override fun filesList() = listOf(this)
}
