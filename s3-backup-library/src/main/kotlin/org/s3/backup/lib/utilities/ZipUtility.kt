package org.s3.backup.lib.utilities

import org.s3.backup.lib.model.FileMetadata
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtility {
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
}
