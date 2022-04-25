package org.s3.backup.cmd.utility

object Messages {
    val helpText = """
        s3-backup cmd utility
        will backup directory content to given s3 bucket_name
        requires s3 key to be present in the environment
        
        usages: 
            s3-backup [--dry-run] directory bucket_name
            s3-backup list bucket_name [backup]
            s3-backup download bucket_name backup file_to_download destination_file
            s3-backup restore bucket_name backup destination_directory
        
        --dry-run:  will not upload any data, just print delta
        list:       will list available backups or files from given backup
        download:   will download given file name to provided destination file
        restore:    will restore given backup state to given directory
    """.trimIndent()
}

// why there is no command line library used?
// because our inputs are very simple and using additional library at this stage is an overkill
fun main(args: Array<String>) {
    val dryRunCase = {
        args.size == 3 && args[0] == "--dry-run"
    }

    val uploadCase = {
        args.size == 2
    }

    val listCase = {
        args.size in 2..3 && args[0] == "list"
    }

    val downloadFileCase = {
        args.size == 5 && args[0] == "download"
    }

    val restoreCase = {
        args.size == 4 && args[0] == "restore"
    }

    try {
        if (restoreCase()) {
            S3BackupUtility.restoreFromBackup(args[1], args[2], args[3])
        } else if (downloadFileCase()) {
            S3BackupUtility.downloadFileFromBackup(args[1], args[2], args[3], args[4])
        } else if (listCase()) {
            if (args.size == 2)
                S3BackupUtility.listAllBackups(args[1])
            else
                S3BackupUtility.listBackupContent(args[1], args[2])
        } else if (dryRunCase()) {
            S3BackupUtility.doBackup(args[1], args[2], dryRun = true)
        } else if (uploadCase()) {
            S3BackupUtility.doBackup(args[0], args[1])
        } else {
            printHelp()
        }
    } catch (err: Exception) {
        println(err.message)
    }
}

private fun printHelp() = println(Messages.helpText)
