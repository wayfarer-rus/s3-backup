package org.s3.backup.cmd.utility.model

@kotlinx.serialization.Serializable
data class FileMetadata(
    val name: String,
    val path: String,
    val lastModified: Long,
    val isFile: Boolean,
    val isDirectory: Boolean
)
