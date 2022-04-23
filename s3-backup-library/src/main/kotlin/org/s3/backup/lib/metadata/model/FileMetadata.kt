package org.s3.backup.lib.metadata.model

import java.io.File

@kotlinx.serialization.Serializable
class FileMetadata(
    override val name: String,
    override val path: String,
    override val lastModified: Long,
    val size: Long = 0,
    val checksum: String,
) : MetadataNode() {
    var archiveLocationRef = ArchiveLocationRef()

    @kotlinx.serialization.Transient
    var localFileRef: File? = null

    override fun filesList() = listOf(this)
    override fun pathList() = listOf(path)
}
