package standard

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.pathString

val USER_DIR: String = System.getProperty("user.dir")

@Suppress("unused", "MemberVisibilityCanBePrivate")
class SenBuffer {
    private val tempDir = Paths.get(USER_DIR, "temp").toFile()

    init {
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
    }

    var baseStream: RandomAccessFile
    val tempFile = Paths.get(USER_DIR, "temp", UUID.randomUUID().toString()).toFile()

    var length: Long
        get() = baseStream.length()
        set(value) {
            baseStream.setLength(value)
        }

    var writeOffset: Long = 0L
    var readOffset: Long = 0L
    var imageWidth: Int = 0
    var imageHeight: Int = 0
    var tempReadOffset: Long = 0L
    var tempWriteOffset: Long = 0L
    var filePath: Path? = null
    lateinit var mBuffer: ByteArray

    constructor(stream: RandomAccessFile) {
        baseStream = stream
    }

    constructor() {
        baseStream = RandomAccessFile(tempFile, "rw")
    }

    constructor(bytes: ByteArray) {
        baseStream = RandomAccessFile(tempFile, "rw").apply { write(bytes) }
    }

    constructor(filepath: Path) {
        baseStream = RandomAccessFile(filepath.toFile(), "rw")
        filePath = filepath
    }

    private fun fixReadOffset(offset: Long) {
        if (offset != -1L && offset > -1L) {
            readOffset = offset
            baseStream.seek(readOffset)
        } else if (offset == -1L) {
            baseStream.seek(readOffset)
        } else {
            throw IllegalArgumentException("FixReadOffsetError: Offset not found")
        }
    }

    private fun fixWriteOffset(offset: Long) {
        if (offset != -1L && offset > -1L) {
            writeOffset = offset
            baseStream.seek(writeOffset)
        } else if (offset == -1L) {
            baseStream.seek(writeOffset)
        } else {
            throw IllegalArgumentException("FixWriteOffsetError: Offset not found")
        }
    }

    fun readBytes(count: Int, offset: Long = -1): ByteArray {
        fixReadOffset(offset)
        if (readOffset + count > length) {
            throw IllegalArgumentException("ReadUBytesError: Offset outside bounds of data view")
        }
        val array = ByteArray(count)
        baseStream.read(array, 0, count)
        readOffset += count
        return array
    }

    fun readByte(offset: Long = -1): Byte {
        fixReadOffset(offset)
        if (readOffset + 1 > length) {
            throw IllegalArgumentException("ReadUByteError: Offset outside bounds of data view")
        }
        val array = ByteArray(1)
        baseStream.read(array, 0, 1)
        readOffset += 1
        return array[0]
    }

    fun readString(count: Int, offset: Long = -1, encodingType: String = "UTF-8"): String {
        fixReadOffset(offset)
        return String(readBytes(count), Charset.forName(encodingType))
    }

    fun getBytes(count: Int, offset: Long): ByteArray {
        baseStream.seek(offset)
        if (offset + count > length) {
            throw IllegalArgumentException("GetUBytesError: Offset outside bounds of data view")
        }
        val array = ByteArray(count)
        baseStream.read(array, 0, count)
        baseStream.seek(readOffset)
        return array
    }

    fun readUInt8(offset: Long = -1): UByte {
        fixReadOffset(offset)
        mBuffer = readBytes(1)
        return mBuffer[0].toUByte()
    }

    fun readUInt16LE(offset: Long = -1): UShort {
        fixReadOffset(offset)
        mBuffer = readBytes(2)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).getShort().toUShort()
    }

    fun readUInt16BE(offset: Long = -1): UShort {
        fixReadOffset(offset)
        mBuffer = readBytes(2)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).getShort().toUShort()
    }

    fun readUInt24LE(offset: Long = -1): UInt {
        fixReadOffset(offset)
        mBuffer = readBytes(3)
        return (mBuffer[0].toUInt() and 0xFFu) or
                ((mBuffer[1].toUInt() and 0xFFu) shl 8) or
                ((mBuffer[2].toUInt() and 0xFFu) shl 16)
    }

    fun readUInt24BE(offset: Long = -1): UInt {
        fixReadOffset(offset)
        mBuffer = readBytes(3)
        return (mBuffer[2].toUInt() and 0xFFu) or
                ((mBuffer[1].toUInt() and 0xFFu) shl 8) or
                ((mBuffer[0].toUInt() and 0xFFu) shl 16)
    }

    fun readUInt32LE(offset: Long = -1): UInt {
        fixReadOffset(offset)
        mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt().toUInt()
    }

    fun readUInt32BE(offset: Long = -1): UInt {
        fixReadOffset(offset)
        mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).getInt().toUInt()
    }

    fun readUInt64LE(offset: Long = -1): ULong {
        fixReadOffset(offset)
        mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).getLong().toULong()
    }

    fun readUInt64BE(offset: Long = -1): ULong {
        fixReadOffset(offset)
        mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).getLong().toULong()
    }

    fun readVarUInt32(offset: Long = -1): UInt {
        fixReadOffset(offset)
        return readVarInt32().toUInt()
    }

    fun readP(value: Long? = null): Long {
        return if (value == null) this.readOffset else {
            this.readOffset = value
            this.readOffset
        }
    }

    fun readVarUInt64(offset: Long = -1): ULong {
        fixReadOffset(offset)
        return readVarInt64().toULong()
    }

    fun readInt8(offset: Long = -1): Byte {
        fixReadOffset(offset)
        mBuffer = readBytes(1)
        return mBuffer[0]
    }

    fun readInt16LE(offset: Long = -1): Short {
        fixReadOffset(offset)
        mBuffer = readBytes(2)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).getShort()
    }

    fun readInt16BE(offset: Long = -1): Short {
        fixReadOffset(offset)
        mBuffer = readBytes(2)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).getShort()
    }

    fun readInt24LE(offset: Long = -1): Int {
        fixReadOffset(offset)
        var num = readUInt24LE()
        if ((num and 0x800000u) != 0u) num = num or 0xff000000u
        return num.toInt()
    }

    fun readInt24BE(offset: Long = -1): Int {
        fixReadOffset(offset)
        var num = readUInt24BE()
        if ((num and 0x800000u) != 0u) num = num or 0xff000000u
        return num.toInt()
    }

    fun readInt32LE(offset: Long = -1): Int {
        fixReadOffset(offset)
        mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt()
    }

    fun readInt32BE(offset: Long = -1): Int {
        fixReadOffset(offset)
        mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).getInt()
    }

    fun readInt64LE(offset: Long = -1): Long {
        fixReadOffset(offset)
        mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).getLong()
    }

    fun readInt64BE(offset: Long = -1): Long {
        fixReadOffset(offset)
        mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).getLong()
    }

    fun readVarInt32(offset: Long = -1): Int {
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

    fun readVarInt64(offset: Long = -1): Long {
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

    fun readZigZag32(offset: Long = -1): Int {
        fixReadOffset(offset)
        val n = readVarInt32().toUInt()
        return ((n.toInt() shl 31) shr 31) xor (n.toInt() shr 1)
    }

    fun readZigZag64(offset: Long = -1): Long {
        fixReadOffset(offset)
        val n = readVarInt64().toULong()
        return ((n.toLong() shr 1) xor -(n.toLong() and 0b1))
    }

    fun readFloatLE(offset: Long = -1): Float {
        fixReadOffset(offset)
        mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).float
    }

    fun readFloatBE(offset: Long = -1): Float {
        fixReadOffset(offset)
        mBuffer = readBytes(4)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).float
    }

    fun readDoubleLE(offset: Long = -1): Double {
        fixReadOffset(offset)
        mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).double
    }

    fun readDoubleBE(offset: Long = -1): Double {
        fixReadOffset(offset)
        mBuffer = readBytes(8)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.BIG_ENDIAN).double
    }

    fun readBool(offset: Long = -1): Boolean {
        fixReadOffset(offset)
        mBuffer = readBytes(1)
        return mBuffer[0].toInt() != 0
    }

    fun readStringByEmpty(offset: Long = -1): String {
        fixReadOffset(offset)
        val bytes = mutableListOf<Byte>()
        var tp: Byte
        while (true) {
            tp = readUInt8().toByte()
            if (tp == 0.toByte()) {
                break
            }
            bytes.add(tp)
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    fun getStringByEmpty(offset: Long): String {
        val tempOffset = readOffset
        readOffset = offset
        val str = readStringByEmpty()
        readOffset = tempOffset
        return str
    }

    fun readCharByInt16LE(offset: Long = -1): Char {
        fixReadOffset(offset)
        mBuffer = readBytes(2)
        return ByteBuffer.wrap(mBuffer).order(ByteOrder.LITTLE_ENDIAN).char
    }

    fun readStringByUInt8(offset: Long = -1): String {
        fixReadOffset(offset)
        return readString(readUInt8().toInt())
    }

    fun readStringByInt8(offset: Long = -1): String {
        fixReadOffset(offset)
        return readString(readInt8().toInt())
    }

    fun readStringByUInt16LE(offset: Long = -1): String {
        fixReadOffset(offset)
        return readString(readUInt16LE().toInt())
    }

    fun readStringByInt16LE(offset: Long = -1): String {
        fixReadOffset(offset)
        return readString(readInt16LE().toInt())
    }

    fun readStringByUInt32LE(offset: Long = -1): String {
        fixReadOffset(offset)
        return readString(readUInt32LE().toInt())
    }

    fun readStringByInt32LE(offset: Long = -1): String {
        fixReadOffset(offset)
        return readString(readInt32LE())
    }

    fun readStringByVarInt32(offset: Long = -1): String {
        fixReadOffset(offset)
        return readString(readVarInt32())
    }

    fun peekUInt8(offset: Long = -1): UByte {
        val num = readUInt8(offset)
        readOffset--
        return num
    }

    fun peekInt8(offset: Long = -1): Byte {
        val num = readInt8(offset)
        readOffset--
        return num
    }

    fun peekUInt16LE(offset: Long = -1): UShort {
        val num = readUInt16LE(offset)
        readOffset -= 2
        return num
    }

    fun peekUInt16BE(offset: Long = -1): UShort {
        val num = readUInt16BE(offset)
        readOffset -= 2
        return num
    }

    fun peekInt16LE(offset: Long = -1): Short {
        val num = readInt16LE(offset)
        readOffset -= 2
        return num
    }

    fun peekInt16BE(offset: Long = -1): Short {
        val num = readInt16BE(offset)
        readOffset -= 2
        return num
    }

    fun peekUInt24LE(offset: Long = -1): UInt {
        val num = readUInt24LE(offset)
        readOffset -= 3
        return num
    }

    fun peekUInt24BE(offset: Long = -1): UInt {
        val num = readUInt24BE(offset)
        readOffset -= 3
        return num
    }

    fun peekInt24LE(offset: Long = -1): Int {
        val num = readInt24LE(offset)
        readOffset -= 3
        return num
    }

    fun peekInt24BE(offset: Long = -1): Int {
        val num = readInt24BE(offset)
        readOffset -= 3
        return num
    }

    fun peekUInt32LE(offset: Long = -1): UInt {
        val num = readUInt32LE(offset)
        readOffset -= 4
        return num
    }

    fun peekUInt32BE(offset: Long = -1): UInt {
        val num = readUInt32BE(offset)
        readOffset -= 4
        return num
    }

    fun peekInt32LE(offset: Long = -1): Int {
        val num = readInt32LE(offset)
        readOffset -= 4
        return num
    }

    fun peekInt32BE(offset: Long = -1): Int {
        val num = readInt32BE(offset)
        readOffset -= 4
        return num
    }

    fun peekString(count: Int, offset: Long = -1, encodingType: String = "UTF-8"): String {
        val str = readString(count, offset, encodingType)
        readOffset -= count
        return str
    }

    fun writeBytes(array: ByteArray, offset: Long = -1) {
        fixWriteOffset(offset)
        val length = array.size
        baseStream.write(array, 0, length)
        writeOffset += length
    }

    fun writeByte(byte: Byte, offset: Long = -1) {
        fixWriteOffset(offset)
        baseStream.write(byteArrayOf(byte), 0, 1)
        writeOffset += 1
    }

    fun writeString(str: String, offset: Long = -1, encodingType: String = "UTF-8") {
        fixWriteOffset(offset)
        val strBytes = str.toByteArray(Charset.forName(encodingType))
        writeBytes(strBytes)
    }

    fun writeStringByEmpty(str: String?, offset: Long = -1) {
        if (str == null) {
            writeUInt8(0u)
            return
        }
        writeString(str, offset)
        writeUInt8(0u)
    }

    fun writeNull(count: Int, offset: Long = -1) {
        if (count < 0) throw Exception()
        if (count == 0) return
        fixWriteOffset(offset)
        val nullBytes = ByteArray(count)
        writeBytes(nullBytes)
    }

    fun writeStringFourUByte(str: String, offset: Long = -1) {
        fixWriteOffset(offset)
        val strLength = str.length
        val strBytes = ByteArray(strLength * 4 + 4)
        for (i in 0 until strLength) {
            strBytes[i * 4] = str[i].code.toByte()
        }
        writeBytes(strBytes)
    }

    fun setBytes(array: ByteArray, offset: Long, overwriteOffset: Boolean) {
        val length = array.size
        if (overwriteOffset) {
            fixWriteOffset(offset)
        } else {
            baseStream.seek(offset)
        }
        baseStream.write(array, 0, length)
        baseStream.seek(writeOffset)
    }

    fun writeUInt8(number: UByte, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = byteArrayOf(number.toByte())
        writeBytes(mBuffer)
    }

    fun writeUInt16LE(number: UShort, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(number.toShort())
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeUInt16BE(number: UShort, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(2)
            .order(ByteOrder.BIG_ENDIAN)
            .putShort(number.toShort())
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeUInt24LE(number: UInt, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = byteArrayOf(
            (number and 0xFFu).toByte(),
            ((number shr 8) and 0xFFu).toByte(),
            ((number shr 16) and 0xFFu).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeUInt24BE(number: UInt, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = byteArrayOf(
            ((number shr 16) and 0xFFu).toByte(),
            ((number shr 8) and 0xFFu).toByte(),
            (number and 0xFFu).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeUInt32LE(number: UInt, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(number.toInt())
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeUInt32BE(number: UInt, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(number.toInt())
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeUInt64LE(number: ULong, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putLong(number.toLong())
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeUInt64BE(number: ULong, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(8)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(number.toLong())
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeFloatLE(number: Float, offset: Long = -1) {
        mBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(number).array()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeFloatBE(number: Float, offset: Long = -1) {
        mBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(number).array()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeDoubleLE(number: Double, offset: Long = -1) {
        mBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(number).array()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeDoubleBE(number: Double, offset: Long = -1) {
        mBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(number).array()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeUVarInt32(number: UInt, offset: Long = -1) {
        var num = number
        val bytes = mutableListOf<Byte>()
        while (num >= 128u) {
            bytes.add((num or 0x80u).toByte())
            num = num shr 7
        }
        bytes.add(num.toByte())
        mBuffer = bytes.toByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeUVarInt64(number: ULong, offset: Long = -1) {
        var num = number
        val bytes = mutableListOf<Byte>()
        while (num >= 128uL) {
            bytes.add((num or 0x80uL).toByte())
            num = num shr 7
        }
        bytes.add(num.toByte())
        mBuffer = bytes.toByteArray()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeInt8(number: Byte, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = byteArrayOf(number)
        writeBytes(mBuffer)
    }

    fun writeInt16LE(number: Short, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(number)
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeInt16BE(number: Short, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(2)
            .order(ByteOrder.BIG_ENDIAN)
            .putShort(number)
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeInt24LE(number: Int, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = byteArrayOf(
            (number and 0xFF).toByte(),
            ((number shr 8) and 0xFF).toByte(),
            ((number shr 16) and 0xFF).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeInt24BE(number: Int, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = byteArrayOf(
            ((number shr 16) and 0xFF).toByte(),
            ((number shr 8) and 0xFF).toByte(),
            (number and 0xFF).toByte()
        )
        writeBytes(mBuffer)
    }

    fun writeInt32LE(number: Int, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(number)
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeInt32BE(number: Int, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(number)
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeBigInt64LE(number: Long, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putLong(number)
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeBigInt64BE(number: Long, offset: Long = -1) {
        fixWriteOffset(offset)
        mBuffer = ByteBuffer.allocate(8)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(number)
            .flip()
            .array()
        writeBytes(mBuffer)
    }

    fun writeVarInt32(number: Int, offset: Long = -1) {
        var num = number
        fixWriteOffset(offset)
        while (num >= 128) {
            writeUInt8((num or 0x80).toUByte())
            num = num shr 7
        }
        writeUInt8(num.toUByte())
    }

    fun writeVarInt64(number: Long, offset: Long = -1) {
        var num = number
        fixWriteOffset(offset)
        while (num >= 128L) {
            writeUInt8((num or 0x80L).toUByte())
            num = num shr 7
        }
        writeUInt8(num.toUByte())
    }

    fun writeBool(value: Boolean, offset: Long = -1) {
        mBuffer = byteArrayOf(if (value) 1 else 0)
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun writeZigZag32(number: Int, offset: Long = -1) {
        fixWriteOffset(offset)
        writeVarInt32((number shl 1) xor (number shr 31))
    }

    fun slice(begin: Long, end: Long) {
        if (begin < 0 || end < begin || end > length) {
            throw IllegalArgumentException("Invalid Buffer Slice")
        }
        val length = end - begin
        val buffer = ByteArray(length.toInt())
        val fileName = UUID.randomUUID()
        baseStream.seek(begin)
        baseStream.read(buffer, 0, length.toInt())
        baseStream = RandomAccessFile(fileName.toString(), "rw").apply { write(buffer) }
        return
    }

    fun writeZigZag64(number: Long, offset: Long = -1) {
        fixWriteOffset(offset)
        writeVarInt64((number shl 1) xor (number shr 63))
    }

    fun writeCharByInt16LE(charStr: Char, offset: Long = -1) {
        val strBytes = charStr.toString().toByteArray(Charsets.UTF_16LE)
        fixWriteOffset(offset)
        writeBytes(strBytes)
    }

    fun writeStringByUInt8(str: String?, offset: Long = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt8(0u)
            return
        }
        writeUInt8(str.length.toUByte())
        writeString(str)
    }

    fun writeStringByInt8(str: String?, offset: Long = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt8(0)
            return
        }
        writeInt8(str.length.toByte())
        writeString(str)
    }

    fun writeStringByUInt16LE(str: String?, offset: Long = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt16LE(0u)
            return
        }
        writeUInt16LE(str.length.toUShort())
        writeString(str)
    }

    fun writeStringByInt16LE(str: String?, offset: Long = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt16LE(0)
            return
        }
        writeInt16LE(str.length.toShort())
        writeString(str)
    }

    fun writeStringByUInt32LE(str: String?, offset: Long = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt32LE(0u)
            return
        }
        writeUInt32LE(str.length.toUInt())
        writeString(str)
    }

    fun writeStringByInt32LE(str: String?, offset: Long = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt32LE(0)
            return
        }
        writeInt32LE(str.length)
        writeString(str)
    }

    fun writeStringByVarInt32(str: String?, offset: Long = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeVarInt32(0)
            return
        }
        val ary = str.toByteArray(Charsets.UTF_8)
        writeVarInt32(ary.size)
        writeBytes(ary)
    }

    fun writeSenBuffer(input: SenBuffer, offset: Long = -1) {
        mBuffer = input.toBytes()
        fixWriteOffset(offset)
        writeBytes(mBuffer)
    }

    fun toBytes(): ByteArray {
        baseStream.seek(0)
        val bytes = ByteArray(length.toInt())
        baseStream.readFully(bytes)
        return bytes
    }

    fun toStream(): RandomAccessFile {
        flush()
        return baseStream
    }

    fun toString(encodingType: String = "UTF-8"): String {
        val array = toBytes()
        return array.toString(Charset.forName(encodingType))
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

    fun createDirectory(outputPath: Path) {
        outputPath.toFile().parentFile?.mkdirs()
    }

    fun outFile(outputPath: Path) {
        createDirectory(outputPath)
        saveFile(outputPath)
    }

    fun copy(s: SenBuffer) {
        var array = ByteArray(81920)
        val count = baseStream.read(array, 0, 81920)
        while (count != 0) {
            s.baseStream.write(array, 0, array.size)
            array = ByteArray(81920)
            baseStream.read(array, 0, 81920)
        }
    }

    fun saveFile(path: Path) {
        BufferedOutputStream(FileOutputStream(path.pathString)).use { it.write(toBytes()); it.flush() }
        flush()
        close()
    }

    fun close() {
        baseStream.close()
        if (tempFile.exists()) tempFile.delete()
    }

    fun flush() {
        baseStream.fd.sync()
    }
}