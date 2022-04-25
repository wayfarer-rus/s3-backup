package org.s3.backup.cmd.utility

import org.s3.backup.lib.client.S3BackupClient
import org.s3.backup.lib.client.request.doBackupRequest
import org.s3.backup.lib.client.request.downloadFileFromBackupRequest
import org.s3.backup.lib.client.request.listAllBackupsRequest
import org.s3.backup.lib.client.request.listBackupContentRequest
import org.s3.backup.lib.client.request.restoreFromBackupRequest
import org.s3.backup.lib.utilities.writeToFile
import org.s3.backup.lib.validators.FileValidators
import java.io.File

object S3BackupUtility {
    private val s3BackupClient = S3BackupClient.create()

    fun listAllBackups(bucketName: String) {
        val listAllBackupsRequest = listAllBackupsRequest {
            this.bucketName = bucketName
        }

        val listOfBackups = s3BackupClient.listAllBackups(listAllBackupsRequest)

        // just print them out
        listOfBackups.backupKeysList.forEach(::println)
    }

    fun listBackupContent(bucketName: String, backupKey: String) {
        // TODO:: can be later expanded with additional arguments for better content visualization
        val listBackupContentRequest = listBackupContentRequest {
            backupContext {
                this.bucketName = bucketName
                this.backupKey = backupKey
            }
        }

        val response = s3BackupClient.listBackupContent(listBackupContentRequest)
        response.pathList.forEach(::println)
    }

    fun downloadFileFromBackup(
        bucketName: String,
        backupKey: String,
        filePathToDownload: String,
        pathDest: String
    ) {
        if (!FileValidators.isValidOutputFile(pathDest)) {
            canNotWriteOutputError(pathDest)
        }

        val downloadFileRequest = downloadFileFromBackupRequest {
            backupContext {
                this.bucketName = bucketName
                this.backupKey = backupKey
            }
            this.filePathToDownload = filePathToDownload
        }

        s3BackupClient.downloadFromBackup(downloadFileRequest).inputStream.writeToFile(File(pathDest))
    }

    fun restoreFromBackup(
        bucketName: String,
        backupKey: String,
        pathDest: String
    ) {
        val restoreBackupRequest = restoreFromBackupRequest {
            backupContext {
                this.bucketName = bucketName
                this.backupKey = backupKey
            }
            this.destinationDirectory = pathDest
        }

        s3BackupClient.restoreFromBackup(restoreBackupRequest)
    }

    fun doBackup(directoryToBackup: String, bucketName: String, dryRun: Boolean = false) {
        val doBackupRequest = doBackupRequest {
            this.bucketName = bucketName
            this.directoryToBackup = directoryToBackup
            this.dryRun = dryRun
        }

        s3BackupClient.doBackup(doBackupRequest)
    }

    private fun canNotWriteOutputError(pathDest: String) {
        throw IllegalArgumentException("Can't write output to '$pathDest'")
    }
}
