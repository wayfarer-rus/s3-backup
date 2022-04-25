package org.s3.backup.cmd.utility

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
internal class MainTest {
    private val bucketNameErrorText = """
        Invalid S3 bucket name: %s.
    """.trimIndent()

    private val directoryErrorText = """
        Given directory %s is either not a directory or not accessible
    """.trimIndent()

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
            directoryErrorText.format(invalidDir),
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
            bucketNameErrorText.format(invalidBucketName),
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
        unmockkAll()
    }
}
