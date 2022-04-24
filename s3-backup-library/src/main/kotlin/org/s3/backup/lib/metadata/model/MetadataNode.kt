package org.s3.backup.lib.metadata.model

@kotlinx.serialization.Serializable
sealed class MetadataNode {
    abstract val name: String
    abstract val path: String
    abstract val lastModified: Long
    abstract fun filesList(): List<FileMetadata>
    abstract fun pathList(): List<String>
    abstract fun writeToDisk(pathDest: String)
}
