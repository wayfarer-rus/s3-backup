package org.s3.backup.cmd.utility

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.s3.backup.cmd.utility.model.FileMetadata
import java.io.File

object S3BackupUtility {
    // the main idea is that we have to create some kind of metadata
    // that will allow us to handle delta-backups
    fun doBackup(directory: File, bucketName: String, dryRun: Boolean = false) {
        // check if there is already a metadata file
        // TODO: download metadata file from bucket
        // create metadata
        val freshMetadata = collectMetadata(directory)
        // calculate diff
        // TODO:
        // save fresh metadata
        val data = Json.encodeToString(freshMetadata)
        val freshMetadataFile = File("${getTempDir()}${ProcessHandle.current().pid()}.metadata")
            .also { it.writeText(data) }
    }

    fun collectMetadata(directory: File): List<FileMetadata> {
        return directory.walkTopDown()
            .map {
                FileMetadata(
                    name = it.name,
                    path = it.path,
                    lastModified = it.lastModified(),
                    isFile = it.isFile,
                    isDirectory = it.isDirectory
                )
            }.toList()
    }

    fun getTempDir(): String {
        return System.getProperty("java.io.tmpdir") ?: ""
    }
}
