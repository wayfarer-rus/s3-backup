package org.s3.backup.lib.metadata.model

import java.io.File

@kotlinx.serialization.Serializable
class DirMetadata(
    override val name: String,
    override val path: String,
    override val lastModified: Long,
) : MetadataNode() {
    private val children: MutableList<MetadataNode> = mutableListOf()

    fun add(newNode: MetadataNode) {
        children.add(newNode)
    }

    override fun filesList() = children.flatMap { it.filesList() }
    override fun pathList() = listOf(path) + children.flatMap { it.pathList() }
    override fun writeToDisk(pathDest: String) {
        File("$pathDest/$path").mkdirs()
        children.forEach { it.writeToDisk(pathDest) }
    }

    override fun flatten() = listOf(this) + children.flatMap { it.flatten() }
}
