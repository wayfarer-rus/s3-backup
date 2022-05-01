package org.s3.backup.lib.utilities

import software.amazon.awssdk.utils.IoUtils
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

const val BYTE_BUFFER_SIZE = 4096

fun sha256(file: File): ByteArray = file.inputStream().use { fis ->
    val digester = MessageDigest.getInstance("SHa-256")
    val buffer = ByteArray(BYTE_BUFFER_SIZE)
    var bytesCount: Int

    while (fis.read(buffer).also { bytesCount = it } != -1) {
        digester.update(buffer, 0, bytesCount)
    }

    digester.digest()
}

fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

fun InputStream.writeToFile(file: File) = use { fis ->
    file.outputStream().use { fos ->
        IoUtils.copy(fis, fos)
    }
}

fun InputStream.copyNBytesToOutputStream(os: OutputStream, size: Long) {
    var buf = ByteArray(kotlin.math.min(BYTE_BUFFER_SIZE.toLong(), size).toInt())
    var count = 0L
    var l = 0

    while ((size - count) >= buf.size && this.read(buf).also { l = it } > -1) {
        os.write(buf, 0, l)
        count += l
    }

    buf = this.readNBytes((size - count).toInt())
    os.write(buf)
}
