package standard

import java.nio.ByteBuffer
import java.util.zip.Deflater
import java.util.zip.Inflater

enum class ZlibCompressionLevel {
    NO_COMPRESSION,
    DEFAULT_COMPRESSION,
    BEST_SPEED,
    BEST_COMPRESSION,
    DEFLATED,
}

fun uncompressZlib(zlibData: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(zlibData)
    val result = ByteArray(1024)
    val output = ByteBuffer.allocate(zlibData.size * 2)
    while (!inflater.finished()) {
        val count = inflater.inflate(result)
        output.put(result, 0, count)
    }
    inflater.end()
    return output.array()
}

fun compressZlib(dataStream: ByteArray, compressionLevel: ZlibCompressionLevel): ByteArray {
    val deflater = Deflater()
    when (compressionLevel) {
        ZlibCompressionLevel.NO_COMPRESSION -> deflater.setLevel(Deflater.NO_COMPRESSION)
        ZlibCompressionLevel.DEFAULT_COMPRESSION -> deflater.setLevel(Deflater.DEFAULT_COMPRESSION)
        ZlibCompressionLevel.BEST_SPEED -> deflater.setLevel(Deflater.BEST_SPEED)
        ZlibCompressionLevel.BEST_COMPRESSION -> deflater.setLevel(Deflater.BEST_COMPRESSION)
        ZlibCompressionLevel.DEFLATED -> deflater.setLevel(Deflater.DEFLATED)
    }
    deflater.setInput(dataStream)
    deflater.finish()
    val bytesOut = ByteArray(1024)
    val output = ByteBuffer.allocate(dataStream.size * 2)
    while (!deflater.finished()) {
        val count = deflater.deflate(bytesOut)
        output.put(bytesOut, 0, count)
    }
    deflater.end()
    return output.array()
}