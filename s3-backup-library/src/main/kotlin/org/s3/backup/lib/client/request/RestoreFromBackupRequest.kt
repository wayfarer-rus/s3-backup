package org.s3.backup.lib.client.request

import org.s3.backup.lib.client.backupContext.BackupContext
import org.s3.backup.lib.validators.FileValidators

class RestoreFromBackupRequest(
    val context: BackupContext,
    val destinationDirectory: String
) {

    class Builder {
        var destinationDirectory: String = ""
        private lateinit var backupContext: BackupContext

        fun build(): RestoreFromBackupRequest {
            if (!FileValidators.isValidOutputDir(destinationDirectory)) {
                invalidDestinationDirectoryError(destinationDirectory)
            }

            return RestoreFromBackupRequest(
                context = backupContext,
                destinationDirectory = destinationDirectory
            )
        }

        fun backupContext(init: BackupContext.Builder.() -> Unit) {
            this.backupContext = BackupContext.Builder().apply(init).build()
        }
    }

    companion object {
        fun invalidDestinationDirectoryError(destinationDirectory: String) {
            throw IllegalArgumentException("Can't write output to '$destinationDirectory'")
        }
    }
}

fun restoreFromBackupRequest(init: RestoreFromBackupRequest.Builder.() -> Unit): RestoreFromBackupRequest {
    return RestoreFromBackupRequest.Builder().apply(init).build()
}
