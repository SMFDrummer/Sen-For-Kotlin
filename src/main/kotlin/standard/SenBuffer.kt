package standard

import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.file.Path

@Suppress("unused", "MemberVisibilityCanBePrivate")
@OptIn(ExperimentalUnsignedTypes::class)
class SenBuffer : Stream {
    var writeOffset: Int = 0
    var readOffset: Int = 0
    var imageWidth: Int = 0
    var imageHeight: Int = 0
    var tempReadOffset: Int = 0
    var tempWriteOffset: Int = 0
    var mBuffer: ByteArray? = null

    constructor() : super()
    constructor(value: Int) : super(value)
    constructor(ubyteArray: UByteArray) : super(ubyteArray)
    constructor(filePath: Path) : super(filePath)

    private fun fixReadOffset(offset: Int) {
        if (offset != -1 && offset > -1) {
            readOffset = offset
            position = readOffset
        } else if (offset == -1) {
            position = readOffset
        } else {
            throw IllegalArgumentException("FixReadOffsetError: Offset not found")
        }
    }

    private fun fixWriteOffset(offset: Int) {
        if (offset != -1 && offset > -1) {
            writeOffset = offset
            position = writeOffset
        } else if (offset == -1) {
            position = writeOffset
        } else {
            throw IllegalArgumentException("FixWriteOffsetError: Offset not found")
        }
    }

    fun readUBytes(count: Int, offset: Int = -1): UByteArray {
        fixReadOffset(offset)
        if (readOffset + count > this.size) {
            throw IllegalArgumentException("ReadUBytesError: Offset outside bounds of data view")
        }
        val array = UByteArray(count)
        read(array, 0, count)
        readOffset += count
        return array.toUByteArray()
    }

    fun readUByte(offset: Int = -1): UByte {
        fixReadOffset(offset)
        if (readOffset + 1 > this.size) {
            throw IllegalArgumentException("ReadUByteError: Offset outside bounds of data view")
        }
        val array = UByteArray(1)
        read(array, 0, 1)
        readOffset += 1
        return array[0]
    }

    fun readString(count: Int, offset: Int = -1, encodingType: String = "UTF-8"): String {
        fixReadOffset(offset)
        return String(readUBytes(count).toByteArray(), Charset.forName(encodingType))
    }

    fun getUBytes(count: Int, offset: Int): UByteArray {
        position = offset
        if (offset + count > this.size) {
            throw IllegalArgumentException("GetUBytesError: Offset outside bounds of data view")
        }
        val array = UByteArray(count)
        read(array, 0, count)
        position = readOffset
        return array.toUByteArray()
    }

    fun readUInt8(offset: Int = -1): UByte {
        fixReadOffset(offset)
        val mBuffer = readUBytes(1)
        return mBuffer[0]
    }

    fun readUInt16LE(offset: Int = -1): UShort {
        fixReadOffset(offset)
        val mBuffer = readUBytes(2)
        return mBuffer[0].toUShort() or (mBuffer[1].toUInt() shl 8).toUShort()
    }

    fun readUInt16BE(offset: Int = -1): UShort {
        fixReadOffset(offset)
        val mBuffer = readUBytes(2)
        return mBuffer[1].toUShort() or (mBuffer[0].toUInt() shl 8).toUShort()
    }

    fun readUInt24LE(offset: Int = -1): UInt {
        fixReadOffset(offset)
        val mBuffer = readUBytes(3)
        return mBuffer[0].toUInt() or (mBuffer[1].toUInt() shl 8) or (mBuffer[2].toUInt() shl 16)
    }

    fun readUInt24BE(offset: Int = -1): UInt {
        fixReadOffset(offset)
        val mBuffer = readUBytes(3)
        return mBuffer[2].toUInt() or (mBuffer[1].toUInt() shl 8) or (mBuffer[0].toUInt() shl 16)
    }

    fun readUInt32LE(offset: Int = -1): UInt {
        fixReadOffset(offset)
        val mBuffer = readUBytes(4)
        return mBuffer[0].toUInt() or (mBuffer[1].toUInt() shl 8) or (mBuffer[2].toUInt() shl 16) or (mBuffer[3].toUInt() shl 24)
    }

    fun readUInt32BE(offset: Int = -1): UInt {
        fixReadOffset(offset)
        val mBuffer = readUBytes(4)
        return mBuffer[3].toUInt() or (mBuffer[2].toUInt() shl 8) or (mBuffer[1].toUInt() shl 16) or (mBuffer[0].toUInt() shl 24)
    }

    fun readUInt64LE(offset: Int = -1): ULong {
        fixReadOffset(offset)
        val mBuffer = readUBytes(8)
        return ((mBuffer[4].toULong() or (mBuffer[5].toULong() shl 8) or (mBuffer[6].toULong() shl 16) or (mBuffer[7].toULong() shl 24)) shl 32) or (mBuffer[0].toULong() or (mBuffer[1].toULong() shl 8) or (mBuffer[2].toULong() shl 16) or (mBuffer[3].toULong() shl 24))
    }

    fun readUInt64BE(offset: Int = -1): ULong {
        fixReadOffset(offset)
        val mBuffer = readUBytes(8)
        return ((mBuffer[3].toULong() or (mBuffer[2].toULong() shl 8) or (mBuffer[1].toULong() shl 16) or (mBuffer[0].toULong() shl 24)) shl 32) or (mBuffer[7].toULong() or (mBuffer[6].toULong() shl 8) or (mBuffer[5].toULong() shl 16) or (mBuffer[4].toULong() shl 24))
    }

    fun readVarUInt32(offset: Int = -1): UInt {
        fixReadOffset(offset)
        return readVarInt32().toUInt()
    }

    fun readP(value: Int? = null): Int {
        return if (value == null) this.readOffset else {
            this.readOffset = value
            this.readOffset
        }
    }

    fun readVarUInt64(offset: Int = -1): ULong {
        fixReadOffset(offset)
        return readVarInt64().toULong()
    }

    fun readInt8(offset: Int = -1): Byte {
        fixReadOffset(offset)
        val mBuffer = readUBytes(1)
        return mBuffer[0].toByte()
    }

    fun readInt16LE(offset: Int = -1): Short {
        fixReadOffset(offset)
        val mBuffer = readUBytes(2)
        return (mBuffer[0].toInt() or (mBuffer[1].toInt() shl 8)).toShort()
    }

    fun readInt16BE(offset: Int = -1): Short {
        fixReadOffset(offset)
        val mBuffer = readUBytes(2)
        return (mBuffer[1].toInt() or (mBuffer[0].toInt() shl 8)).toShort()
    }

    fun readInt24LE(offset: Int = -1): Int {
        fixReadOffset(offset)
        var num = readUInt24LE()
        if ((num and 0x800000u) != 0u) num = num or 0xff000000u
        return num.toInt()
    }

    fun readInt24BE(offset: Int = -1): Int {
        fixReadOffset(offset)
        var num = readUInt24BE()
        if ((num and 0x800000u) != 0u) num = num or 0xff000000u
        return num.toInt()
    }

    fun readInt32LE(offset: Int = -1): Int {
        fixReadOffset(offset)
        val mBuffer = readUBytes(4)
        return mBuffer[0].toInt() or (mBuffer[1].toInt() shl 8) or (mBuffer[2].toInt() shl 16) or (mBuffer[3].toInt() shl 24)
    }

    fun readInt32BE(offset: Int = -1): Int {
        fixReadOffset(offset)
        val mBuffer = readUBytes(4)
        return mBuffer[3].toInt() or (mBuffer[2].toInt() shl 8) or (mBuffer[1].toInt() shl 16) or (mBuffer[0].toInt() shl 24)
    }

    fun readInt64LE(offset: Int = -1): Long {
        fixReadOffset(offset)
        val mBuffer = readUBytes(8)
        return ((mBuffer[4].toLong() or (mBuffer[5].toLong() shl 8) or (mBuffer[6].toLong() shl 16) or (mBuffer[7].toLong() shl 24)) shl 32) or (mBuffer[0].toLong() or (mBuffer[1].toLong() shl 8) or (mBuffer[2].toLong() shl 16) or (mBuffer[3].toLong() shl 24))
    }

    fun readInt64BE(offset: Int = -1): Long {
        fixReadOffset(offset)
        val mBuffer = readUBytes(8)
        return ((mBuffer[3].toLong() or (mBuffer[2].toLong() shl 8) or (mBuffer[1].toLong() shl 16) or (mBuffer[0].toLong() shl 24)) shl 32) or (mBuffer[7].toLong() or (mBuffer[6].toLong() shl 8) or (mBuffer[5].toLong() shl 16) or (mBuffer[4].toLong() shl 24))
    }

    fun readVarInt32(offset: Int = -1): Int {
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

    fun readVarInt64(offset: Int = -1): Long {
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

    fun readZigZag32(offset: Int = -1): Int {
        fixReadOffset(offset)
        val n = readVarInt32().toUInt()
        return ((n.toInt() shl 31) shr 31) xor (n.toInt() shr 1)
    }

    fun readZigZag64(offset: Int = -1): Long {
        fixReadOffset(offset)
        val n = readVarInt64().toULong()
        return ((n.toLong() shr 1) xor -(n.toLong() and 0b1))
    }

    fun readFloatLE(offset: Int = -1): Float {
        fixReadOffset(offset)
        val mBuffer = readUBytes(4)
        return ByteBuffer.wrap(mBuffer.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).float
    }

    fun readFloatBE(offset: Int = -1): Float {
        fixReadOffset(offset)
        val mBuffer = readUBytes(4)
        return ByteBuffer.wrap(mBuffer.toByteArray()).order(ByteOrder.BIG_ENDIAN).float
    }

    fun readDoubleLE(offset: Int = -1): Double {
        fixReadOffset(offset)
        val mBuffer = readUBytes(8)
        return ByteBuffer.wrap(mBuffer.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).double
    }

    fun readDoubleBE(offset: Int = -1): Double {
        fixReadOffset(offset)
        val mBuffer = readUBytes(8)
        return ByteBuffer.wrap(mBuffer.toByteArray()).order(ByteOrder.BIG_ENDIAN).double
    }

    fun readBool(offset: Int = -1): Boolean {
        fixReadOffset(offset)
        val mBuffer = readUBytes(1)
        return mBuffer[0].toInt() != 0
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun readStringByEmpty(offset: Int = -1): String {
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

    fun getStringByEmpty(offset: Int): String {
        val tempOffset = readOffset
        readOffset = offset
        val str = readStringByEmpty()
        readOffset = tempOffset
        return str
    }

    fun readCharByInt16LE(offset: Int = -1): Char {
        fixReadOffset(offset)
        val mBuffer = readUBytes(2)
        return ByteBuffer.wrap(mBuffer.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).char
    }

    fun readStringByUInt8(offset: Int = -1): String {
        fixReadOffset(offset)
        return readString(readUInt8().toInt())
    }

    fun readStringByInt8(offset: Int = -1): String {
        fixReadOffset(offset)
        return readString(readInt8().toInt())
    }

    fun readStringByUInt16LE(offset: Int = -1): String {
        fixReadOffset(offset)
        return readString(readUInt16LE().toInt())
    }

    fun readStringByInt16LE(offset: Int = -1): String {
        fixReadOffset(offset)
        return readString(readInt16LE().toInt())
    }

    fun readStringByUInt32LE(offset: Int = -1): String {
        fixReadOffset(offset)
        return readString(readUInt32LE().toInt())
    }

    fun readStringByInt32LE(offset: Int = -1): String {
        fixReadOffset(offset)
        return readString(readInt32LE())
    }

    fun readStringByVarInt32(offset: Int = -1): String {
        fixReadOffset(offset)
        return readString(readVarInt32())
    }

    fun peekUInt8(offset: Int = -1): UByte {
        val num = readUInt8(offset)
        readOffset--
        return num
    }

    fun peekInt8(offset: Int = -1): Byte {
        val num = readInt8(offset)
        readOffset--
        return num
    }

    fun peekUInt16LE(offset: Int = -1): UShort {
        val num = readUInt16LE(offset)
        readOffset -= 2
        return num
    }

    fun peekUInt16BE(offset: Int = -1): UShort {
        val num = readUInt16BE(offset)
        readOffset -= 2
        return num
    }

    fun peekInt16LE(offset: Int = -1): Short {
        val num = readInt16LE(offset)
        readOffset -= 2
        return num
    }

    fun peekInt16BE(offset: Int = -1): Short {
        val num = readInt16BE(offset)
        readOffset -= 2
        return num
    }

    fun peekUInt24LE(offset: Int = -1): UInt {
        val num = readUInt24LE(offset)
        readOffset -= 3
        return num
    }

    fun peekUInt24BE(offset: Int = -1): UInt {
        val num = readUInt24BE(offset)
        readOffset -= 3
        return num
    }

    fun peekInt24LE(offset: Int = -1): Int {
        val num = readInt24LE(offset)
        readOffset -= 3
        return num
    }

    fun peekInt24BE(offset: Int = -1): Int {
        val num = readInt24BE(offset)
        readOffset -= 3
        return num
    }

    fun peekUInt32LE(offset: Int = -1): UInt {
        val num = readUInt32LE(offset)
        readOffset -= 4
        return num
    }

    fun peekUInt32BE(offset: Int = -1): UInt {
        val num = readUInt32BE(offset)
        readOffset -= 4
        return num
    }

    fun peekInt32LE(offset: Int = -1): Int {
        val num = readInt32LE(offset)
        readOffset -= 4
        return num
    }

    fun peekInt32BE(offset: Int = -1): Int {
        val num = readInt32BE(offset)
        readOffset -= 4
        return num
    }

    fun peekString(count: Int, offset: Int = -1, encodingType: String = "UTF-8"): String {
        val str = readString(count, offset, encodingType)
        readOffset -= count
        return str
    }

    fun writeUBytes(array: UByteArray, offset: Int = -1) {
        fixWriteOffset(offset)
        val length = array.size
        write(array.toByteArray(), 0, length)
        writeOffset += length
    }

    fun writeUByte(byte: UByte, offset: Int = -1) {
        fixWriteOffset(offset)
        write(ubyteArrayOf(byte).toByteArray(), 0, 1)
        writeOffset += 1
    }

    fun writeString(str: String, offset: Int = -1, encodingType: String = "UTF-8") {
        fixWriteOffset(offset)
        val strBytes = str.toByteArray(Charset.forName(encodingType)).toUByteArray()
        writeUBytes(strBytes)
    }

    fun writeStringByEmpty(str: String?, offset: Int = -1) {
        if (str == null) {
            writeUInt8(0u)
            return
        }
        writeString(str, offset)
        writeUInt8(0u)
    }

    fun writeNull(count: Int, offset: Int = -1) {
        if (count < 0) throw Exception()
        if (count == 0) return
        fixWriteOffset(offset)
        val nullBytes = UByteArray(count)
        writeUBytes(nullBytes)
    }

    fun writeStringFourUByte(str: String, offset: Int = -1) {
        fixWriteOffset(offset)
        val strLength = str.length
        val strBytes = UByteArray(strLength * 4 + 4)
        for (i in 0 until strLength) {
            strBytes[i * 4] = str[i].code.toUByte()
        }
        writeUBytes(strBytes)
    }

    fun setUBytes(array: UByteArray, offset: Int, overwriteOffset: Boolean) {
        val length = array.size
        if (overwriteOffset) {
            fixWriteOffset(offset)
        } else {
            position = offset
        }
        write(array.toByteArray(), 0, length)
        position = writeOffset
    }

    fun writeUInt8(number: UByte, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(number)
        writeUBytes(mBuffer)
    }

    fun writeUInt16LE(number: UShort, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number and 0xFFu).toUByte(), (number.toUInt() shr 8).toUByte())
        writeUBytes(mBuffer)
    }

    fun writeUInt16BE(number: UShort, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number.toUInt() shr 8).toUByte(), (number and 0xFFu).toUByte())
        writeUBytes(mBuffer)
    }

    fun writeUInt24LE(number: UInt, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number and 0xFFu).toUByte(), (number shr 8).toUByte(), (number shr 16).toUByte())
        writeUBytes(mBuffer)
    }

    fun writeUInt24BE(number: UInt, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number shr 16).toUByte(), (number shr 8).toUByte(), (number and 0xFFu).toUByte())
        writeUBytes(mBuffer)
    }

    fun writeUInt32LE(number: UInt, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number and 0xFFu).toUByte(),
            (number shr 8).toUByte(),
            (number shr 16).toUByte(),
            (number shr 24).toUByte()
        )
        writeUBytes(mBuffer)
    }

    fun writeUInt32BE(number: UInt, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number shr 24).toUByte(),
            (number shr 16).toUByte(),
            (number shr 8).toUByte(),
            (number and 0xFFu).toUByte()
        )
        writeUBytes(mBuffer)
    }

    fun writeUInt64LE(number: ULong, offset: Int = -1) {
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
        writeUBytes(mBuffer)
    }

    fun writeUInt64BE(number: ULong, offset: Int = -1) {
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
        writeUBytes(mBuffer)
    }

    fun writeFloatLE(number: Float, offset: Int = -1) {
        val mBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun writeFloatBE(number: Float, offset: Int = -1) {
        val mBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun writeDoubleLE(number: Double, offset: Int = -1) {
        val mBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun writeDoubleBE(number: Double, offset: Int = -1) {
        val mBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(number).array().toUByteArray()
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun writeUVarInt32(number: UInt, offset: Int = -1) {
        var num = number
        val bytes = mutableListOf<UByte>()
        while (num >= 128u) {
            bytes.add((num or 0x80u).toUByte())
            num = num shr 7
        }
        bytes.add(num.toUByte())
        val mBuffer = bytes.toUByteArray()
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun writeUVarInt64(number: ULong, offset: Int = -1) {
        var num = number
        val bytes = mutableListOf<UByte>()
        while (num >= 128uL) {
            bytes.add((num or 0x80uL).toUByte())
            num = num shr 7
        }
        bytes.add(num.toUByte())
        val mBuffer = bytes.toUByteArray()
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun writeInt8(number: Byte, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(number.toUByte())
        writeUBytes(mBuffer)
    }

    fun writeInt16LE(number: Short, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(number.toUByte(), (number.toInt() shr 8).toUByte())
        writeUBytes(mBuffer)
    }

    fun writeInt16BE(number: Short, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number.toInt() shr 8).toUByte(), number.toUByte())
        writeUBytes(mBuffer)
    }

    fun writeInt24LE(number: Int, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(number.toUByte(), (number shr 8).toUByte(), (number shr 16).toUByte())
        writeUBytes(mBuffer)
    }

    fun writeInt24BE(number: Int, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf((number shr 16).toUByte(), (number shr 8).toUByte(), number.toUByte())
        writeUBytes(mBuffer)
    }

    fun writeInt32LE(number: Int, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            number.toUByte(),
            (number shr 8).toUByte(),
            (number shr 16).toUByte(),
            (number shr 24).toUByte()
        )
        writeUBytes(mBuffer)
    }

    fun writeInt32BE(number: Int, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number shr 24).toUByte(),
            (number shr 16).toUByte(),
            (number shr 8).toUByte(),
            number.toUByte()
        )
        writeUBytes(mBuffer)
    }

    fun writeBigInt64LE(number: Long, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            number.toUByte(),
            (number shr 8).toUByte(),
            (number shr 16).toUByte(),
            (number shr 24).toUByte(),
            (number shr 32).toUByte(),
            (number shr 40).toUByte(),
            (number shr 48).toUByte(),
            (number shr 56).toUByte()
        )
        writeUBytes(mBuffer)
    }

    fun writeBigInt64BE(number: Long, offset: Int = -1) {
        fixWriteOffset(offset)
        val mBuffer = ubyteArrayOf(
            (number shr 56).toUByte(),
            (number shr 48).toUByte(),
            (number shr 40).toUByte(),
            (number shr 32).toUByte(),
            (number shr 24).toUByte(),
            (number shr 16).toUByte(),
            (number shr 8).toUByte(),
            number.toUByte()
        )
        writeUBytes(mBuffer)
    }

    fun writeVarInt32(number: Int, offset: Int = -1) {
        var num = number.toUInt()
        fixWriteOffset(offset)
        while (num >= 128u) {
            writeUInt8((num or 0x80u).toUByte())
            num = num shr 7
        }
        writeUInt8(num.toUByte())
    }

    fun writeVarInt64(number: Long, offset: Int = -1) {
        var num = number.toULong()
        fixWriteOffset(offset)
        while (num >= 128uL) {
            writeUInt8((num or 0x80uL).toUByte())
            num = num shr 7
        }
        writeUInt8(num.toUByte())
    }

    fun writeBool(value: Boolean, offset: Int = -1) {
        val mBuffer = ubyteArrayOf(if (value) 1u else 0u)
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun writeZigZag32(number: Int, offset: Int = -1) {
        fixWriteOffset(offset)
        writeVarInt32((number shl 1) xor (number shr 31))
    }

    fun slice(begin: Int, end: Int) {
        if (begin < 0 || end < begin || end > size) {
            throw IllegalArgumentException("Invalid Buffer Slice")
        }
        val length = end - begin
        val buffer = UByteArray(length)
        seek(begin, SeekOrigin.Begin)
        read(buffer, 0, length)
        stream = ByteArrayOutputStream(buffer.size).apply { write(buffer.toUByteArray().toByteArray()) }
        return
    }

    fun writeZigZag64(number: Long, offset: Int = -1) {
        fixWriteOffset(offset)
        writeVarInt64((number shl 1) xor (number shr 63))
    }

    fun writeCharByInt16LE(charStr: Char, offset: Int = -1) {
        val strBytes = charStr.toString().toByteArray(Charsets.UTF_16LE).toUByteArray()
        fixWriteOffset(offset)
        writeUBytes(strBytes)
    }

    fun writeStringByUInt8(str: String?, offset: Int = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt8(0u)
            return
        }
        writeUInt8(str.length.toUByte())
        writeString(str)
    }

    fun writeStringByInt8(str: String?, offset: Int = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt8(0)
            return
        }
        writeInt8(str.length.toByte())
        writeString(str)
    }

    fun writeStringByUInt16LE(str: String?, offset: Int = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt16LE(0u)
            return
        }
        writeUInt16LE(str.length.toUShort())
        writeString(str)
    }

    fun writeStringByInt16LE(str: String?, offset: Int = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt16LE(0)
            return
        }
        writeInt16LE(str.length.toShort())
        writeString(str)
    }

    fun writeStringByUInt32LE(str: String?, offset: Int = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeUInt32LE(0u)
            return
        }
        writeUInt32LE(str.length.toUInt())
        writeString(str)
    }

    fun writeStringByInt32LE(str: String?, offset: Int = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeInt32LE(0)
            return
        }
        writeInt32LE(str.length)
        writeString(str)
    }

    fun writeStringByVarInt32(str: String?, offset: Int = -1) {
        fixWriteOffset(offset)
        if (str == null) {
            writeVarInt32(0)
            return
        }
        val ary = str.toByteArray(Charsets.UTF_8).toUByteArray()
        writeVarInt32(ary.size)
        writeUBytes(ary)
    }

    fun writeSenBuffer(input: SenBuffer, offset: Int = -1) {
        val mBuffer = input.toUBytes()
        fixWriteOffset(offset)
        writeUBytes(mBuffer)
    }

    fun toUBytes(): UByteArray {
        return stream.toByteArray().toUByteArray()
    }

    fun toStream(): ByteArrayOutputStream {
        flush()
        return stream
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

    fun createDirectory(outputPath: Path) {
        val path = outputPath.parent.toString()
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    fun outFile(outputPath: Path) {
        createDirectory(outputPath)
        saveFile(outputPath)
    }

    fun copy(s: SenBuffer) {
        var array = UByteArray(81920)
        read(array, 0, 81920)
        while (array.isNotEmpty()) {
            s.write(array.toUByteArray().toByteArray(), 0, array.size)
            array = UByteArray(81920)
            read(array, 0, 81920)
        }
    }

    fun saveFile(path: Path) {
        BufferedOutputStream(FileOutputStream(path.toString())).use { bufferedStream -> bufferedStream.write(stream.toByteArray()) }
        close()
    }

    fun close() {
        stream.close()
    }

    fun flush() {
        stream.flush()
    }
}