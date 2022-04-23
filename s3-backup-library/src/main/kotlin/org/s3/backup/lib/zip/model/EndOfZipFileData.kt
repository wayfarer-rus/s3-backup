package org.s3.backup.lib.zip.model

import org.s3.backup.lib.utilities.readNBytes
import org.s3.backup.lib.utilities.toInt
import org.s3.backup.lib.utilities.toLong
import java.io.RandomAccessFile

fun readEndOfZipFileData(fis: RandomAccessFile, fileSize: Long): EndOfZipFileData {
    fis.seek(fileSize - EndOfZipFileData.SIZE_BYTES)
    val buff = fis.readNBytes(4)

    if (!buff.contentEquals(EndOfZipFileData.eofSignatureByteArray)) {
        error("Invalid Signature for End Of Zip File")
    }

    var commentSize: Int
    val result = EndOfZipFileData(
        numberOfDisks = fis.readNBytes(2).reversedArray().toInt(),
        diskWhereCdStarts = fis.readNBytes(2).reversedArray().toInt(),
        numberOfCdrOnDisk = fis.readNBytes(2).reversedArray().toInt(),
        totalNumberOfCdr = fis.readNBytes(2).reversedArray().toInt(),
        sizeOfCd = fis.readNBytes(4).reversedArray().toLong(),
        offsetOfStartOfCd = fis.readNBytes(4).reversedArray(),
        commentLength = fis.readNBytes(2).reversedArray().toInt().also { commentSize = it },
        comment = fis.readNBytes(commentSize).toString(Charsets.UTF_8),
    )

    if (result.incomplete()) {
        fis.seek(fileSize - EndOfZipFileData.EXTENDED_BYTES)

        if (fis.readNBytes(4).contentEquals(EndOfZipFileData.extEofSignatureByteArray)) {
            // we hit the extended attributes
            // skip to the point
            fis.skipBytes(36)
            result.sizeOfCd = fis.readNBytes(8).reversedArray().toLong()
            result.offsetOfStartOfCd = fis.readNBytes(8).reversedArray()
        } else {
            error("Invalid Signature for End Of Zip File")
        }
    }

    return result
}

class EndOfZipFileData(
    val numberOfDisks: Int,
    val diskWhereCdStarts: Int,
    val numberOfCdrOnDisk: Int,
    val totalNumberOfCdr: Int,
    var sizeOfCd: Long,
    var offsetOfStartOfCd: ByteArray,
    val commentLength: Int,
    val comment: String,
) {
    // if all bytes are FF
    // this means, that offset data stored in extended attributes
    fun incomplete() = offsetOfStartOfCd.all { it == 0xFF.toByte() }

    companion object {
        const val SIZE_BYTES = 22
        const val EXTENDED_BYTES = 22 + 56 + 20

        // End of central directory signature = 0x06054b50
        // in little-endian format
        val eofSignatureByteArray = byteArrayOf(0x06, 0x05, 0x4b, 0x50).reversedArray()

        // Extended end of central directory signature 0x06064b50
        val extEofSignatureByteArray = byteArrayOf(0x06, 0x06, 0x4b, 0x50).reversedArray()
    }
}
