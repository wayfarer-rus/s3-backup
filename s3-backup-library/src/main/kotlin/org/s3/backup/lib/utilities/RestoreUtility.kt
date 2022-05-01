package org.s3.backup.lib.utilities

import mu.KotlinLogging
import org.s3.backup.lib.client.S3Client
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.metadata.model.MetadataNode
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

private val logger = KotlinLogging.logger {}

internal object RestoreUtility {
    fun inputStreamForFileNode(bucketName: String, fileNode: FileMetadata): InputStream {
        if (fileNode.archiveLocationRef.archiveName.isBlank() ||
            fileNode.archiveLocationRef.fileLocation.isInvalid() ||
            fileNode.archiveLocationRef.fileLocation.length != fileNode.size
        ) {
            error("Invalid File Node state: ${fileNode.archiveLocationRef}")
        }

        return if (fileNode.size == 0L) {
            ByteArrayInputStream(ByteArray(0))
        } else {
            S3Client.bufferedInputStreamFromFileWithRange(
                bucketName,
                fileNode.archiveLocationRef.archiveName,
                fileNode.archiveLocationRef.toRangeString()
            )
        }
    }

    fun doRestore(
        metadata: MetadataNode,
        bucketName: String,
        destinationDirectory: String
    ) {
        // group all files from metadata by its archive residence
        val fileNodesGroupedByBackup = metadata.filesList()
            .sortedBy { it.archiveLocationRef.fileLocation.offset }
            .groupBy { it.archiveLocationRef.archiveName }
        val rangeForBackup: MutableMap<String, Pair<Long, Long>> = mutableMapOf()
        val dataStreamsByBackupMap = fileNodesGroupedByBackup
            // calculate range for each archive that must be downloaded
            .mapValues { entry ->
                val folded = entry.value
                    .distinctBy { it.checksum }
                    .map {
                        val offset = it.archiveLocationRef.fileLocation.offset
                        var length = it.archiveLocationRef.fileLocation.length
                        // range is inclusive
                        if (length > 0) length--
                        offset to offset + length
                    }.fold(Pair<Long, Long>(Long.MAX_VALUE, 0)) { acc, next ->
                        // calculating minimal ranges is futile
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
                    bucketName = bucketName,
                    fileName = archiveName,
                    rangeToDownload
                )
            }

        // write to disk
        flushToDisk(
            destinationDirectory,
            fileNodesGroupedByBackup,
            dataStreamsByBackupMap,
            rangeForBackup
        )

        logger.info { "closing all opened streams..." }
        dataStreamsByBackupMap.forEach { (_, stream) -> stream.close() }
        logger.info { "restoration complete" }
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
            logger.info { "reading data from $backupKey backup..." }
            var currentOffset = 0L

            fileNodes.forEach { fileNode ->
                // create file object pointing to specified location
                val f = File(destinationDirectory + fileNode.path)
                logger.info { "extracting file ${f.name} to path: ${f.path}" }
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
                            fileNode.archiveLocationRef.fileLocation.offset -
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
}
