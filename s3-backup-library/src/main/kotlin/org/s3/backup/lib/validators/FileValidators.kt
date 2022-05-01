package org.s3.backup.lib.validators

import java.io.File

object FileValidators {
    fun isValidOutputFile(pathDest: String) =
        File(pathDest).let { it.isFile && it.canWrite() || !it.exists() && it.parentFile.canWrite() }

    fun isValidOutputDir(pathDest: String) = File(pathDest).let {
        (it.exists() && it.isDirectory && it.canWrite()) || it.mkdirs()
    }

    fun isValidInputDir(directory: String) = File(directory).let { it.exists() && it.isDirectory && it.canRead() }
}
