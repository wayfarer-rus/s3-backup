package org.s3.backup.cmd.utility

import java.nio.file.Paths

object PathUtility {
    private val homeDir: String? = System.getProperty("user.home")

    fun resolvePath(filePath: String?): String? {
        var path = filePath?.trim()
        if (path.isNullOrBlank()) return null

        if (path.startsWith('~') && homeDir != null) {
            path = path.replace("~", homeDir)
        }

        return Paths.get(path).toAbsolutePath().normalize().toString()
    }

    fun workDirectory(fileOrDirName: String? = null): String {
        var path = Paths.get("")
        if (!fileOrDirName.isNullOrBlank()) {
            path = path.resolve(fileOrDirName)
        }
        return path.toAbsolutePath().toString()
    }
}
