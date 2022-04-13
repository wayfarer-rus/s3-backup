package org.s3.backup.cmd.utility

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
    fun `test if directory is valid`() {
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
    fun `test bucket name is valid`() {
        val invalidBucketName = "doc_example_bucket"
        val output = tapSystemOut {
            main(arrayOf("/tmp", invalidBucketName))
        }

        assertEquals(
            Messages.bucketNameErrorText.format(invalidBucketName),
            output?.trim()
        )
    }
}
