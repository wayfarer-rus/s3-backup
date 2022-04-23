package org.s3.backup.lib.zip.model

import org.s3.backup.lib.utilities.toInt
import org.s3.backup.lib.utilities.toLong

class ExtendedInformation(
    private val byteArray: ByteArray
) {
    fun isValid() =
        byteArray.size >= 12 && byteArray.copyOfRange(0, 2).contentEquals(signatureByteArray)

    operator fun get(i: Int): Long? =
        when (i) {
            0 -> component1
            1 -> component2
            2 -> component3
            else -> null
        }

    val sizeOfChunk: Int
        get() = byteArray.copyOfRange(2, 4).reversedArray().toInt()

    val component1: Long
        get() = byteArray.copyOfRange(4, 12).reversedArray().toLong()

    val component2: Long?
        get() = if (sizeOfChunk >= 16) {
            byteArray.copyOfRange(12, 20).reversedArray().toLong()
        } else null

    val component3: Long?
        get() = if (sizeOfChunk >= 24) {
            byteArray.copyOfRange(20, 28).reversedArray().toLong()
        } else null

    companion object {
        val signatureByteArray = byteArrayOf(0x00, 0x01).reversedArray()
    }
}
