package org.s3.backup.lib.metadata.model

import software.amazon.awssdk.utils.IoUtils
import java.io.File
import java.io.FileInputStream

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

    @kotlinx.serialization.Transient
    private val _inputStream: Lazy<FileInputStream?> = lazy {
        localFileRef?.inputStream()
    }

    val inputStream: FileInputStream?
        get() = _inputStream.value

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
