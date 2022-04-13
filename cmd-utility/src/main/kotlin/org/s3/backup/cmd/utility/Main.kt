package org.s3.backup.cmd.utility

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
        
        usage: s3-backup directory bucket_name
    """.trimIndent()
}

fun main(args: Array<String>) {
    if (args.size == 2) {
        if (!validDirectory(args[0])) {
            println(Messages.directoryErrorText.format(args[0]))
        } else if (!validBucketName(args[1])) {
            println(Messages.bucketNameErrorText.format(args[1]))
        }
    } else {
        printHelp()
    }
}

private fun validBucketName(bucketName: String): Boolean {
    return S3BucketValidator.isValidName(bucketName)
}

private fun validDirectory(dir: String): Boolean = File(dir).isDirectory

private fun printHelp() = println(Messages.helpText)
