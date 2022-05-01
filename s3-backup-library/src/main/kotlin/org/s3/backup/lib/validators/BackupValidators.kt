package org.s3.backup.lib.validators

object BackupValidators {
    fun isValidKey(backupKey: String) = backupKey.matches(backupKeyRegex)

    private val backupKeyRegex = "^\\d{13}$".toRegex()
    val backupMetadataKeyRegex = "^(\\d{13})\\.metadata$".toRegex()
}
