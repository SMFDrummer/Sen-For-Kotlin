package standard

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.file.Paths

@Suppress("unused", "MemberVisibilityCanBePrivate")
@OptIn(ExperimentalUnsignedTypes::class)
class SenBuffer {
    var baseStream: ByteArrayOutputStream
    var length: Long
        get() = baseStream.size().toLong()
        set(value) {
            baseStream = ByteArrayOutputStream(value.toInt())
        }
    var writeOffset: Long = 0
    var readOffset: Long = 0
    val current: Long
        get() = this.readOffset
    var imageWidth: Int = 0
    var imageHeight: Int = 0
    var tempReadOffset: Long = 0
    var tempWriteOffset: Long = 0
    var filePath: String? = null
    private var mBuffer: ByteArray? = null
    var size: Long
        get() = this.length
        set(value) {
            this.length = value
        }
    var encode: Charset = Charsets.UTF_8

    constructor(stream: ByteArrayOutputStream) {
        baseStream = stream
    }

    constructor() : this(ByteArrayOutputStream())

    constructor(bytes: ByteArray) : this(ByteArrayOutputStream().apply { write(bytes) })

    constructor(size: Int) {
        val bytes = UByteArray(size)
        baseStream = ByteArrayOutputStream().apply { write(bytes.toByteArray()) }
    }

    constructor(filepath: String) {
        val newPath = checkPath(filepath)
        filePath = newPath
        val bytes = File(newPath).readBytes()
        baseStream = ByteArrayOutputStream().apply { write(bytes) }
    }

    private fun checkPath(filepath: String): String {
        val path = Paths.get(filepath.replace("\\", "/"))
        val newStringDir = path.parent.toString().split("/").map { it.trimEnd() }
        val newFilePath = path.fileName.toString()
        val newPath = Paths.get(newStringDir.joinToString("/"), newFilePath).toString()
        return newPath
    }

    fun ByteArrayOutputStream.skip(offset: Long) {
        if (offset < 0) {
            throw IllegalArgumentException("Offset cannot be negative")
        }

        val currentPosition = this.size()
        val newPosition = currentPosition + offset.toInt()

        if (newPosition < 0) {
            throw IllegalArgumentException("Invalid offset value")
        }

        if (newPosition > currentPosition) {
            val zeroBytes = ByteArray(newPosition - currentPosition)
            this.write(zeroBytes)
        }
    }

    fun ByteArrayOutputStream.setOffset(offset: Long): ByteArrayOutputStream {
        if (offset < 0 || offset > this.size().toLong()) {
            throw IllegalArgumentException("Offset is out of bounds: $offset")
        }

        val newStream = ByteArrayOutputStream()
        newStream.write(this.toByteArray(), 0, offset.toInt())
        return newStream
    }

    private fun fixReadOffset(offset: Long) {
        if (offset != -1L && offset > -1L) {
            readOffset = offset
            baseStream.skip(readOffset)
        } else if (offset == -1L) {
            baseStream.skip(readOffset)
        } else {
            throw IllegalArgumentException("Offset not found")
        }
    }

    private fun fixWriteOffset(offset: Long) {
        if (offset != -1L && offset > -1L) {
            writeOffset = offset
            baseStream = baseStream.setOffset(writeOffset)
        } else if (offset == -1L) {
            baseStream = baseStream.setOffset(writeOffset)
        } else {
            throw Exception()
        }
    }

    fun readBytes(count: Int, offset: Long = -1L): ByteArray {
        fixReadOffset(offset)
        if (readOffset + count > length) {
            throw IllegalArgumentException("Offset outside bounds of data view")
        }
        val array = ByteArray(count)
        baseStream.toByteArray().copyInto(array, 0, readOffset.toInt(), readOffset.toInt() + count)
        readOffset += count
        return array
    }

    fun readByte(offset: Long = -1L): Byte {
        fixReadOffset(offset)
        if (readOffset + 1 > length) {
            throw IllegalArgumentException("Offset outside bounds of data view")
        }
        val array = ByteArray(1)
        baseStream.toByteArray().copyInto(array, 0, readOffset.toInt(), readOffset.toInt() + 1)
        readOffset += 1
        return array[0]
    }

    fun readString(count: Int, offset: Long = -1L, encodingType: String = "UTF-8"): String {
        fixReadOffset(offset)
        val array = readBytes(count)
        val charset = Charset.forName(encodingType)
        return String(array, charset)
    }

    fun getBytes(count: Int, offset: Long): ByteArray {
        baseStream.skip(offset)
        if (offset + count > length) {
            throw IllegalArgumentException("Offset outside bounds of data view")
        }
        val array = ByteArray(count)
        baseStream.toByteArray().copyInto(array, 0, offset.toInt(), offset.toInt() + count)
        baseStream.skip(readOffset)
        return array
    }

    fun readUInt8(offset: Long = -1L): UByte {
        fixReadOffset(offset)
        val mBuffer = readBytes(1)
        return mBuffer[0].toUByte()
    }

    fun readUInt16LE(offset: Long = -1L): UShort {
        fixReadOffset(offset)
        val mBuffer = readBytes(2)
        return mBuffer[0].toUShort() or (mBuffer[1].toUInt() shl 8).toUShort()
    }

    fun readUInt16BE(offset: Long = -1L): UShort {
        fixReadOffset(offset)
        val mBuffer = readBytes(2)
        return mBuffer[1].toUShort() or (mBuffer[0].toUInt() shl 8).toUShort()
    }

    fun readUInt24LE(offset: Long = -1L): UInt {
        fixReadOffset(offset)
        val mBuffer = readBytes(3)
        return mBuffer[0].toUInt() or (mBuffer[1].toUInt() shl 8) or (mBuffer[2].toUInt() shl 16)
    }

    fun readUInt24BE(offset: Long = -1L): UInt {
        fixReadOffset(offset)
        val mBuffer = readBytes(3)
        return mBuffer[2].toUInt() or (mBuffer[1].toUInt() shl 8) or (mBuffer[0].toUInt() shl 16)
    }

    fun readUInt32LE(offset: Long = -1L): UInt {
        fixReadOffset(offset)
        val mBuffer = readBytes(4)
        return mBuffer[0].toUInt() or (mBuffer[1].toUInt() shl 8) or (mBuffer[2].toUInt() shl 16) or (mBuffer[3].toUInt() shl 24)
    }

    fun readUInt32BE(offset: Long = -1L): UInt {
        fixReadOffset(offset)
        val mBuffer = readBytes(4)
        return mBuffer[3].toUInt() or (mBuffer[2].toUInt() shl 8) or (mBuffer[1].toUInt() shl 16) or (mBuffer[0].toUInt() shl 24)
    }

    fun readUInt64LE(offset: Long = -1L): ULong {
        fixReadOffset(offset)
        val mBuffer = readBytes(8)
        return ((mBuffer[4].toULong() or (mBuffer[5].toULong() shl 8) or (mBuffer[6].toULong() shl 16) or (mBuffer[7].toULong() shl 24)) shl 32) or (mBuffer[0].toULong() or (mBuffer[1].toULong() shl 8) or (mBuffer[2].toULong() shl 16) or (mBuffer[3].toULong() shl 24))
    }

    fun readUInt64BE(offset: Long = -1L): ULong {
        fixReadOffset(offset)
        val mBuffer = readBytes(8)
        return ((mBuffer[3].toULong() or (mBuffer[2].toULong() shl 8) or (mBuffer[1].toULong() shl 16) or (mBuffer[0].toULong() shl 24)) shl 32) or (mBuffer[7].toULong() or (mBuffer[6].toULong() shl 8) or (mBuffer[5].toULong() shl 16) or (mBuffer[4].toULong() shl 24))
    }

    fun readVarUInt32(offset: Long = -1L): UInt {
        fixReadOffset(offset)
        return readVarInt32().toUInt()
    }

    fun readP(value: Long? = null): Long {
        return if (value == null) this.readOffset else {
            this.readOffset = value
            this.current
        }
    }

    fun readVarUInt64(offset: Long = -1L): ULong {
        fixReadOffset(offset)
        return readVarInt64().toULong()
    }

    fun readInt8(offset: Long = -1L): Byte {
        fixReadOffset(offset)
        val mBuffer = readBytes(1)
        return mBuffer[0].toByte()
    }

    fun readInt16LE(offset: Long = -1L): Short {
        fixReadOffset(offset)
        val mBuffer = readBytes(2)
        return (mBuffer[0].toInt() or (mBuffer[1].toInt() shl 8)).toShort()
    }

    fun readInt16BE(offset: Long = -1L): Short {
        fixReadOffset(offset)
        val mBuffer = readBytes(2)
        return (mBuffer[1].toInt() or (mBuffer[0].toInt() shl 8)).toShort()
    }

    fun readInt24LE(offset: Long = -1L): Int {
        fixReadOffset(offset)
        var num = readUInt24LE()
        if ((num and 0x800000u) != 0u) num = num or 0xff000000u
        return num.toInt()
    }

    fun readInt24BE(offset: Long = -1L): Int {
        fixReadOffset(offset)
        var num = readUInt24BE()
        if ((num and 0x800000u) != 0u) num = num or 0xff000000u
        return num.toInt()
    }

    fun readInt32LE(offset: Long = -1L): Int {
        fixReadOffset(offset)
        val mBuffer = readBytes(4)
        return mBuffer[0].toInt() or (mBuffer[1].toInt() shl 8) or (mBuffer[2].toInt() shl 16) or (mBuffer[3].toInt() shl 24)
    }

    fun readInt32BE(offset: Long = -1L): Int {
        fixReadOffset(offset)
        val mBuffer = readBytes(4)
        return mBuffer[3].toInt() or (mBuffer[2].toInt() shl 8) or (mBuffer[1].toInt() shl 16) or (mBuffer[0].toInt() shl 24)
    }

    fun readInt64LE(offset: Long = -1L): Long {
        fixReadOffset(offset)
        val mBuffer = readBytes(8)
        return ((mBuffer[4].toLong() or (mBuffer[5].toLong() shl 8) or (mBuffer[6].toLong() shl 16) or (mBuffer[7].toLong() shl 24)) shl 32) or (mBuffer[0].toLong() or (mBuffer[1].toLong() shl 8) or (mBuffer[2].toLong() shl 16) or (mBuffer[3].toLong() shl 24))
    }

    fun readInt64BE(offset: Long = -1L): Long {
        fixReadOffset(offset)
        val mBuffer = readBytes(8)
        return ((mBuffer[3].toLong() or (mBuffer[2].toLong() shl 8) or (mBuffer[1].toLong() shl 16) or (mBuffer[0].toLong() shl 24)) shl 32) or (mBuffer[7].toLong() or (mBuffer[6].toLong() shl 8) or (mBuffer[5].toLong() shl 16) or (mBuffer[4].toLong() shl 24))
    }

    fun readVarInt32(offset: Long = -1L): Int {
        fixReadOffset(offset)
        var num = 0
        var num2 = 0
        var b: UByte
        do {
            if (num2 == 35) {
                throw Exception()
            }
            b = readUInt8()
            num = num or ((b.toInt() and 0x7F) shl num2)
            num2 += 7
        } while ((b.toInt() and 0x80) != 0)
        return num
    }

    fun readVarInt64(offset: Long = -1L): Long {
        fixReadOffset(offset)
        var num: Long = 0
        var num2 = 0
        var b: UByte
        do {
            if (num2 == 70) {
                throw Exception()
            }
            b = readUInt8()
            num = num or ((b.toLong() and 0x7F) shl num2)
            num2 += 7
        } while ((b.toInt() and 0x80) != 0)
        return num
    }

    fun readZigZag32(offset: Long = -1L): Int {
        fixReadOffset(offset)
        val n = readVarInt32().toUInt()
        return ((n.toInt() shl 31) shr 31) xor (n.toInt() shr 1)
    }

    fun readZigZag64(offset: Long = -1L): Long {
        fixReadOffset(offset)
        val n = readVarInt64().toULong()
        return ((n.toLong() shr 1) xor -(n.toLong() and 0b1))
    }

    fun readFloatLE(offset: Long = -1L): Float {
        fixReadOffset(offset)
        val mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).float
    }

    fun readFloatBE(offset: Long = -1L): Float {
        fixReadOffset(offset)
        val mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).float
    }

    fun readDoubleLE(offset: Long = -1L): Double {
        fixReadOffset(offset)
        val mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).double
    }

    fun readDoubleBE(offset: Long = -1L): Double {
        fixReadOffset(offset)
        val mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).double
    }

    fun readBool(offset: Long = -1L): Boolean {
        fixReadOffset(offset)
        val mBuffer = readBytes(1)
        return mBuffer[0].toInt() != 0
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun readStringByEmpty(offset: Long = -1L): String {
        fixReadOffset(offset)
        val bytes = mutableListOf<UByte>()
        var tp: UByte
        while (true) {
            tp = readUInt8()
            if (tp == 0.toUByte()) {
                break
            }
            bytes.add(tp)
        }
        val byteStr = bytes.toUByteArray()
        return String(byteStr.toByteArray(), Charsets.UTF_8)
    }

    fun getStringByEmpty(offset: Long): String {
        val tempOffset = readOffset
        readOffset = offset
        val str = readStringByEmpty()
        readOffset = tempOffset
        return str
    }

    fun readCharByInt16LE(offset: Long = -1L): Char {
        fixReadOffset(offset)
        val mBuffer = readBytes(2)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).char
    }

    fun readStringByUInt8(offset: Long = -1L): String {
        fixReadOffset(offset)
        return readString(readUInt8().toInt())
    }

    fun readStringByInt8(offset: Long = -1L): String {
        fixReadOffset(offset)
        return readString(readInt8().toInt())
    }

    fun readStringByUInt16LE(offset: Long = -1L): String {
        fixReadOffset(offset)
        return readString(readUInt16LE().toInt())
    }

    fun readStringByInt16LE(offset: Long = -1L): String {
        fixReadOffset(offset)
        return readString(readInt16LE().toInt())
    }

    fun readStringByUInt32LE(offset: Long = -1L): String {
        fixReadOffset(offset)
        return readString(readUInt32LE().toInt())
    }

    fun readStringByInt32LE(offset: Long = -1L): String {
        fixReadOffset(offset)
        return readString(readInt32LE())
    }

    fun readStringByVarInt32(offset: Long = -1L): String {
        fixReadOffset(offset)
        return readString(readVarInt32())
    }

    fun peekUInt8(offset: Long = -1L): UByte {
        val num = readUInt8(offset)
        readOffset--
        return num
    }

    fun peekInt8(offset: Long = -1L): Byte {
        val num = readInt8(offset)
        readOffset--
        return num
    }

    fun peekUInt16LE(offset: Long = -1L): UShort {
        val num = readUInt16LE(offset)
        readOffset -= 2
        return num
    }

    fun peekUInt16BE(offset: Long = -1L): UShort {
        val num = readUInt16BE(offset)
        readOffset -= 2
        return num
    }

    fun peekInt16LE(offset: Long = -1L): Short {
        val num = readInt16LE(offset)
        readOffset -= 2
        return num
    }

    fun peekInt16BE(offset: Long = -1L): Short {
        val num = readInt16BE(offset)
        readOffset -= 2
        return num
    }

    fun peekUInt24LE(offset: Long = -1L): UInt {
        val num = readUInt24LE(offset)
        readOffset -= 3
        return num
    }

    fun peekUInt24BE(offset: Long = -1L): UInt {
        val num = readUInt24BE(offset)
        readOffset -= 3
        return num
    }

    fun peekInt24LE(offset: Long = -1L): Int {
        val num = readInt24LE(offset)
        readOffset -= 3
        return num
    }

    fun peekInt24BE(offset: Long = -1L): Int {
        val num = readInt24BE(offset)
        readOffset -= 3
        return num
    }

    fun peekUInt32LE(offset: Long = -1L): UInt {
        val num = readUInt32LE(offset)
        readOffset -= 4
        return num
    }

    fun peekUInt32BE(offset: Long = -1L): UInt {
        val num = readUInt32BE(offset)
        readOffset -= 4
        return num
    }

    fun peekInt32LE(offset: Long = -1L): Int {
        val num = readInt32LE(offset)
        readOffset -= 4
        return num
    }

    fun peekInt32BE(offset: Long = -1L): Int {
        val num = readInt32BE(offset)
        readOffset -= 4
        return num
    }

    fun peekString(count: Int, offset: Long = -1L, encodingType: String = "UTF-8"): String {
        val str = readString(count, offset, encodingType)
        readOffset -= count
        return str
    }

    fun writeBytes(array: UByteArray, offset: Long = -1L) {
        fixWriteOffset(offset)
        val length = array.size
        baseStream.write(array.toByteArray(), 0, length)
        writeOffset += length
    }

    fun writeByte(byte: UByte, offset: Long = -1L) {
        fixWriteOffset(offset)
        baseStream.write(ubyteArrayOf(byte).toByteArray(), 0, 1)
        writeOffset += 1
    }

    fun writeBytes(array: ByteArray, offset: Long = -1L) {
        fixWriteOffset(offset)
        val length = array.size
        baseStream.write(array, 0, length)
        writeOffset += length
    }

    fun writeByte(byte: Byte, offset: Long = -1L) {
        fixWriteOffset(offset)
        baseStream.write(byteArrayOf(byte), 0, 1)
        writeOffset += 1
    }

    fun writeString(str: String, offset: Long = -1L, encodingType: String = "UTF-8") {
        fixWriteOffset(offset)
        val strBytes = str.toByteArray(Charset.forName(encodingType)).toUByteArray()
        writeBytes(strBytes)
    }

    fun writeStringByEmpty(str: String?, offset: Long = -1L) {
        if (str == null) {
            writeUInt8(0u)
            return
        }
        writeString(str, offset)
        writeUInt8(0u)
    }

    fun writeNull(count: Int, offset: Long = -1L) {
        if (count < 0) throw Exception()
        if (count == 0) return
        fixWriteOffset(offset)
        val nullBytes = UByteArray(count)
        writeBytes(nullBytes)
    }

    fun writeStringFourUByte(str: String, offset: Long = -1L) {
        fixWriteOffset(offset)
        val strLength = str.length
        val strBytes = UByteArray(strLength * 4 + 4)
        for (i in 0 until strLength) {
            strBytes[i * 4] = str[i].code.toUByte()
        }
        writeBytes(strBytes)
    }

    fun setUBytes(array: UByteArray, offset: Long, overwriteOffset: Boolean) {
        val length = array.size
        if (overwriteOffset) {
            fixWriteOffset(offset)
        } else {
            baseStream.skip(offset)
        }
        baseStream.write(array.toByteArray(), 0, length)
        baseStream.skip(writeOffset)
    }

    fun writeUInt8(number: UByte, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(number)
        writeBytes(mBuffer)
    }

    fun writeUInt16LE(number: UShort, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number and 0xFFu).toUByte(), (number.toUInt() shr 8).toUByte())
        writeBytes(mBuffer)
    }

    fun writeUInt16BE(number: UShort, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number.toUInt() shr 8).toUByte(), (number and 0xFFu).toUByte())
        writeBytes(mBuffer)
    }

    fun writeUInt24LE(number: UInt, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number and 0xFFu).toUByte(), (number shr 8).toUByte(), (number shr 16).toUByte())
        writeBytes(mBuffer)
    }

    fun writeUInt24BE(number: UInt, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number shr 16).toUByte(), (number shr 8).toUByte(), (number and 0xFFu).toUByte())
        writeBytes(mBuffer)
    }

    fun writeUInt32LE(number: UInt, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number and 0xFFu).toUByte(),
            (number shr 8).toUByte(),
            (number shr 16).toUByte(),
            (number shr 24).toUByte()
        )
        writeBytes(mBuffer)
    }

    fun writeUInt32BE(number: UInt, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number shr 24).toUByte(),
            (number shr 16).toUByte(),
            (number shr 8).toUByte(),
            (number and 0xFFu).toUByte()
        )
        writeBytes(mBuffer)
    }

    fun writeUInt64LE(number: ULong, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number and 0xFFu).toUByte(),
            (number shr 8).toUByte(),
            (number shr 16).toUByte(),
            (number shr 24).toUByte(),
            (number shr 32).toUByte(),
            (number shr 40).toUByte(),
            (number shr 48).toUByte(),
            (number shr 56).toUByte()
        )
        writeBytes(mBuffer)
    }

    fun writeUInt64BE(number: ULong, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number shr 56).toUByte(),
            (number shr 48).toUByte(),
            (number shr 40).toUByte(),
            (number shr 32).toUByte(),
            (number shr 24).toUByte(),
            (number shr 16).toUByte(),
            (number shr 8).toUByte(),
            (number and 0xFFu).toUByte()
        )
        writeBytes(mBuffer)
    }

    fun writeFloatLE(number: Float, offset: Long = -1L) {
        val mBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeFloatBE(number: Float, offset: Long = -1L) {
        val mBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeDoubleLE(number: Double, offset: Long = -1L) {
        val mBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeDoubleBE(number: Double, offset: Long = -1L) {
        val mBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeUVarInt32(number: UInt, offset: Long = -1L) {
        var num = number
        val bytes = mutableListOf<UByte>()
        while (num >= 128u) {
            bytes.add((num or 0x80u).toUByte())
            num = num shr 7
        }
        bytes.add(num.toUByte())
        val mBuffer = bytes.toUByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeUVarInt64(number: ULong, offset: Long = -1L) {
        var num = number
        val bytes = mutableListOf<UByte>()
        while (num >= 128uL) {
            bytes.add((num or 0x80uL).toUByte())
            num = num shr 7
        }
        bytes.add(num.toUByte())
        val mBuffer = bytes.toUByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeInt8(number: Byte, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf(number)
        writeBytes(mBuffer)
    }

    fun writeInt16LE(number: Short, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf((number.toInt() and 0xFF).toByte(), (number.toInt() shr 8).toByte())
        writeBytes(mBuffer)
    }

    fun writeInt16BE(number: Short, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf((number.toInt() shr 8).toByte(), (number.toInt() and 0xFF).toByte())
        writeBytes(mBuffer)
    }

    fun writeInt24LE(number: Int, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf((number and 0xFF).toByte(), (number shr 8).toByte(), (number shr 16).toByte())
        writeBytes(mBuffer)
    }

    fun writeInt24BE(number: Int, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf((number shr 16).toByte(), (number shr 8).toByte(), (number and 0xFF).toByte())
        writeBytes(mBuffer)
    }

    fun writeInt32LE(number: Int, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf(
            (number and 0xFF).toByte(),
            (number shr 8).toByte(),
            (number shr 16).toByte(),
            (number shr 24).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeInt32BE(number: Int, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf(
            (number shr 24).toByte(),
            (number shr 16).toByte(),
            (number shr 8).toByte(),
            (number and 0xFF).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeBigInt64LE(number: Long, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf(
            (number and 0xFF).toByte(),
            (number shr 8).toByte(),
            (number shr 16).toByte(),
            (number shr 24).toByte(),
            (number shr 32).toByte(),
            (number shr 40).toByte(),
            (number shr 48).toByte(),
            (number shr 56).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeBigInt64BE(number: Long, offset: Long = -1L) {
        fixWriteOffset(offset)
        val mBuffer = byteArrayOf(
            (number shr 56).toByte(),
            (number shr 48).toByte(),
            (number shr 40).toByte(),
            (number shr 32).toByte(),
            (number shr 24).toByte(),
            (number shr 16).toByte(),
            (number shr 8).toByte(),
            (number and 0xFF).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeVarInt32(number: Int, offset: Long = -1L) {
        var num = number.toUInt()
        fixWriteOffset(offset)
        while (num >= 128u) {
            writeUInt8((num or 0x80u).toUByte())
            num = num shr 7
        }
        writeUInt8(num.toUByte())
    }

    fun writeVarInt64(number: Long, offset: Long = -1L) {
        var num = number.toULong()
        fixWriteOffset(offset)
        while (num >= 128uL) {
            writeUInt8((num or 0x80uL).toUByte())
            num = num shr 7
        }
        writeUInt8(num.toUByte())
    }

    fun writeBool(value: Boolean, offset: Long = -1L) {
        val mBuffer = ubyteArrayOf(if (value) 1u else 0u)
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeZigZag32(number: Int, offset: Long = -1L) {
        fixWriteOffset(offset)
        writeVarInt32((number shl 1) xor (number shr 31))
    }

    fun slice(begin: Long, end: Long) {
        if (begin < 0 || end < begin || end > this.baseStream.size().toLong()) {
            throw IllegalArgumentException("Invalid Buffer Slice")
        }
        val buffer = baseStream.toByteArray().sliceArray(begin.toInt() until end.toInt())
        baseStream = ByteArrayOutputStream().apply { write(buffer) }
    }

    fun writeZigZag64(number: Long, offset: Long = -1L) {
        fixWriteOffset(offset)
        writeVarInt64((number shl 1) xor (number shr 63))
    }

    fun writeCharByInt16LE(charStr: Char, offset: Long = -1L) {
        val strBytes = charStr.toString().toByteArray(Charsets.UTF_16LE)
        fixWriteOffset(offset)
        writeBytes(strBytes)
    }

    fun writeStringByUInt8(str: String?, offset: Long = -1L) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt8(0u)
            return
        }
        writeUInt8(str.length.toUByte())
        writeString(str)
    }

    fun writeStringByInt8(str: String?, offset: Long = -1L) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt8(0)
            return
        }
        writeInt8(str.length.toByte())
        writeString(str)
    }

    fun writeStringByUInt16LE(str: String?, offset: Long = -1L) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt16LE(0u)
            return
        }
        writeUInt16LE(str.length.toUShort())
        writeString(str)
    }

    fun writeStringByInt16LE(str: String?, offset: Long = -1L) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt16LE(0)
            return
        }
        writeInt16LE(str.length.toShort())
        writeString(str)
    }

    fun writeStringByUInt32LE(str: String?, offset: Long = -1L) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt32LE(0u)
            return
        }
        writeUInt32LE(str.length.toUInt())
        writeString(str)
    }

    fun writeStringByInt32LE(str: String?, offset: Long = -1L) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt32LE(0)
            return
        }
        writeInt32LE(str.length)
        writeString(str)
    }

    fun writeStringByVarInt32(str: String?, offset: Long = -1L) {
        fixWriteOffset(offset)
        if (str == null) {
            writeVarInt32(0)
            return
        }
        val ary = str.toByteArray(encode).toUByteArray()
        writeVarInt32(ary.size)
        writeBytes(ary)
    }

    fun writeSenBuffer(input: SenBuffer, offset: Long = -1L) {
        val mBuffer = input.toUBytes()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun toUBytes(): UByteArray {
        val tempOffset = baseStream.size().toLong()
        val bytes = baseStream.toByteArray().toUByteArray()
        baseStream = ByteArrayOutputStream().apply { write(bytes.toByteArray()) }
        writeOffset = tempOffset
        return bytes
    }

    fun toBytes(): ByteArray {
        val tempOffset = baseStream.size().toLong()
        val bytes = baseStream.toByteArray()
        baseStream = ByteArrayOutputStream().apply { write(bytes) }
        writeOffset = tempOffset
        return bytes
    }

    fun toStream(): ByteArrayOutputStream {
        return baseStream
    }

    fun toString(encodingType: String = "UTF-8"): String {
        val array = toUBytes()
        return array.toByteArray().toString(Charset.forName(encodingType))
    }

    fun backupReadOffset() {
        tempReadOffset = readOffset
    }

    fun restoreReadOffset() {
        readOffset = tempReadOffset
    }

    fun backupWriteOffset() {
        tempWriteOffset = writeOffset
    }

    fun restoreWriteOffset() {
        writeOffset = tempWriteOffset
    }

    fun createDirectory(outputPath: String) {
        val path = Paths.get(outputPath).parent.toString()
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun outFile(outputPath: String) {
        val newPath = checkPath(outputPath)
        createDirectory(newPath)
        saveFile(newPath)
    }

    fun copy(s: SenBuffer) {
        val array = UByteArray(81920)
        val count = read(array, 0, array.size)
        while (count != 0) {
            s.write(array, 0, count)
        }
    }

    fun ByteArrayOutputStream.read(buffer: UByteArray, offset: Int, count: Int): Int {
        val byteArray = this.toByteArray()
        if (byteArray.size - offset < count) {
            throw IndexOutOfBoundsException("Not enough data available in the stream")
        }
        for (i in 0 until count) {
            buffer[i] = byteArray[offset + i].toUByte()
        }
        return count
    }

    fun read(buffer: UByteArray, offset: Int, count: Int): Int {
        return this.baseStream.read(buffer, offset, count)
    }

    fun write(buffer: UByteArray, offset: Int, count: Int) {
        this.baseStream.write(buffer.toByteArray(), offset, count)
    }

    fun saveFile(path: String) {
        FileOutputStream(path).use { fileStream ->
            baseStream.reset()
            baseStream.writeTo(fileStream)
        }
        close()
    }

    fun close() {
        baseStream.close()
    }

    fun flush() {
        baseStream.flush()
    }
}