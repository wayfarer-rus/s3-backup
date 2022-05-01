package org.s3.backup.lib

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.s3.backup.lib.metadata.model.FileLocationInArchive
import org.s3.backup.lib.metadata.model.FileMetadata
import org.s3.backup.lib.utilities.BYTE_BUFFER_SIZE
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class DataBlobPublisher(val name: String, val fileReferencesList: List<FileMetadata>) : Publisher<ByteBuffer> {
    private val bufferSize = BYTE_BUFFER_SIZE
    private val dataBlobStream = BufferedInputStream(DataBlobStream(name, fileReferencesList), bufferSize)
    val estimatedContentLength = fileReferencesList.sumOf { it.size }

    override fun subscribe(subscriber: Subscriber<in ByteBuffer>?) {
        subscriber?.onSubscribe(DataBlobSubscription(subscriber, dataBlobStream))
    }
}

class DataBlobSubscription(
    private val subscriber: Subscriber<in ByteBuffer>,
    private val dataBlobStream: BufferedInputStream,
) : org.reactivestreams.Subscription {
    // single thread executor will return chunks in order
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var futureList: MutableList<Future<*>> = mutableListOf()
    private var completed = false

    @Synchronized
    override fun request(n: Long) {
        if (!completed) {
            if (n <= 0) {
                executor.execute { subscriber.onError(IllegalArgumentException()) }
            } else {
                futureList.add(
                    executor.submit {
                        val nextChunk = dataBlobStream.readBytes()
                        subscriber.onNext(ByteBuffer.wrap(nextChunk))

                        if (nextChunk.isEmpty()) {
                            completed = true
                            subscriber.onComplete()
                        }
                    }
                )
            }
        }
    }

    @Synchronized
    override fun cancel() {
        completed = true
        // cancel all requests and await
        futureList.onEach { it.cancel(false) }.forEach { it.get() }
        // close data stream
        dataBlobStream.close()
    }
}

class DataBlobStream(val name: String, fileReferencesList: List<FileMetadata>) : InputStream() {
    private var closed = false
    private var offset = 0L
    private var cursor = 0L
    private var currentFileRef: FileMetadata? = null
    private var fileReferencesIterator = fileReferencesList.iterator()

    init {
        nextFile()
    }

    @Throws(IOException::class)
    private fun ensureOpen() {
        if (closed) {
            throw IOException("Stream closed")
        }
    }

    private fun nextFile() {
        currentFileRef = if (fileReferencesIterator.hasNext()) {
            fileReferencesIterator.next()
        } else {
            null
        }
    }

    private fun saveStateAndNextFile(cfr: FileMetadata) {
        // we reached the end of the current file
        cfr.inputStream?.close()
        // write offset and size
        cfr.archiveLocationRef.archiveName = name
        cfr.archiveLocationRef.fileLocation = FileLocationInArchive(offset = offset, length = cursor)
        offset += cursor
        cursor = 0
        // switch to the next file
        nextFile()
    }

    override fun read(): Int {
        ensureOpen()
        return currentFileRef?.let { cfr ->
            val nextByte = cfr.inputStream?.read() ?: -1

            if (nextByte > -1) {
                ++cursor
                return@let nextByte
            } else {
                saveStateAndNextFile(cfr)
                return@let read()
            }
        } ?: -1
    }

    override fun available(): Int {
        ensureOpen()
        return currentFileRef?.let { cfr ->
            cfr.inputStream?.let { fis ->
                val a = fis.available()

                // a == 0 means we at the end of the file and must switch to the next one to continue reading
                // FileInputStream returns 0 only at EOF
                if (a == 0) {
                    saveStateAndNextFile(cfr)
                    available()
                } else {
                    a
                }
            }
        } ?: 0
    }

    override fun close() {
        super.close()
        currentFileRef?.inputStream?.close()
        closed = true
    }
}
