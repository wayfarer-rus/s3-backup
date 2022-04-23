package org.s3.backup.lib.zip.model

import org.s3.backup.lib.utilities.readNBytes
import org.s3.backup.lib.utilities.toInt
import org.s3.backup.lib.utilities.toLong
import java.io.RandomAccessFile

fun readCdfhSignature(fis: RandomAccessFile, offset: Long): CdfhSignature {
    fis.seek(offset)
    val buff = fis.readNBytes(4)

    if (!buff.contentEquals(CdfhSignature.cdfhSignatureByteArray)) {
        error("Invalid CDFH signature")
    }

    var fileNameLength: Int
    var extraFieldLength: Int
    var commentsLength: Int
    return CdfhSignature(
        versionMadeBy = fis.readNBytes(2).reversedArray().toInt(),
        versionToExtract = fis.readNBytes(2).reversedArray().toInt(),
        generalPurposeBitFlag = fis.readNBytes(2).reversedArray(),
        compressionMethod = fis.readNBytes(2).reversedArray().toInt(),
        lastModificationTime = fis.readNBytes(2).reversedArray().toInt(),
        lastModificationDate = fis.readNBytes(2).reversedArray().toInt(),
        checkSum = fis.readNBytes(4).reversedArray().toInt(),
        compressedSizeBytes = fis.readNBytes(4),
        uncompressedSizeBytes = fis.readNBytes(4),
        fileNameLength = fis.readNBytes(2).reversedArray().toInt().also { fileNameLength = it },
        extraFieldLength = fis.readNBytes(2).reversedArray().toInt().also { extraFieldLength = it },
        fileCommentLength = fis.readNBytes(2).reversedArray().toInt().also { commentsLength = it },
        diskNumberWhereFileStarts = fis.readNBytes(2).reversedArray().toInt(),
        internalFileAttributes = fis.readNBytes(2).reversedArray(),
        externalFileAttributes = fis.readNBytes(4).reversedArray(),
        fileOffsetBytes = fis.readNBytes(4),
        fileName = fis.readNBytes(fileNameLength).toString(Charsets.UTF_8),
        extraField = ExtendedInformation(fis.readNBytes(extraFieldLength)),
        fileComment = fis.readNBytes(commentsLength).toString(Charsets.UTF_8)
    )
}

class CdfhSignature(
    val versionMadeBy: Int,
    val versionToExtract: Int,
    val generalPurposeBitFlag: ByteArray,
    val compressionMethod: Int,
    val lastModificationTime: Int,
    val lastModificationDate: Int,
    val checkSum: Int,
    private val compressedSizeBytes: ByteArray,
    private val uncompressedSizeBytes: ByteArray,
    val fileNameLength: Int,
    val extraFieldLength: Int,
    val fileCommentLength: Int,
    val diskNumberWhereFileStarts: Int,
    val internalFileAttributes: ByteArray,
    val externalFileAttributes: ByteArray,
    private val fileOffsetBytes: ByteArray,
    val fileName: String,
    val extraField: ExtendedInformation,
    val fileComment: String,
) {
    var compressedSize: Long
    var uncompressedSize: Long
    var fileOffset: Long

    init {
        var ind = 0
        uncompressedSize = safeGetLongValueFromByteArrayOrDefault(uncompressedSizeBytes) {
            if (extraField.isValid())
                this.extraField[ind++]
            else null
        } ?: error(errorMsg)

        compressedSize = safeGetLongValueFromByteArrayOrDefault(compressedSizeBytes) {
            if (extraField.isValid())
                this.extraField[ind++]
            else null
        } ?: error(errorMsg)

        fileOffset = safeGetLongValueFromByteArrayOrDefault(fileOffsetBytes) {
            if (this.extraField.isValid())
                this.extraField[ind++]
            else null
        } ?: error(errorMsg)
    }

    fun byteSize(): Int {
        return DEFAULT_BYTE_SIZE + fileNameLength + extraFieldLength + fileCommentLength
    }

    private fun safeGetLongValueFromByteArrayOrDefault(bytes: ByteArray, default: () -> Long?) =
        if (bytes.all { it == 0xFF.toByte() }) {
            // means that actual value is in extended information
            default()
        } else {
            bytes.reversedArray().toLong()
        }

    companion object {
        private const val errorMsg = "Invalid Contend Directory File Header structure"
        const val DEFAULT_BYTE_SIZE = 46

        // Central directory file header signature = 0x02014b50
        // in little-endian format
        val cdfhSignatureByteArray = byteArrayOf(0x02, 0x01, 0x4b, 0x50).reversedArray()
    }
}
