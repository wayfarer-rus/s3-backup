package org.s3.backup.lib.client.request

import org.s3.backup.lib.client.backupContext.BackupContext

class ListBackupContentRequest(val backupContext: BackupContext) {
    class Builder {
        lateinit var backupContext: BackupContext

        fun build(): ListBackupContentRequest {
            return ListBackupContentRequest(backupContext)
        }

        fun backupContext(init: BackupContext.Builder.() -> Unit) {
            backupContext = BackupContext.Builder().apply(init).build()
        }
    }
}

fun listBackupContentRequest(init: ListBackupContentRequest.Builder.() -> Unit): ListBackupContentRequest {
    return ListBackupContentRequest.Builder().apply(init).build()
}
