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
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.utilities.BackupUtility
import org.s3.backup.lib.utilities.copyNBytesToOutputStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File

private val logger = KotlinLogging.logger {}

class S3BackupClient {
    // will find build context and download given file by given path
    fun downloadFromBackup(downloadFileRequest: DownloadFileFromBackupRequest): DownloadFileFromBackupResponse {
        // find file in metadata
        val fileNode = downloadFileRequest.context.backupMetadata.filesList()
            .find { it.path == downloadFileRequest.filePathToDownload }
            ?: error("File '${downloadFileRequest.filePathToDownload}' wasn't found in ${downloadFileRequest.context}.")

        val bufferedInputStream = if (fileNode.size == 0L) {
            ByteArrayInputStream(ByteArray(0))
        } else {
            BackupUtility.bufferedInputStreamFromFileNode(
                downloadFileRequest.context.bucketName,
                fileNode
            )
        }

        // return temp file as input stream to the caller
        return DownloadFileFromBackupResponse(
            inputStream = bufferedInputStream
        )
    }

    fun restoreFromBackup(restoreBackupRequest: RestoreFromBackupRequest) {
        val backupMetadata = restoreBackupRequest.context.backupMetadata
        // group all files from metadata by its archive residence
        val fileNodesGroupedByBackup = backupMetadata.filesList()
            .sortedBy { it.archiveLocationRef.zipLfhLocation?.offset }
            .groupBy { it.archiveLocationRef.archiveName }
        val rangeForBackup: MutableMap<String, Pair<Long, Long>> = mutableMapOf()
        val dataStreamsByBackupMap = fileNodesGroupedByBackup
            // calculate range for each archive that must be downloaded
            .mapValues { entry ->
                val folded = entry.value
                    .distinctBy { it.checksum }
                    .map {
                        val offset = it.archiveLocationRef.zipLfhLocation?.offset ?: 0
                        var length = it.archiveLocationRef.zipLfhLocation?.length ?: 0
                        // range is inclusive
                        if (length > 0) length--
                        offset to offset + length
                    }.fold(Pair<Long, Long>(Long.MAX_VALUE, 0)) { acc, next ->
                        // calculating minival ranges is futile
                        // S3 will return us min-max anyway
                        kotlin.math.min(acc.first, next.first) to kotlin.math.max(acc.second, next.second)
                    }
                // remember this range
                rangeForBackup[entry.key] = folded
                "bytes=${folded.first}-${folded.second}"
            }
            // now we have all ranges for all archives that we have to download
            .mapValues { (archiveName, rangeToDownload) ->
                logger.debug { "downloading range ($rangeToDownload) from $archiveName" }
                S3Client.bufferedInputStreamFromFileWithRange(
                    bucketName = restoreBackupRequest.context.bucketName,
                    fileName = archiveName,
                    rangeToDownload
                )
            }

        // write to disk
        flushToDisk(
            restoreBackupRequest.destinationDirectory,
            fileNodesGroupedByBackup,
            dataStreamsByBackupMap,
            rangeForBackup
        )

        logger.debug { "closing all opened streams..." }
        dataStreamsByBackupMap.forEach { (_, stream) -> stream.close() }
        logger.debug { "restoration complete" }
    }

    private fun flushToDisk(
        destinationDirectory: String,
        // file nodes ordered by offset and grouped by backup number
        fileNodesGroupedByBackup: Map<String, List<FileMetadata>>,
        // data streams for requested backups
        dataStreamsByBackupMap: Map<String, BufferedInputStream>,
        // requested range values for backups
        // needed to calculate correct offset in data stream
        rangeForBackup: MutableMap<String, Pair<Long, Long>>
    ) {
        val extractedFilesMap: MutableMap<String, File> = mutableMapOf()

        fileNodesGroupedByBackup.forEach { (backupKey, fileNodes) ->
            logger.debug { "reading data from $backupKey backup..." }
            var currentOffset = 0L

            fileNodes.forEach { fileNode ->
                // create file object pointing to specified location
                val f = File(destinationDirectory + fileNode.path)
                logger.debug { "extracting file ${f.name} to path: ${f.path}" }
                // make directories
                f.parentFile.mkdirs()

                // if file is an empty file just create it
                if (fileNode.size == 0L) f.createNewFile()
                else if (extractedFilesMap[fileNode.checksum] != null) {
                    extractedFilesMap[fileNode.checksum]?.copyTo(f, true)
                }
                // otherwise we read its content out of the stream from cloud
                else {
                    dataStreamsByBackupMap[backupKey]?.let { backupDataStream ->
                        // calculate offset in stream
                        val offset =
                            (fileNode.archiveLocationRef.zipLfhLocation?.offset ?: 0L) -
                                (rangeForBackup[backupKey]?.first ?: 0L)

                        // we do need while loop here, while BufferedInputStream reads in chunks
                        while (offset > currentOffset) {
                            currentOffset += backupDataStream.skip(offset - currentOffset)
                        }

                        val size = fileNode.size

                        f.outputStream().use { fos ->
                            backupDataStream.copyNBytesToOutputStream(fos, size)
                        }
                        currentOffset += size
                    }
                }
                // remember file by checksum
                extractedFilesMap[fileNode.checksum] = f
            }
        }
    }

    fun listAllBackups(listAllBackupsRequest: ListAllBackupsRequest): ListAllBackupsResponse {
        return ListAllBackupsResponse(S3Client.collectMetadataEntriesFromBucket(listAllBackupsRequest.bucketName))
    }

    fun listBackupContent(listBackupContentRequest: ListBackupContentRequest): ListBackupContentResponse {
        val pathList = listBackupContentRequest.backupContext.backupMetadata.pathList()
        return ListBackupContentResponse(pathList)
    }

    fun doBackup(doBackupRequest: DoBackupRequest) {
        BackupUtility.doBackup(
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
