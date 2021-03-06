package org.s3.backup.cmd.utility

object Messages {
    val helpText = """
        s3-backup cmd utility
        will backup directory content to given s3 bucket_name
        requires s3 key to be present in the environment
        
        usages: 
            s3-backup backup [--dry-run] bucket_name directory 
            s3-backup list bucket_name [backup_key]
            s3-backup download bucket_name backup_key file_to_download [destination_file]
            s3-backup restore bucket_name backup_key destination_directory
        
        --dry-run:  will not upload any data, just print delta
        backup:     will do backup to given s3, unless --dry-run is set
        list:       will list available backups; if backup_key is given - list files
        download:   will download given file name to provided destination file
        restore:    will restore given backup state to given directory
    """.trimIndent()
}

// why there is no command line library used?
// because our inputs are very simple and using additional library at this stage is an overkill
fun main(args: Array<String>) {
    val dryRunCase = {
        args.size == 4 && args[0] == "backup" && args[1] == "--dry-run"
    }

    val uploadCase = {
        args.size == 3 && args[0] == "backup"
    }

    val listCase = {
        args.size in 2..3 && args[0] == "list"
    }

    val downloadFileCase = {
        args.size in 4..5 && args[0] == "download"
    }

    val restoreCase = {
        args.size == 4 && args[0] == "restore"
    }

    try {
        when {
            restoreCase() -> S3BackupUtility.restoreFromBackup(args[1], args[2], args[3])
            downloadFileCase() -> when (args.size) {
                5 -> S3BackupUtility.downloadFileFromBackup(args[1], args[2], args[3], args[4])
                else -> S3BackupUtility.downloadFileFromBackup(args[1], args[2], args[3])
            }
            listCase() -> when (args.size) {
                2 -> S3BackupUtility.listAllBackups(args[1])
                else -> S3BackupUtility.listBackupContent(args[1], args[2])
            }
            dryRunCase() -> S3BackupUtility.doBackup(args[2], args[3], dryRun = true)
            uploadCase() -> S3BackupUtility.doBackup(args[1], args[2])
            else -> printHelp()
        }
    } catch (err: Exception) {
        println(err.message)
    }
}

private fun printHelp() = println(Messages.helpText)
