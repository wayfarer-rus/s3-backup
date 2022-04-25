package org.s3.backup.lib.utilities

import software.amazon.awssdk.utils.IoUtils
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest

fun getTempDir(): String = System.getProperty("java.io.tmpdir") ?: ""

fun sha256(file: File): ByteArray = file.inputStream().use { fis ->
    val digester = MessageDigest.getInstance("SHa-256")
    val buffer = ByteArray(4096)
    var bytesCount: Int

    while (fis.read(buffer).also { bytesCount = it } != -1) {
        digester.update(buffer, 0, bytesCount)
    }

    digester.digest()
}

fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

fun ByteArray.toLong(): Long {
    if (this.size in 1..Long.SIZE_BYTES) {
        val buff = ByteArray(Long.SIZE_BYTES - this.size) + this
        return ByteBuffer.wrap(buff).long
    }

    error("Invalid array size (${this.size}) for Long conversion")
}

fun ByteArray.toInt(): Int {
    if (this.size in 1..Int.SIZE_BYTES) {
        // prepend array to size
        val buff = ByteArray(Int.SIZE_BYTES - this.size) + this
        return ByteBuffer.wrap(buff).int
    }

    error("Invalid array size (${this.size}) for Int conversion")
}

fun RandomAccessFile.readNBytes(n: Int): ByteArray {
    val buf = ByteArray(n)
    val bufSize = this.read(buf)

    if (bufSize != n) {
        error("Couldn't read requested $n bytes. $bufSize available.")
    }

    return buf
}

fun InputStream.writeToFile(file: File) = use { fis ->
    file.outputStream().use { fos ->
        IoUtils.copy(fis, fos)
    }
}
