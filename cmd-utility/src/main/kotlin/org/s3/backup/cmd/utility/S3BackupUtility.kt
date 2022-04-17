package org.s3.backup.cmd.utility

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.s3.backup.cmd.utility.model.DirMetadata
import org.s3.backup.cmd.utility.model.FileMetadata
import org.s3.backup.cmd.utility.model.MetadataNode
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Stack

object S3BackupUtility {
    // the main idea is that we have to create some kind of metadata
    // that will allow us to handle delta-backups
    fun doBackup(directory: File, bucketName: String, dryRun: Boolean = false) {
        val currentTimestamp = System.currentTimeMillis()
        val metadataFileName = "$currentTimestamp.metadata"
        val zipFileName = "$currentTimestamp.zip"
        val metadataFileFullPath = "${getTempDir()}$metadataFileName"
        val zipFileFullPath = "${getTempDir()}$zipFileName"
        // access bucket and download latest metadata file
        // TODO:
        val latestBackupMetadata: MetadataNode? = downloadLatestMetadata(bucketName)
        // create metadata for selected directory
        val freshMetadata = collectMetadata(directory)

        // compare root of the bucket metadata with current root
        if (latestBackupMetadata != null && freshMetadata.name == latestBackupMetadata.name) {
            // if root is the same, calculate delta
            val newOrUpdatedFiles = updateOldFilesAndFindNewFiles(latestBackupMetadata, freshMetadata)

            // if there are no mew checksums - exit with "nothing to do message"
            if (newOrUpdatedFiles.isEmpty()) {
                println("nothing to do; no updates")
                return
            }

            // 4) for new checksums create new zip-file
            zipNewFiles(zipFileFullPath, zipFileName, dryRun, newOrUpdatedFiles)
            // 5) upload zip file with new metadata file to the cloud
            // TODO:
        } else {
            // completely new backup
            // compress it to Zip
            zipNewFiles(zipFileFullPath, zipFileName, dryRun, freshMetadata.filesList())
        }

        val data = Json.encodeToString(freshMetadata)
        println("Metadata file will be created here: $metadataFileFullPath")

        if (!dryRun) {
            File(metadataFileFullPath).also { it.writeText(data) }
        }

        uploadToCloud(File(metadataFileFullPath), File(zipFileFullPath))
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

    private fun zipNewFiles(
        zipFileFullPath: String,
        zipFileName: String,
        dryRun: Boolean,
        fileCards: List<FileMetadata>
    ) {
        // collect files distinct by the checksum
        // this is new files. All new files will have new zip file name as reference
        val distinctFileCards = fileCards
            .onEach {
                it.archiveLocationRef = zipFileName
            }
            .distinctBy { it.checksum }

        println("Zip archive will be created here: $zipFileFullPath")

        if (!dryRun) {
            ZipUtility.zipFiles(distinctFileCards, zipFileFullPath)
        }
    }

    private fun downloadLatestMetadata(bucketName: String): MetadataNode? {
        // TODO:
        return null
    }

    private fun uploadToCloud(metadataFile: File, zipFileName: File) {
        println("and upload to S3...")
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
                        checksum = sha256(file).toHex()
                    ).apply {
                        localFileRef = file
                    }
                    stack.peek().add(newFile)
                }

            return latestPop
        }
    }

    fun getTempDir(): String = System.getProperty("java.io.tmpdir") ?: ""
}

fun sha256(file: File): ByteArray = file.inputStream().use { fis ->
    val digester = MessageDigest.getInstance("SHa-256")
    val buffer = ByteArray(4096)
    var bytesCount: Int

    while (fis.read(buffer).also { bytesCount = it } != -1) {
        digester.update(buffer, 0, bytesCount)
    }

    digester.digest()
}

fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }