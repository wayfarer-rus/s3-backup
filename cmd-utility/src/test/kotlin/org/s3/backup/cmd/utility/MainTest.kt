package org.s3.backup.cmd.utility

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.s3.backup.lib.utilities.S3BackupUtility
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
internal class MainTest {
    @Test
    fun helpTest() {
        val output = tapSystemOut {
            main(arrayOf("--help"))
        }

        assertEquals(
            Messages.helpText,
            output?.trim()
        )
    }

    @Test
    fun `test if directory is invalid`() {
        val invalidDir = "/foo"
        val output = tapSystemOut {
            main(arrayOf(invalidDir, "bar"))
        }

        assertEquals(
            Messages.directoryErrorText.format(invalidDir),
            output?.trim()
        )
    }

    @Test
    fun `test bucket name is invalid`() {
        val invalidBucketName = "doc_example_bucket"
        val output = tapSystemOut {
            main(arrayOf("/tmp", invalidBucketName))
        }

        assertEquals(
            Messages.bucketNameErrorText.format(invalidBucketName),
            output?.trim()
        )
    }

    @Test
    fun `test dry-run`() {
        mockkObject(S3BackupUtility)
        val flagCapture = slot<Boolean>()
        every { S3BackupUtility.doBackup(any(), any(), capture(flagCapture)) } returns Unit

        main(arrayOf("--dry-run", "/tmp", "stub.bucket.name"))

        assertTrue { flagCapture.isCaptured }
        assertFalse { flagCapture.isNull }
        assertTrue { flagCapture.captured }
    }

    @Test
    fun `test list all backups`() {
        mockkObject(S3BackupUtility)
        val bnCapture = slot<String>()
        every { S3BackupUtility.listAllBackups(capture(bnCapture)) } returns emptyList()

        val bucketName = "stub.bucket.name"
        main(arrayOf("list", bucketName))

        assertTrue { bnCapture.isCaptured }
        assertEquals(bucketName, bnCapture.captured)
    }

    @Test
    fun `test list backup`() {
        mockkObject(S3BackupUtility)
        val bnCapture = slot<String>()
        val bkCapture = slot<String>()
        every { S3BackupUtility.listBackupContent(capture(bnCapture), capture(bkCapture)) } returns emptyList()

        val bucketName = "stub.bucket.name"
        val backupKey = "12345678"
        main(arrayOf("list", bucketName, backupKey))

        assertTrue { bnCapture.isCaptured }
        assertTrue { bkCapture.isCaptured }
        assertEquals(bucketName, bnCapture.captured)
        assertEquals(backupKey, bkCapture.captured)
    }
}
