package org.s3.backup.cmd.utility.model

@kotlinx.serialization.Serializable
sealed class MetadataNode {
    abstract val name: String
    abstract val path: String
    abstract val lastModified: Long
    abstract fun filesList(): List<FileMetadata>
}
