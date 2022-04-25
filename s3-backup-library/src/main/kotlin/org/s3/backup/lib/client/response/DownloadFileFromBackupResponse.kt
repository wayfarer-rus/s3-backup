package org.s3.backup.lib.client.response

import java.io.InputStream

class DownloadFileFromBackupResponse(
    val inputStream: InputStream
)
