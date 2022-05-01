package org.s3.backup.cmd.utility

import io.mockk.junit5.MockKExtension
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.s3.backup.lib.client.S3Client
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.metadata.model.MetadataNode
import org.s3.backup.lib.utilities.BackupUtility
import java.io.File
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
internal class BackupUtilityTest {

    @Test
    @Disabled
    fun deserializationTest() {
        val data =
            "{\"type\":\"org.s3.backup.cmd.utility.model.DirMetadata\",\"name\":\"test-origin\",\"path\":\"\",\"lastModified\":1650202313903,\"_children\":[{\"type\":\"org.s3.backup.cmd.utility.model.FileMetadata\",\"name\":\"foo.bin\",\"path\":\"/foo.bin\",\"lastModified\":1650202313899,\"checksum\":\"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\"},{\"type\":\"org.s3.backup.cmd.utility.model.DirMetadata\",\"name\":\"dir1\",\"path\":\"/dir1\",\"lastModified\":1650202313903,\"_children\":[{\"type\":\"org.s3.backup.cmd.utility.model.FileMetadata\",\"name\":\"bar.bin\",\"path\":\"/dir1/bar.bin\",\"lastModified\":1650202313903,\"checksum\":\"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\"}]}]}"
        val result = Json.decodeFromString<MetadataNode>(data)
        println(result)
    }

    @Test
    @Disabled
    fun `test prepare backup with dry-run`() {
        val testDir = getResourcePath("test-origin")!!
        BackupUtility.doBackup(File(testDir), "andrei.test", true)
    }

    @Test
    @Disabled
    fun `test backup`() {
//        val testDir = getResourcePath("test-origin")!!
        val testDir = "/Users/andrey.efimov/traceability/poc/s3-backup/s3-backup-library"
        BackupUtility.doBackup(File(testDir), "andrei.test")
    }

    @Test
    @Disabled
    fun `test calculate huge hash`() {
        val testDir = getResourcePath("huge.bin")!!
        val metadata = BackupUtility.collectMetadata(File(testDir)) as FileMetadata
        assertEquals("c7f92b5994b7b27eff56263f368c9631bbd1f80947273805b8627d82f40cc92d", metadata.checksum)
    }

    @Test
    @Disabled
    fun `test calculate zero hash`() {
        val testDir = getResourcePath("zero.bin")!!
        val metadata = BackupUtility.collectMetadata(File(testDir)) as FileMetadata
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", metadata.checksum)
    }

    @Test
    fun `test origin metadata collection is correct`() {
        val testDir = getResourcePath("test-origin")!!
        val metadata = BackupUtility.collectMetadata(File(testDir))
        println(metadata)
    }

    @Test
    @Disabled
    fun `test range downloads from bucket`() {
        val stream = S3Client.bufferedInputStreamFromFileWithRange(
            bucketName = "andrei.test",
            "1651409557579",
            "bytes=0-0,-1" // first and last bytes
        )
        println()
    }
}

fun getResourcePath(resourceFileName: String) =
    BackupUtilityTest::class.java.classLoader?.getResource(resourceFileName)?.path
