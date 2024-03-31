package standard

import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

val s: String = File.separator

@OptIn(ExperimentalUnsignedTypes::class)
open class Stream {
    var stream: ByteArrayOutputStream

    var filePath: Path? = null

    val size: Int
        get() = stream.size()

    var position: Int = 0

    constructor() {
        stream = ByteArrayOutputStream()
    }

    constructor(value: Int) {
        stream = ByteArrayOutputStream(value)
    }

    constructor(ubyteArray: UByteArray) {
        stream = ByteArrayOutputStream(ubyteArray.size).apply { write(ubyteArray.toByteArray()) }
    }

    constructor(filePath: Path) {
        val bytes = Files.readAllBytes(filePath)
        stream = ByteArrayOutputStream(bytes.size).apply { write(bytes) }
        this.filePath = filePath
    }

    fun read(ubyteArray: UByteArray, offset: Int, length: Int): UByteArray {
        val ubytes = stream.toByteArray().toUByteArray()
        for (i in offset until offset + length) {
            ubyteArray[i - offset] = ubytes[i + position]
        }
        position += offset + length
        return ubyteArray
    }

    fun write(byteArray: ByteArray, offset: Int, length: Int) {
        stream.write(byteArray, offset, length)
        position += length
    }

    fun skip(offset: Int) {
        if (offset < 0) {
            throw IllegalArgumentException("Offset cannot be negative")
        }
        if (position + offset > position) {
            stream.write(ByteArray(offset))
        }
        position += offset
    }

    fun seek(offset: Int, origin: SeekOrigin) {
        when (origin) {
            SeekOrigin.Begin -> position = offset
            SeekOrigin.Current -> position += offset
            SeekOrigin.End -> position = size - offset
        }
    }

    enum class SeekOrigin {
        Begin,
        Current,
        End
    }
}
