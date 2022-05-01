package org.s3.backup.lib.client

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.s3.backup.lib.DataBlobPublisher
import org.s3.backup.lib.metadata.model.MetadataNode
import org.s3.backup.lib.validators.BackupValidators
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.transfer.s3.ObjectTransfer
import software.amazon.awssdk.transfer.s3.S3TransferManager

private val logger = KotlinLogging.logger {}
const val MAX_PART_SIZE = 5L * 1024L * 1024L * 1024L // 5 GiB
const val DEFAULT_PART_SIZE = 8 * 1024 * 1024L // 8 MiB
const val MAX_PARTS = 10_000

// all real operations with S3 contained here.
internal object S3Client {
    // create s3 client from default environment variables
    // good enough for now
    private val s3 = software.amazon.awssdk.services.s3.S3Client.create()

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
        logger.info { "trying to load metadata $backupKey from bucket $bucketName" }
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

    // note range downloads are limited with 1 range
    // you can't download multiple ranges with 1 request
    // complete object will be returned instead
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

    fun uploadToCloud(
        bucketName: String,
        metadataKey: String,
        metadata: MetadataNode,
        dataBlobPublisher: DataBlobPublisher
    ) {
        if (dataBlobPublisher.estimatedContentLength > MAX_PARTS * MAX_PART_SIZE) {
            error("Can't upload more than ${MAX_PARTS * MAX_PART_SIZE} bytes")
        }
        logger.info { "uploading to S3..." }
        val transferManager = S3TransferManager
            .builder().s3ClientConfiguration {
                it.minimumPartSizeInBytes(
                    kotlin.math.max(
                        DEFAULT_PART_SIZE,
                        dataBlobPublisher.estimatedContentLength / MAX_PARTS
                    )
                )
            }.build()

        val blobUpload = transferManager.upload { u ->
            u.putObjectRequest { porb ->
                porb.bucket(bucketName).key(dataBlobPublisher.name)
                    .contentLength(dataBlobPublisher.estimatedContentLength)
            }
            u.requestBody(AsyncRequestBody.fromPublisher(dataBlobPublisher))
        }
        // we have to await while blob is completely uploaded to calculate offsets
        blobUpload.printProgress(dataBlobPublisher.name, dataBlobPublisher.estimatedContentLength)

        val metadataUpload = transferManager.upload { u ->
            u.putObjectRequest { or ->
                or.bucket(bucketName).key(metadataKey)
            }
            u.requestBody(AsyncRequestBody.fromString(Json.encodeToString(metadata)))
        }
        metadataUpload.printProgress(metadata.name)
    }

    private fun getObjectRequest(
        bucketName: String,
        backupKey: String
    ): GetObjectRequest.Builder = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(backupKey)

    fun deleteBackup(bucketName: String, backupKey: String): Boolean {
        val listObjectsRequest = ListObjectsV2Request
            .builder()
            .bucket(bucketName)
            .build()

        val objects = s3.listObjectsV2Paginator(listObjectsRequest)
            .contents()
            .filter { it.key() == backupKey || it.key().startsWith(backupKey) }

        return s3.deleteObjects { req ->
            req.bucket(bucketName).delete { deleteBuilder ->
                deleteBuilder.objects(objects.map { ObjectIdentifier.builder().key(it.key()).build() })
            }
        }.hasDeleted()
    }
}

val fuLogger = KotlinLogging.logger("FileUpload")
private fun ObjectTransfer.printProgress(keyName: String, estimatedSize: Long? = null) {
    while (!this.completionFuture().isDone) {
        val transferred = this.progress().snapshot().bytesTransferred()
        val size: Long = estimatedSize ?: this.progress().snapshot().transferSizeInBytes().orElse(0)

        fuLogger.info { "$keyName: $transferred/$size" }

        Thread.sleep(1000)
    }
}
