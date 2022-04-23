package org.s3.backup.cmd.utility

import org.s3.backup.lib.utilities.S3BackupUtility
import org.s3.backup.lib.validators.S3BucketValidator
import java.io.File

object Messages {
    val bucketNameErrorText = """
        bucket name '%s' is invalid
    """.trimIndent()

    val directoryErrorText = """
        directory '%s' doesn't exit
    """.trimIndent()

    val helpText = """
        s3-backup cmd utility
        will backup directory content to given s3 bucket_name
        requires s3 key to be present in the environment
        
        usage: s3-backup [--dry-run] directory bucket_name
        
        --dry-run:  will not upload any data, just print delta
    """.trimIndent()
}

// why there is no command line library used?
// because our inputs are very simple and using additional library at this stage is an overkill
fun main(args: Array<String>) {
    val validateMainParams = { offset: Int ->
        if (!validDirectory(args[offset])) {
            println(Messages.directoryErrorText.format(args[offset]))
            false
        } else if (!validBucketName(args[offset + 1])) {
            println(Messages.bucketNameErrorText.format(args[offset + 1]))
            false
        } else {
            true
        }
    }

    val dryRunCase = {
        args.size == 3 && args[0] == "--dry-run"
    }

    val uploadCase = {
        args.size == 2
    }

    val listCase = {
        args.size in 2..3 && args[0] == "list"
    }

    val downloadFile = {
        args.size == 5 && args[0] == "download"
    }

    if (downloadFile()) {
        if (validBucketName(args[1])) {
            S3BackupUtility.downloadFileFromBackup(args[1], args[2], args[3], args[4])
        }
    } else if (listCase()) {
        if (validBucketName(args[1])) {
            if (args.size == 2)
                S3BackupUtility.listAllBackups(args[1]).forEach { println(it) }
            else
                S3BackupUtility.listBackupContent(args[1], args[2]).forEach { println(it) }
        }
    } else if (dryRunCase()) {
        if (validateMainParams(1))
            S3BackupUtility.doBackup(File(args[1]), args[2], dryRun = true)
    } else if (uploadCase()) {
        if (validateMainParams(0))
            S3BackupUtility.doBackup(File(args[0]), args[1])
    } else {
        printHelp()
    }
}

private fun validBucketName(bucketName: String) = S3BucketValidator.isValidName(bucketName)

// fix for ~ https://stackoverflow.com/questions/7163364/how-to-handle-in-file-paths
private fun validDirectory(dirName: String): Boolean {
    val dir = File(dirName)
    return dir.isDirectory && dir.canRead()
}

private fun printHelp() = println(Messages.helpText)
