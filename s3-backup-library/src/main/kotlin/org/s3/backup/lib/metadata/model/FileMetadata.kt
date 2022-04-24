package org.s3.backup.lib.metadata.model

import software.amazon.awssdk.utils.IoUtils
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
    override fun writeToDisk(pathDest: String) {
        if (localFileRef == null) error("can't write file '$path' (checksum $checksum) to $pathDest")

        File("$pathDest/$path").outputStream().use { fos ->
            localFileRef?.inputStream()?.use { fis ->
                IoUtils.copy(fis, fos)
            }
        }
    }
}
