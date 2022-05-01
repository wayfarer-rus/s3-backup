package org.s3.backup.cmd.utility

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.s3.backup.lib.S3Backup
import org.s3.backup.lib.S3BackupMetadata
import org.s3.backup.lib.metadata.model.DirMetadata
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.metadata.model.MetadataNode
import java.io.InputStream
import java.nio.file.Paths
import kotlin.test.assertTrue

// small demo with alternative api style
// backup will be deleted at the end of the run
// do not forget to enable before run
internal class S3BackupLibraryDemoTest {
    private val bucketName = "andrei.test"
    private val testDirectory = Paths.get(
        S3BackupLibraryDemoTest::class.java.classLoader?.getResource("test-origin")?.path,
        ".."
    )

    @Test
    @Disabled
    fun demoTest() {
        // create context
        val s3Backup = S3Backup(bucketName)
        // create backup
        var backupMeta: S3BackupMetadata = s3Backup.upload(directoryPath = testDirectory)
        // list backups
        println("list backups from bucket")
        val backupsList: List<String> = s3Backup.list()
        println(backupsList)
        // get backup metadata
        backupMeta = s3Backup.getMetadata(backupMeta.key)
        // backup metadata can be used for various get operations
        val backupFiles: List<FileMetadata> = backupMeta.filesMetadataList
        println("list of files from backup metadata")
        backupFiles.forEach { println(it.path) }

        val backupDirs: List<DirMetadata> = backupMeta.directoriesMetadataList
        println("list of directories from backup metadata")
        backupDirs.forEach { println(it.path) }

        val node: MetadataNode? = backupMeta.findNode("/test-compare/test")
        println("found node by path: ${node?.path}")

        assertTrue { backupMeta.contains("dir1") }
        println("backup contains 'dir1'")

        val fileNode: FileMetadata? = backupMeta.findFile("foo.bin")
        println("found file by name: ${fileNode?.path}")

        // download single file
        val inputStream: InputStream = s3Backup.fileNodeInputStream(fileNode!!)
        println("file content:")
        println(String(inputStream.readAllBytes()))
        // restore backup
        s3Backup.restore(fromKey = backupMeta.key, toDirectory = Paths.get("../restore"))

        assertTrue { s3Backup.deleteBackup(backupMeta.key) }
        println("backup deleted. test finished")
    }
}
