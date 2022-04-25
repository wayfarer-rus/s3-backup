package org.s3.backup.lib.client

import mu.KotlinLogging
import org.s3.backup.lib.client.request.DoBackupRequest
import org.s3.backup.lib.client.request.DownloadFileFromBackupRequest
import org.s3.backup.lib.client.request.ListAllBackupsRequest
import org.s3.backup.lib.client.request.ListBackupContentRequest
import org.s3.backup.lib.client.request.RestoreFromBackupRequest
import org.s3.backup.lib.client.response.DownloadFileFromBackupResponse
import org.s3.backup.lib.client.response.ListAllBackupsResponse
import org.s3.backup.lib.client.response.ListBackupContentResponse
import org.s3.backup.lib.utilities.S3BackupUtility
import org.s3.backup.lib.utilities.ZipUtility
import org.s3.backup.lib.utilities.getTempDir
import java.io.File

private val logger = KotlinLogging.logger {}

class S3BackupClient {
    // will find build context and download given file by given path
    fun downloadFromBackup(downloadFileRequest: DownloadFileFromBackupRequest): DownloadFileFromBackupResponse {
        // find file in metadata
        val fileNode = downloadFileRequest.context.backupMetadata.filesList()
            .find { it.path == downloadFileRequest.filePathToDownload }
            ?: error("File '${downloadFileRequest.filePathToDownload}' wasn't found in ${downloadFileRequest.context}.")

        // create temp file that will hold uncompressed data
        val tmpFile: File = File.createTempFile(
            downloadFileRequest.context.backupKey,
            downloadFileRequest.context.bucketName
        )

        logger.debug { "decompressing downloaded file into temporary file" }
        ZipUtility.unzipOneFileToDestination(
            fileInputStream = S3BackupUtility.bufferedInputStreamFromFileNode(
                downloadFileRequest.context.bucketName,
                fileNode
            ),
            destination = tmpFile
        )

        // return temp file as input stream to the caller
        return DownloadFileFromBackupResponse(
            inputStream = tmpFile.inputStream()
        )
    }

    fun restoreFromBackup(restoreBackupRequest: RestoreFromBackupRequest) {
        val backupMetadata = restoreBackupRequest.context.backupMetadata
        val decompressedData = backupMetadata.filesList()
            // group all files from metadata by its archive residence
            .groupBy { it.archiveLocationRef.archiveName }
            // calculate range for each archive that must be downloaded
            // TODO:: here we can make better algorithm by comparing prices for bytes downloaded VS amount of requests to the S3
            .mapValues {
                val rangePair = it.value.fold(Pair<Long, Long>(Long.MAX_VALUE, 0)) { acc, fileMeta ->
                    val offset = fileMeta.archiveLocationRef.zipLfhLocation?.offset ?: 0
                    val length = fileMeta.archiveLocationRef.zipLfhLocation?.length ?: 0
                    kotlin.math.min(acc.first, offset) to kotlin.math.max(acc.second, offset + length)
                }

                "bytes=${rangePair.first}-${rangePair.second}"
            }
            // now we have all ranges for all archives that we now must download
            .map { (archiveName, rangeToDownload) ->
                logger.debug { "downloading range ($rangeToDownload) from $archiveName" }
                S3BackupUtility.bufferedInputStreamFromFileWithRange(
                    bucketName = restoreBackupRequest.context.bucketName,
                    fileName = archiveName,
                    rangeToDownload
                )
            }
            // here we do the decompression of all files from range to the temp directory
            // at this point all files will have wierd names
            .flatMap {
                logger.debug { "decompressing downloaded content to temp directory: ${getTempDir()}" }
                ZipUtility.unzipMultipleFilesToDestination(it, File(getTempDir())).entries
            }
            // associate all downloaded files by their wierd names (sha-256) with actual file
            .associate {
                it.key to it.value
            }

        // apply content from temp directory to metadata
        backupMetadata.filesList().forEach {
            it.localFileRef = decompressedData[it.checksum]
        }

        logger.debug { "copy data from temp directory to the given output directory: ${restoreBackupRequest.destinationDirectory}" }
        backupMetadata.writeToDisk(restoreBackupRequest.destinationDirectory)
    }

    fun listAllBackups(listAllBackupsRequest: ListAllBackupsRequest): ListAllBackupsResponse {
        return ListAllBackupsResponse(S3BackupUtility.collectMetadataEntriesFromBucket(listAllBackupsRequest.bucketName))
    }

    fun listBackupContent(listBackupContentRequest: ListBackupContentRequest): ListBackupContentResponse {
        val pathList = listBackupContentRequest.backupContext.backupMetadata.pathList()
        return ListBackupContentResponse(pathList)
    }

    fun doBackup(doBackupRequest: DoBackupRequest) {
        S3BackupUtility.doBackup(
            File(doBackupRequest.directoryToBackup),
            doBackupRequest.bucketName,
            doBackupRequest.isDryRun
        )
    }

    companion object {
        fun create(): S3BackupClient {
            return S3BackupClient()
        }
    }
}
