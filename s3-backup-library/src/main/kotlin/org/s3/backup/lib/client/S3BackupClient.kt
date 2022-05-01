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

/**
 * Request based style API
 */
class S3BackupClient {

    /**
     * Will find given file by full path in given backup and return input stream for download.
     *
     * Download File Request example:
     *
     * <pre>
     * {@code
     * downloadFileFromBackupRequest {
     *   backupContext {
     *     bucketName = "test.bucket"
     *     backupKey = "1651394085028"
     *   }
     *   filePathToDownload = "/subdir/file.txt"
     * }
     * }
     * </pre>
     *
     * @param downloadFileRequest
     * @return DownloadFileFromBackupResponse object that contains inputStream
     * @throws Exception
     *         may throw exceptions
     */
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

    /**
     * Will restore backup from given request.
     *
     * Restore Backup Request example:
     *
     * <pre>
     * {@code
     * restoreFromBackupRequest {
     *   backupContext {
     *     bucketName = "test.bucket"
     *     backupKey = "1651394085028"
     *   }
     *   destinationDirectory = "/home/user/restore"
     * }
     * }
     * </pre>
     *
     * @param restoreBackupRequest
     * @throws Exception
     *         may throw exceptions
     */
    fun restoreFromBackup(restoreBackupRequest: RestoreFromBackupRequest) =
        RestoreUtility.doRestore(
            restoreBackupRequest.context.backupMetadata,
            restoreBackupRequest.context.bucketName,
            restoreBackupRequest.destinationDirectory
        )

    /**
     * Will list all available backups from given request.
     *
     * List All Backups Request example:
     *
     * <pre>
     * {@code
     * listAllBackupsRequest {
     *   bucketName = "test.bucket"
     * }
     * }
     * </pre>
     *
     * @param listAllBackupsRequest
     * @return ListAllBackupsResponse containing all backup keys available in bucket
     * @throws Exception
     *         may throw exceptions
     */
    fun listAllBackups(listAllBackupsRequest: ListAllBackupsRequest) =
        ListAllBackupsResponse(S3Client.collectMetadataEntriesFromBucket(listAllBackupsRequest.bucketName))

    /**
     * Will list backup content from given request.
     *
     * List Backup Content Request example:
     *
     * <pre>
     * {@code
     * istBackupContentRequest {
     *   backupContext {
     *     bucketName = "test.bucket"
     *     backupKey = "1651394085028"
     *   }
     * }
     * }
     * </pre>
     *
     * @param listBackupContentRequest
     * @return ListBackupContentResponse containing all file paths from backup
     * @throws Exception
     *         may throw exceptions
     */
    fun listBackupContent(listBackupContentRequest: ListBackupContentRequest) =
        ListBackupContentResponse(listBackupContentRequest.backupContext.backupMetadata.pathList())

    /**
     * Will restore backup from given request.
     *
     * Restore Backup Request example:
     *
     * <pre>
     * {@code
     * doBackupRequest {
     *   bucketName = "test.bucket"
     *   directoryToBackup = "/home/user/backup_this"
     *   dryRun = false
     * }
     * }
     * </pre>
     *
     * When dryRun is set to true, will not upload data to S3, just print out some stats.
     *
     * @param doBackupRequest
     * @throws Exception
     *         may throw exceptions
     */
    fun doBackup(doBackupRequest: DoBackupRequest) {
        BackupUtility.doBackup(
            File(doBackupRequest.directoryToBackup),
            doBackupRequest.bucketName,
            doBackupRequest.isDryRun
        )
    }

    companion object {
        /**
         * Creates instance of S3BackupClient
         *
         * @return new instance of S3BackupClient
         */
        fun create(): S3BackupClient {
            return S3BackupClient()
        }
    }
}
