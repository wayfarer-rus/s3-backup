package org.s3.backup.lib.utilities

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.s3.backup.lib.metadata.model.DirMetadata
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.metadata.model.MetadataNode
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.FileUpload
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.UploadFileRequest
import java.io.File
import java.nio.file.Files
import java.util.Stack

private val logger = KotlinLogging.logger {}

object S3BackupUtility {
    // the main idea is that we have to create some kind of metadata
    // that will allow us to handle delta-backups
    fun doBackup(directory: File, bucketName: String, dryRun: Boolean = false) {
        val currentTimestamp = System.currentTimeMillis()
        val metadataFileName = "$currentTimestamp.metadata"
        val zipFileName = "$currentTimestamp.zip"
        val metadataFileFullPath = "${getTempDir()}$metadataFileName"
        val zipFileFullPath = "${getTempDir()}$zipFileName"

        logger.info { "access bucket and download latest metadata file..." }
        val latestBackupMetadata: MetadataNode? = downloadLatestMetadata(bucketName)
        logger.info { "create metadata for selected directory" }
        val freshMetadata = collectMetadata(directory)

        // compare root of the bucket metadata with current root
        if (latestBackupMetadata != null && freshMetadata.name == latestBackupMetadata.name) {
            logger.info { "calculate delta" }
            val newOrUpdatedFiles = updateOldFilesAndFindNewFiles(latestBackupMetadata, freshMetadata)

            // if there are no mew checksums - exit with "nothing to do message"
            if (newOrUpdatedFiles.isEmpty()) {
                logger.info { "nothing to do; no updates" }
                return
            }

            // 4) for new checksums create new zip-file
            if (!dryRun) {
                zipNewFiles(zipFileFullPath, zipFileName, newOrUpdatedFiles)
            }
        } else {
            // completely new backup
            // compress it to Zip
            if (!dryRun) {
                zipNewFiles(zipFileFullPath, zipFileName, freshMetadata.filesList())
            }
        }

        val data = Json.encodeToString(freshMetadata)
        logger.debug { "metadata file will be created here: $metadataFileFullPath" }

        if (!dryRun) {
            File(metadataFileFullPath).also { it.writeText(data) }
        }

        // 5) upload zip file with new metadata file to the cloud
        if (!dryRun) {
            uploadToCloud(bucketName, File(metadataFileFullPath), File(zipFileFullPath))
        }

        logger.info { "done" }
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
        fileCards: List<FileMetadata>
    ) {
        // collect files distinct by the checksum
        // this is new files. All new files will have new zip file name as reference
        val distinctFileCards = fileCards
            .onEach {
                it.archiveLocationRef.archiveName = zipFileName
            }
            .distinctBy { it.checksum }

        logger.debug { "zip archive will be created here: $zipFileFullPath" }
        logger.info { "compression in progress. Please wait..." }
        ZipUtility.zipFiles(distinctFileCards, zipFileFullPath)
        logger.info { "compression complete; updating metadata..." }
        val zipFilesOffsetsMap = ZipUtility.offsetsMapFromZipFile(zipFileFullPath)
        fileCards.forEach {
            it.archiveLocationRef.zipLfhLocation = zipFilesOffsetsMap[it.checksum]
        }
    }

    // TODO: take a look at https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-glacier.html
    private fun downloadLatestMetadata(bucketName: String): MetadataNode? {
        val (s3, entries) = collectMetadataEntriesFromBucket(bucketName)

        return entries.maxByOrNull {
            it.toLong()
        }?.let<String, MetadataNode> { key ->
            // download latest metadata file
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName).key(key).build()

            // all bytes should be fine here, while metadata likely will not be more than 2G
            val metadataObj = s3.getObject(getObjectRequest).readAllBytes().toString(Charsets.UTF_8)
            Json.decodeFromString(metadataObj)
        }
    }

    private fun collectMetadataEntriesFromBucket(bucketName: String): Pair<S3Client, List<String>> {
        val s3 = S3Client.create()

        // find latest metadata file
        val metadataFileNameMatcher = "(\\d{13})\\.metadata".toRegex()
        val listObjectsRequest = ListObjectsV2Request
            .builder()
            .bucket(bucketName)
            .build()

        val entries = s3.listObjectsV2Paginator(listObjectsRequest)
            .contents()
            .filter { metadataFileNameMatcher.matches(it.key()) }
            .mapNotNull { metadataFileNameMatcher.find(it.key())?.destructured?.component1() }
        return Pair(s3, entries)
    }

    private fun uploadToCloud(bucketName: String, metadataFile: File, zipFile: File) {
        logger.info { "uploading to S3..." }
        val uploadFileRequest: (File, String) -> (UploadFileRequest.Builder) -> Unit = { file, bucket ->
            { b: UploadFileRequest.Builder ->
                b.source(file)
                    .putObjectRequest { req: PutObjectRequest.Builder ->
                        req.bucket(bucket).key(file.name)
                    }
            }
        }

        val transferManager = S3TransferManager.create()
        val metadataUpload = transferManager.uploadFile(uploadFileRequest(metadataFile, bucketName))
        val zipUpload = transferManager.uploadFile(uploadFileRequest(zipFile, bucketName))

        metadataUpload.printProgress(metadataFile.name)
        zipUpload.printProgress(zipFile.name)
        metadataUpload.completionFuture().join()
        zipUpload.completionFuture().join()
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

    fun listAllBackups(bucketName: String): List<String> {
        val (_, entries) = collectMetadataEntriesFromBucket(bucketName)
        return entries
    }

    fun listBackupContent(bucketName: String, backupKey: String): List<String> {
        val (_, backupMetadata) = downloadBackupMetadata(bucketName, backupKey)
        return backupMetadata?.pathList()
            ?: error("Object not found in bucket '$bucketName' by key '$backupKey'")
    }

    fun downloadFileFromBackup(bucketName: String, backupKey: String, filePathToDownload: String, pathDest: String) {
        if (!File(pathDest).let { it.isDirectory && it.canWrite() }) {
            error("'$pathDest' must be a writeable directory")
        }

        val (s3, backupMetadata) = downloadBackupMetadata(bucketName, backupKey)
        val fileNode = backupMetadata?.filesList()?.find { it.path == filePathToDownload }
            ?: error("File ($filePathToDownload) not found in backup $backupKey from bucket $bucketName")

        val zippedFileInputStream = s3.getObject(
            getObjectRequest(
                bucketName,
                fileNode.archiveLocationRef.archiveName
            ).range(fileNode.archiveLocationRef.toRangeString()).build()
        ).buffered()

        ZipUtility.unzipOneFileToDestination(
            zippedFileInputStream,
            File("$pathDest/${File(filePathToDownload).name}")
        )
    }

    private fun downloadBackupMetadata(
        bucketName: String,
        backupKey: String
    ): Pair<S3Client, MetadataNode?> {
        val s3 = S3Client.create()
        val getObjectRequest = getObjectRequest(bucketName, "$backupKey.metadata").build()
        val backupMetadata = s3.getObject(getObjectRequest)
            ?.readAllBytes()
            ?.toString(Charsets.UTF_8)
            ?.let { json ->
                Json.decodeFromString<MetadataNode>(json)
            }
        return Pair(s3, backupMetadata)
    }

    fun restoreBackup(bucketName: String, backupKey: String, pathDest: String) {
        if (!File(pathDest).let { it.isDirectory && it.canWrite() }) {
            error("'$pathDest' must be a writeable directory")
        }

        val (s3, backupMetadata) = downloadBackupMetadata(bucketName, backupKey)
        if (backupMetadata == null) error("backup $backupKey not found in $bucketName")

        val decompressedData = backupMetadata.filesList()
            .groupBy { it.archiveLocationRef.archiveName }
            .mapValues {
                val rangePair = it.value.fold(Pair<Long, Long>(Long.MAX_VALUE, 0)) { acc, fileMeta ->
                    val offset = fileMeta.archiveLocationRef.zipLfhLocation?.offset ?: 0
                    val length = fileMeta.archiveLocationRef.zipLfhLocation?.length ?: 0
                    kotlin.math.min(acc.first, offset) to kotlin.math.max(acc.second, offset + length)
                }

                "bytes=${rangePair.first}-${rangePair.second}"
            }.map { (archiveName, rangeToDownload) ->
                s3.getObject(
                    getObjectRequest(
                        bucketName,
                        archiveName
                    ).range(rangeToDownload).build()
                ).buffered()
            }.flatMap {
                ZipUtility.unzipMultipleFilesToDestination(it, File(getTempDir())).entries
            }.associate {
                it.key to it.value
            }

        backupMetadata.filesList().forEach {
            it.localFileRef = decompressedData[it.checksum]
        }

        backupMetadata.writeToDisk(pathDest)
    }

    private fun getObjectRequest(
        bucketName: String,
        backupKey: String
    ): GetObjectRequest.Builder = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(backupKey)
}

val fuLogger = KotlinLogging.logger("FileUpload")
private fun FileUpload.printProgress(keyName: String) {
    while (!this.completionFuture().isDone) {
        val transferred = this.progress().snapshot().bytesTransferred()
        val size: Long = this.progress().snapshot().transferSizeInBytes().orElse(0)

        fuLogger.info { "$keyName: $transferred/$size" }

        Thread.sleep(1000)
    }
}
