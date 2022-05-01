package org.s3.backup.lib.client

import org.s3.backup.lib.client.request.DoBackupRequest
import org.s3.backup.lib.client.request.DownloadFileFromBackupRequest
import org.s3.backup.lib.client.request.ListAllBackupsRequest
import org.s3.backup.lib.client.request.ListBackupContentRequest
import org.s3.backup.lib.client.request.RestoreFromBackupRequest
import org.s3.backup.lib.client.response.DownloadFileFromBackupResponse
import org.s3.backup.lib.client.response.ListAllBackupsResponse
import org.s3.backup.lib.client.response.ListBackupContentResponse
import org.s3.backup.lib.utilities.BackupUtility
import org.s3.backup.lib.utilities.RestoreUtility
import java.io.File

class S3BackupClient {
    // will find build context and download given file by given path
    fun downloadFromBackup(downloadFileRequest: DownloadFileFromBackupRequest): DownloadFileFromBackupResponse {
        // find file in metadata
        val fileNode = downloadFileRequest.context.backupMetadata.filesList()
            .find { it.path == downloadFileRequest.filePathToDownload }
            ?: error("File '${downloadFileRequest.filePathToDownload}' wasn't found in ${downloadFileRequest.context}.")

        // return temp file as input stream to the caller
        return DownloadFileFromBackupResponse(
            inputStream = RestoreUtility.inputStreamForFileNode(
                downloadFileRequest.context.bucketName,
                fileNode
            )
        )
    }

    fun restoreFromBackup(restoreBackupRequest: RestoreFromBackupRequest) =
        RestoreUtility.doRestore(
            restoreBackupRequest.context.backupMetadata,
            restoreBackupRequest.context.bucketName,
            restoreBackupRequest.destinationDirectory
        )

    fun listAllBackups(listAllBackupsRequest: ListAllBackupsRequest) =
        ListAllBackupsResponse(S3Client.collectMetadataEntriesFromBucket(listAllBackupsRequest.bucketName))

    fun listBackupContent(listBackupContentRequest: ListBackupContentRequest) =
        ListBackupContentResponse(listBackupContentRequest.backupContext.backupMetadata.pathList())

    fun doBackup(doBackupRequest: DoBackupRequest) =
        BackupUtility.doBackup(
            File(doBackupRequest.directoryToBackup),
            doBackupRequest.bucketName,
            doBackupRequest.isDryRun
        )

    companion object {
        fun create(): S3BackupClient {
            return S3BackupClient()
        }
    }
}
