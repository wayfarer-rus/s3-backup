package org.s3.backup.lib.utilities

import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.zip.model.ZipLfhLocation
import org.s3.backup.lib.zip.model.readCdfhSignature
import org.s3.backup.lib.zip.model.readEndOfZipFileSignature
import software.amazon.awssdk.utils.IoUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object ZipUtility {
    @Throws(IOException::class)
    @JvmStatic
    fun zipFiles(filesToZip: List<FileMetadata>, zipFilePath: String) {
        FileOutputStream(zipFilePath).use { fos ->
            ZipOutputStream(fos).use { zipOut ->
                for (fileToZip in filesToZip) {
                    fileToZip.localFileRef?.let { localFile ->
                        FileInputStream(localFile).use { fis ->
                            val zipEntry = ZipEntry(fileToZip.checksum)
                            zipOut.putNextEntry(zipEntry)
                            val bytes = ByteArray(4096)
                            var length: Int

                            while (fis.read(bytes).also { length = it } > -1) {
                                zipOut.write(bytes, 0, length)
                            }
                        }
                    }
                }
            }
        }
    }

    fun unzipMultipleFilesToDestination(fileInputStream: InputStream, destinationDir: File): Map<String, File> {
        val result = mutableMapOf<String, File>()

        ZipInputStream(fileInputStream).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val outputFile = File("$destinationDir/${entry!!.name}")

                outputFile.outputStream().use { fos ->
                    IoUtils.copy(zis, fos)
                }

                result[entry!!.name] = outputFile
            }
        }

        return result
    }

    fun unzipOneFileToDestination(fileInputStream: InputStream, destination: File) {
        destination.outputStream().use { fos ->
            ZipInputStream(fileInputStream).use { zis ->
                zis.nextEntry?.let {
                    IoUtils.copy(zis, fos)
                }
            }
        }
    }

    fun offsetsMapFromZipFile(zipFileFullPath: String): Map<String, ZipLfhLocation> {
        val fileReferences = mutableMapOf<String, ZipLfhLocation>()

        RandomAccessFile(zipFileFullPath, "r").use { fis ->
            val endOfZipFileData = readEndOfZipFileSignature(fis, fis.length())
            var offset = endOfZipFileData.offsetOfStartOfCdBytes.toLong()

            val cdfhOffset = offset
            var previousFileKey = ""

            while (offset > -1 && offset < (cdfhOffset + endOfZipFileData.sizeOfCd)) {
                val signatureData = readCdfhSignature(fis, offset)

                fileReferences[previousFileKey]?.let {
                    it.length = signatureData.fileOffset - it.offset
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

        return fileReferences
    }
}
