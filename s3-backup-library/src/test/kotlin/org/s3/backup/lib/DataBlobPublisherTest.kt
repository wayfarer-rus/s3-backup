package org.s3.backup.lib

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.s3.backup.cmd.utility.getResourcePath
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.utilities.BackupUtility
import java.io.BufferedInputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
internal class DataBlobPublisherTest {
    @Test
    @Disabled
    fun testForDebug() {
        val fileMetadataList = BackupUtility.collectMetadata(
            File("/Users/andrey.efimov/traceability/poc/s3-backup/s3-backup-library")
        ).filesList().distinctBy { it.checksum }
        var bytes: ByteArray

        BufferedInputStream(DataBlobStream(name = "test", fileMetadataList)).use { testable ->
            bytes = testable.readAllBytes()
            // 289247
            // 372
            assertTrue { bytes.isNotEmpty() }
        }
    }

    @Test
    fun testRead() {
        val fileMetadataList = getTestFileMetadata("")
        BufferedInputStream(DataBlobStream(name = "test", fileMetadataList)).use { testable ->
            val bytes = testable.readAllBytes()
            assertTrue { bytes.isNotEmpty() }
        }
    }

    @Test
    fun testAvailable() {
        val filesMetadata = getTestFileMetadata("test-origin")
        BufferedInputStream(DataBlobStream(name = "test", filesMetadata)).use { testable ->
            assertEquals(0, testable.available())
            assertEquals(0, testable.available())
            assertTrue {
                filesMetadata.all {
                    it.archiveLocationRef.archiveName == "test"
                }
            }
        }
    }

    private fun getTestFileMetadata(path: String): List<FileMetadata> {
        return BackupUtility.collectMetadata(
            File(getResourcePath(path))
        )
            .filesList()
    }
}
