package org.s3.backup.lib.client

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.s3.backup.lib.metadata.model.MetadataNode
import org.s3.backup.lib.validators.BackupValidators
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.FileUpload
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.UploadFileRequest
import java.io.File

private val logger = KotlinLogging.logger {}

// all real operations with S3 contained here.
internal object S3Client {
    // create s3 client from default environment varialbles
    // good enough for now
    private val s3 = software.amazon.awssdk.services.s3.S3Client.create()
    private val transferManager = S3TransferManager.create()

    fun collectMetadataEntriesFromBucket(bucketName: String): List<String> {
        // find latest metadata file
        val listObjectsRequest = ListObjectsV2Request
            .builder()
            .bucket(bucketName)
            .build()

        return s3.listObjectsV2Paginator(listObjectsRequest)
            .contents()
            .mapNotNull { BackupValidators.backupMetadataKeyRegex.find(it.key())?.destructured?.component1() }
    }

    fun downloadBackupMetadata(
        bucketName: String,
        backupKey: String
    ): MetadataNode? {
        logger.debug { "trying to load metadata $backupKey from bucket $bucketName" }
        val getObjectRequest = getObjectRequest(bucketName, "$backupKey.metadata").build()
        val backupMetadata = s3.getObject(getObjectRequest)
            ?.readAllBytes()
            ?.toString(Charsets.UTF_8)
            ?.let { json ->
                logger.debug { "unmarshalling from JSON format to MetadataNode" }
                Json.decodeFromString<MetadataNode>(json)
            }
        logger.debug { "download successful" }
        return backupMetadata
    }

    fun bufferedInputStreamFromFileWithRange(
        bucketName: String,
        fileName: String,
        rangeToDownload: String
    ) = s3.getObject(
        getObjectRequest(
            bucketName,
            fileName
        ).range(rangeToDownload).build()
    ).buffered()

    fun uploadToCloud(bucketName: String, metadataFile: File, zipFile: File) {
        logger.info { "uploading to S3..." }
        val uploadFileRequest: (File, String) -> (UploadFileRequest.Builder) -> Unit = { file, bucket ->
            { b: UploadFileRequest.Builder ->
                b.source(file)
                    .putObjectRequest { req: PutObjectRequest.Builder ->
                        req.bucket(bucket).key(file.name)
                    }
            }
        }

        val metadataUpload = transferManager.uploadFile(uploadFileRequest(metadataFile, bucketName))
        val zipUpload = transferManager.uploadFile(uploadFileRequest(zipFile, bucketName))

        metadataUpload.printProgress(metadataFile.name)
        zipUpload.printProgress(zipFile.name)
        metadataUpload.completionFuture().join()
        zipUpload.completionFuture().join()
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
