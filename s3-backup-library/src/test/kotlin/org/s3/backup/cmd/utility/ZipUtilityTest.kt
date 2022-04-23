package org.s3.backup.cmd.utility

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.utilities.ZipUtility
import org.s3.backup.lib.utilities.sha256
import org.s3.backup.lib.utilities.toHex
import org.s3.backup.lib.utilities.toLong
import org.s3.backup.lib.zip.model.ZipLfhLocation
import org.s3.backup.lib.zip.model.readCdfhSignature
import org.s3.backup.lib.zip.model.readEndOfZipFileSignature
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipInputStream

@Disabled
internal class ZipUtilityTest {

    @Test
//    @Disabled
    fun compress6gigfile() {
        val file = File("/Users/andrey.efimov/traceability/poc/s3-backup/sample.bin")
        val zipFilePath = "/Users/andrey.efimov/traceability/poc/s3-backup/sample.zip"

        ZipUtility.zipFiles(
            listOf(
                FileMetadata(
                    name = file.name,
                    path = file.path,
                    lastModified = file.lastModified(),
                    checksum = sha256(file).toHex()
                ).apply {
                    localFileRef = file
                }
            ),
            zipFilePath
        )
    }

    @Test
    fun findZipCentralDirectoryFileHeader2() {
        // offset is 8180; file size is 9962; offset from EOF is 1782
//        val pathname = "/var/folders/k0/q1xjrc151gd47wg4srx3pgn80000gn/T/1650482162324.zip" // 10K
        // offset is 6593144382; file size is 6595705060; offset from EOF is 2560678
        val pathname = "/var/folders/k0/q1xjrc151gd47wg4srx3pgn80000gn/T/1650479294359.zip" // 6G
//        val pathname = "/Users/andrey.efimov/traceability/poc/s3-backup/sample.zip"

        val zipFileSize = File(pathname).length()
        val lookUpForName = "c1de472111517a390ea074ae9083de6e0a33c5afd5baf8ccd4ab3411d59b961c"
        val fileReferences = mutableMapOf<String, ZipLfhLocation>()

        RandomAccessFile(pathname, "r").use { fis ->
            val endOfZipFileData = readEndOfZipFileSignature(fis, zipFileSize)
            var offset = endOfZipFileData.offsetOfStartOfCdBytes.toLong()

            val cdfhOffset = offset
            var previousFileKey = ""

            while (offset > -1 && offset < (cdfhOffset + endOfZipFileData.sizeOfCd)) {
                val signatureData = readCdfhSignature(fis, offset)

                if (previousFileKey.isNotBlank()) {
                    fileReferences[previousFileKey]?.let {
                        it.length = signatureData.fileOffset - it.offset
                    }
                }

                fileReferences[signatureData.fileName] = ZipLfhLocation(signatureData.fileOffset)
                previousFileKey = signatureData.fileName

                offset += signatureData.byteSize()
            }

            if (previousFileKey.isNotBlank()) {
                fileReferences[previousFileKey]?.let {
                    it.length = cdfhOffset - it.offset
                }
            }
        }

        println(fileReferences[lookUpForName])
        tryToDecompress(pathname, fileReferences[lookUpForName]!!.offset, fileReferences[lookUpForName]!!.length)
    }

    fun tryToDecompress(pathName: String, offset: Long, size: Long) {
        val result = File("/Users/andrey.efimov/traceability/poc/s3-backup/decompressed.mp3")

        result.outputStream().use { fos ->
            File(pathName).inputStream().use { fis ->
                fis.skip(offset)
                ByteArrayInputStream(fis.readNBytes(size.toInt())).use { bais ->
                    ZipInputStream(bais).use { zis ->
                        zis.nextEntry?.let {
                            val buff = ByteArray(4096)
                            var len: Int
                            while (zis.read(buff).also { len = it } > -1) {
                                fos.write(buff, 0, len)
                            }
                        }
                    }
                }
            }
        }
    }
}
