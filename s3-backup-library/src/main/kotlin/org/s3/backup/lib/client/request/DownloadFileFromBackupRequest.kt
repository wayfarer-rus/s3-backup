package org.s3.backup.lib.client.request

import org.s3.backup.lib.client.backupContext.BackupContext

class DownloadFileFromBackupRequest(
    val filePathToDownload: String,
    val context: BackupContext
) {
    class Builder {
        var filePathToDownload: String = ""
        lateinit var backupContext: BackupContext

        fun build(): DownloadFileFromBackupRequest {
            if (filePathToDownload.isBlank()) {
                throw IllegalArgumentException("Given file path for download must be defined.")
            }

            return DownloadFileFromBackupRequest(
                filePathToDownload = filePathToDownload,
                context = backupContext
            )
        }

        fun backupContext(init: BackupContext.Builder.() -> Unit) {
            backupContext = BackupContext.Builder().apply(init).build()
        }
    }
}

fun downloadFileFromBackupRequest(init: DownloadFileFromBackupRequest.Builder.() -> Unit): DownloadFileFromBackupRequest {
    return DownloadFileFromBackupRequest.Builder().apply(init).build()
}
