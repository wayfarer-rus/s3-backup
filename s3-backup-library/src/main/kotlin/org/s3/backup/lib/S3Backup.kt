package org.s3.backup.lib

import mu.KotlinLogging
import org.s3.backup.lib.client.S3Client
import org.s3.backup.lib.client.backupContext.BackupContext
import org.s3.backup.lib.client.request.DoBackupRequest.Companion.invalidDirectoryToBackupError
import org.s3.backup.lib.client.request.RestoreFromBackupRequest
import org.s3.backup.lib.metadata.model.DirMetadata
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.metadata.model.MetadataNode
import org.s3.backup.lib.utilities.BackupUtility
import org.s3.backup.lib.utilities.RestoreUtility
import org.s3.backup.lib.validators.FileValidators
import org.s3.backup.lib.validators.S3BucketValidators
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * "Classic" style API
 */
class S3Backup(val bucketName: String) {
    init {
        if (!S3BucketValidators.isValidName(bucketName))
            BackupContext.invalidBucketNameError(bucketName)
    }

    /**
     * Uploads directory path content to the S3
     *
     * @param directoryPath
     *        path to the directory that will be uploaded
     * @return S3BackupMetadata object with backup metadata and key
     */
    fun upload(directoryPath: Path): S3BackupMetadata {
        val directoryToBackup = directoryPath.toAbsolutePath().normalize().toString()

        if (!FileValidators.isValidInputDir(directoryToBackup)) {
            invalidDirectoryToBackupError(directoryToBackup)
        }

        val key = "${System.currentTimeMillis()}"
        val metadataNode = BackupUtility.doBackup(
            File(directoryToBackup),
            bucketName,
            key = key
        )

        return S3BackupMetadata(key, metadataNode)
    }

    /**
     * Will return backup metadata object from bucket.
     *
     * @param key
     *        backup key
     * @return S3BackupMetadata object with metadata adn key
     */
    fun getMetadata(key: String) = S3BackupMetadata(key, getMetadataNode(key))

    private fun getMetadataNode(key: String): MetadataNode {
        try {
            return S3Client.downloadBackupMetadata(bucketName, key)!!
        } catch (err: Exception) {
            logger.debug(err) { "Error loading backup metadata" }
            error("Unable to load metadata for key $key from bucket $bucketName due to error: ${err.message}")
        }
    }

    /**
     * List backups in bucket.
     *
     * @return list of all backup keys
     */
    fun list(): List<String> {
        return S3Client.collectMetadataEntriesFromBucket(bucketName)
    }

    /**
     * Get file data from S3 as an inputStream.
     *
     * @param fileNode
     *        FileMetadata object from backup metadata
     * @return InputStream with file content
     */
    fun fileNodeInputStream(fileNode: FileMetadata) =
        RestoreUtility.inputStreamForFileNode(
            bucketName,
            fileNode
        )

    /**
     * Restore backup to given directory.
     *
     * @param fromKey
     *        backup key
     * @param toDirectory
     *        path to the directory
     */
    fun restore(fromKey: String, toDirectory: Path) {
        val destinationDirectory = toDirectory.toAbsolutePath().normalize().toString()

        if (!FileValidators.isValidOutputDir(destinationDirectory)) {
            RestoreFromBackupRequest.invalidDestinationDirectoryError(destinationDirectory)
        }

        RestoreUtility.doRestore(
            getMetadataNode(fromKey),
            bucketName,
            destinationDirectory
        )
    }

    /**
     * Delete backup from S3
     *
     * @param key
     *        backup key
     * @return true if backup was deleted, false otherwise
     */
    fun deleteBackup(key: String) = S3Client.deleteBackup(bucketName, key)
}

class S3BackupMetadata(
    val key: String,
    metadataNode: MetadataNode
) {
    private val allNodes: List<MetadataNode> = metadataNode.flatten()
    val directoriesMetadataList: List<DirMetadata> = allNodes.filterIsInstance<DirMetadata>()
    val filesMetadataList: List<FileMetadata> = metadataNode.filesList()

    fun contains(name: String) = allNodes.any(metadataNodeMatcher(name))

    fun findNode(name: String) = allNodes.find(metadataNodeMatcher(name))

    fun findFile(fileName: String) = filesMetadataList.find(metadataNodeMatcher(fileName))

    private fun metadataNodeMatcher(name: String): (MetadataNode) -> Boolean =
        { it.name == name || it.path == name || (it is FileMetadata && it.checksum == name) }
}
