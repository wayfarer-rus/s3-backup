package org.s3.backup.cmd.utility

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
internal class S3BackupUtilityTest {

    @Test
//    @Disabled
    fun `test prepare backup with dry-run`() {
        S3BackupUtility.doBackup(File("/tmp"), "", true)
    }

    @Test
    fun `test collect metadata`() {
        S3BackupUtility.collectMetadata(File("/Users/andrey.efimov/tmp"))
    }
}
