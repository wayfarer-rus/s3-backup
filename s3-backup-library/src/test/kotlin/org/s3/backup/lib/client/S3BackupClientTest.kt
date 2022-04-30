package org.s3.backup.lib.client

import org.junit.jupiter.api.Test
import org.s3.backup.lib.client.request.downloadFileFromBackupRequest
import org.s3.backup.lib.client.request.restoreFromBackupRequest
import java.io.ByteArrayOutputStream

internal class S3BackupClientTest {

    @Test
//    @Disabled
    fun downloadFromBackup() {
        val result = ByteArrayOutputStream(372).use { baos ->
            S3BackupClient.create().downloadFromBackup(
                downloadFileFromBackupRequest {
                    backupContext {
                        bucketName = "andrei.test"
                        backupKey = "1651313854368"
                    }
                    filePathToDownload = "/src/main/resources/logback.xml"
                }
            ).inputStream.copyTo(baos)
        }

        println(result)
    }

    @Test
    // @Disabled
    fun restoreBackup() {
        S3BackupClient.create().restoreFromBackup(
            restoreFromBackupRequest {
                backupContext {
                    backupKey = "1651324396526"
                    bucketName = "andrei.test"
                }
                destinationDirectory = "/Users/andrey.efimov/traceability/poc/s3-backup/restore"
            }
        )
    }
}
