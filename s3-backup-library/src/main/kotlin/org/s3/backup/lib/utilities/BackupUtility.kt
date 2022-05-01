package org.s3.backup.lib.utilities

import mu.KotlinLogging
import org.s3.backup.lib.DataBlobPublisher
import org.s3.backup.lib.client.S3Client
import org.s3.backup.lib.metadata.model.DirMetadata
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.metadata.model.MetadataNode
import java.io.File
import java.nio.file.Files
import java.util.Stack

private val logger = KotlinLogging.logger {}

internal object BackupUtility {
    // the main idea is that we have to create some kind of metadata
    // that will allow us to handle delta-backups
    fun doBackup(
        directory: File,
        bucketName: String,
        dryRun: Boolean = false,
        key: String = "${System.currentTimeMillis()}"
    ): MetadataNode {
        val metadataFileName = "$key.metadata"

        logger.info { "access bucket and download latest metadata file..." }
        val latestBackupMetadata: MetadataNode? = downloadLatestMetadata(bucketName)
        logger.info { "create metadata for selected directory" }
        val freshMetadata = collectMetadata(directory)

        // compare root of the bucket metadata with current root
        val dataBlobPublisher = if (latestBackupMetadata != null && freshMetadata.name == latestBackupMetadata.name) {
            logger.info { "calculate delta" }
            val newOrUpdatedFiles = updateOldFilesAndFindNewFiles(latestBackupMetadata, freshMetadata)

            // if there are no mew checksums - exit with "nothing to do message"
            if (newOrUpdatedFiles.isEmpty()) {
                error { "nothing to do; no updates" }
            }

            // 4) for new files create stream
            DataBlobPublisher(name = key, newOrUpdatedFiles)
        } else if (latestBackupMetadata == null) {
            // completely new backup
            DataBlobPublisher(name = key, freshMetadata.filesList())
        } else {
            error("Backup of different directories are not supported. Current backup directory is ${latestBackupMetadata.name}")
        }

        // 5) upload blob file with new metadata file to the cloud
        if (!dryRun) {
            S3Client.uploadToCloud(bucketName, metadataFileName, freshMetadata, dataBlobPublisher)
        } else {
            logger.info {
                """
                    
                    bucket name $bucketName
                    backup key ${dataBlobPublisher.name}
                    backup files from '$directory'
                    backup files count: ${dataBlobPublisher.fileReferencesList.size}
                    backup estimated size ${dataBlobPublisher.estimatedContentLength}
                """.trimIndent()
            }
        }

        logger.info { "done" }
        return freshMetadata
    }

    private fun updateOldFilesAndFindNewFiles(
        latestBackupMetadata: MetadataNode,
        freshMetadata: MetadataNode
    ): MutableList<FileMetadata> {
        // 1) take existing checksums
        val backupFileCards = latestBackupMetadata.filesList()
        val backupFileLocations = backupFileCards.associate {
            it.checksum to it.archiveLocationRef
        }

        // 2) take fresh checksums
        val freshFileCards = freshMetadata.filesList()
        val newOrUpdatedFiles = mutableListOf<FileMetadata>()

        freshFileCards.forEach {
            if (backupFileLocations.containsKey(it.checksum)) {
                // 3) replace matched checksums with references to existing backup
                it.archiveLocationRef = backupFileLocations[it.checksum]!!
            } else {
                newOrUpdatedFiles.add(it)
            }
        }

        return newOrUpdatedFiles
    }

    // TODO: take a look at https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-glacier.html
    private fun downloadLatestMetadata(bucketName: String): MetadataNode? {
        val entries = S3Client.collectMetadataEntriesFromBucket(bucketName)

        return entries.maxByOrNull {
            it.toLong()
        }?.let { key ->
            logger.debug { "download latest metadata file $key" }
            // all bytes should be fine here, while metadata likely will not be more than 2G
            S3Client.downloadBackupMetadata(bucketName, key)
        }
    }

    // build tree like structure
    // /root
    // |- file.foo
    // |- /dir
    //    |- file.bar
    // it will serve us for browsing backups later
    // each leaf must be a file card.
    // directories exist only in metadata
    fun collectMetadata(directory: File): MetadataNode {
        val rootPath = directory.path

        if (directory.isFile) {
            return FileMetadata(
                name = directory.name,
                path = directory.path.replace(rootPath, "/"),
                lastModified = directory.lastModified(),
                size = directory.length(),
                checksum = sha256(directory).toHex()
            )
        } else {
            val stack = Stack<DirMetadata>()
            // init with placeholder
            var latestPop = DirMetadata("", "", 0)

            directory.walkTopDown()
                .onEnter {
                    val newDir = DirMetadata(
                        name = it.name,
                        path = it.path.replace(rootPath, ""),
                        lastModified = it.lastModified()
                    )

                    if (stack.isNotEmpty()) {
                        stack.peek().add(newDir)
                    }

                    stack.push(newDir)
                    true
                }.onLeave {
                    latestPop = stack.pop()
                }
                // ignore symbolic links
                .filterNot { Files.isSymbolicLink(it.toPath()) }
                .filter { it.isFile }
                .forEach { file ->
                    val newFile = FileMetadata(
                        name = file.name,
                        path = file.path.replace(rootPath, ""),
                        lastModified = file.lastModified(),
                        size = file.length(),
                        checksum = sha256(file).toHex()
                    ).apply {
                        localFileRef = file
                    }
                    stack.peek().add(newFile)
                }

            return latestPop
        }
    }
}
