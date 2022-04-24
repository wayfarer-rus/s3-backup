package org.s3.backup.lib.metadata.model

import java.io.File

@kotlinx.serialization.Serializable
class DirMetadata(
    override val name: String,
    override val path: String,
    override val lastModified: Long,
) : MetadataNode() {
    private val _children: MutableList<MetadataNode> = mutableListOf()
    val children: List<MetadataNode>
        get() = _children

    fun add(newNode: MetadataNode) {
        _children.add(newNode)
    }

    override fun filesList() = _children.flatMap { it.filesList() }
    override fun pathList() = listOf(path) + _children.flatMap { it.pathList() }
    override fun writeToDisk(pathDest: String) {
        File("$pathDest/$path").mkdirs()
        _children.forEach { it.writeToDisk(pathDest) }
    }
}
