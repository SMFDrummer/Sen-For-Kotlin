package RTON

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import standard.Crypto
import standard.JsonFeature
import standard.SenBuffer
import standard.by
import kotlin.io.path.pathString

object RTONProcession {
    private const val HEADER = "RTON"
    private const val VERSION: UInt = 1u
    private const val EOR = "DONE"
    private val R0x90List = mutableListOf<String>()
    private val R0x92List = mutableListOf<String>()
    private val NULL = null
    private const val RTID_BEGIN = "RTID("
    private const val RTID_END = ")"
    private const val RTID_0 = "RTID(0)"
    private const val RTID_2 = "RTID(%s.%s.%08x@%s)"
    private const val RTID_3 = "RTID(%s@%s)"
    private const val BINARY = "\$BINARY(\"%s\", %s)"
    private const val BINARY_BEGIN = "\$BINARY(\""
    private const val BINARY_END = ")"
    private const val BINARY_MIDDLE = "\", "
    private lateinit var tempString: String

    //---------------------- rton crypto start ----------------------

    fun decrypt(rtonFile: SenBuffer): SenBuffer {
        if (rtonFile.readInt16LE() != 0x10.toShort()) throw Exception("RTON is not encrypted")
        return SenBuffer(Crypto.RTON().decrypt(rtonFile.getBytes((rtonFile.length - 2).toInt(), 2)))
    }

    fun encrypt(rtonFile: SenBuffer): SenBuffer {
        val iv = Crypto.RTON().iv.toByteArray()
        val padSize = iv.size - ((rtonFile.length + iv.size - 1) % iv.size + 1)
        rtonFile.writeNull(padSize.toInt())
        val senBuffer = SenBuffer()
        senBuffer.writeInt16LE(0x10)
        senBuffer.writeBytes(Crypto.RTON().encrypt(rtonFile.toBytes()))
        return senBuffer
    }

    //----------------------- rton crypto end -----------------------


    //---------------------- rton 2 json start ----------------------

    fun decode(rtonFile: SenBuffer): JsonObject {
        R0x90List.clear()
        R0x92List.clear()
        var jsonObject = mutableMapOf<String, JsonElement>()
        val rtonMagic = rtonFile.readString(4)
        val rtonVer = rtonFile.readUInt32LE()
        if (rtonMagic != HEADER) {
            throw RTONDecodeException(
                "Wrong RTON header",
                rtonFile.filePath?.pathString ?: "Undefined",
                "Begin with RTON",
                RTONListException.Header
            )
        }
        if (rtonVer != VERSION) {
            throw RTONDecodeException(
                "Wrong RTON version",
                rtonFile.filePath?.pathString ?: "Undefined",
                "Version must be 1",
                RTONListException.Version
            )
        }
        jsonObject = readObject(rtonFile, JsonObject(jsonObject)).toMutableMap()
        val EOF = rtonFile.readString(4)
        if (EOF != EOR) throw RTONDecodeException(
            "End of RTON file wrong",
            rtonFile.filePath?.pathString ?: "Undefined",
            "End of RTON must be DONE",
            RTONListException.Ends
        )
        R0x90List.clear()
        R0x92List.clear()
        return JsonObject(jsonObject)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readByteCode(
        byte: UByte,
        valueType: Boolean,
        rtonFile: SenBuffer,
        jsonObject: JsonObject
    ): JsonElement {
        return when (byte) {
            0x0.toUByte() -> JsonPrimitive(false)
            0x1.toUByte() -> JsonPrimitive(true)
            0x2.toUByte() -> JsonPrimitive(if (valueType) null else "Null")
            0x8.toUByte() -> JsonPrimitive(rtonFile.readInt8())
            0x9.toUByte() -> JsonPrimitive(0)
            0xA.toUByte() -> JsonPrimitive(rtonFile.readUInt8())
            0xB.toUByte() -> JsonPrimitive(0)
            0x10.toUByte() -> JsonPrimitive(rtonFile.readInt16LE())
            0x11.toUByte() -> JsonPrimitive(0)
            0x12.toUByte() -> JsonPrimitive(rtonFile.readUInt16LE())
            0x13.toUByte() -> JsonPrimitive(0)
            0x20.toUByte() -> JsonPrimitive(rtonFile.readInt32LE())
            0x21.toUByte() -> JsonPrimitive(0)
            0x22.toUByte() -> JsonPrimitive(rtonFile.readFloatLE())
            0x23.toUByte() -> JsonPrimitive(0.0F)
            0x24.toUByte() -> JsonPrimitive(rtonFile.readVarInt32())
            0x25.toUByte() -> JsonPrimitive(rtonFile.readZigZag32())
            0x26.toUByte() -> JsonPrimitive(rtonFile.readUInt32LE())
            0x27.toUByte() -> JsonPrimitive(0U)
            0x28.toUByte() -> JsonPrimitive(rtonFile.readVarUInt32())
            0x40.toUByte() -> JsonPrimitive(rtonFile.readInt64LE())
            0x41.toUByte() -> JsonPrimitive(0L)
            0x42.toUByte() -> JsonPrimitive(rtonFile.readDoubleLE())
            0x43.toUByte() -> JsonPrimitive(0.0)
            0x44.toUByte() -> JsonPrimitive(rtonFile.readVarInt64())
            0x45.toUByte() -> JsonPrimitive(rtonFile.readZigZag64())
            0x46.toUByte() -> JsonPrimitive(rtonFile.readUInt64LE())
            0x47.toUByte() -> JsonPrimitive(0UL)
            0x48.toUByte() -> JsonPrimitive(rtonFile.readVarUInt64())
            0x81.toUByte() -> JsonPrimitive(
                if (valueType) rtonFile.readString(rtonFile.readVarInt32()) else rtonFile.readString(
                    rtonFile.readVarInt32()
                )
            )

            0x82.toUByte() -> {
                rtonFile.readVarInt32()
                JsonPrimitive(
                    if (valueType) rtonFile.readString(rtonFile.readVarInt32()) else rtonFile.readString(
                        rtonFile.readVarInt32()
                    )
                )
            }

            0x83.toUByte() -> JsonPrimitive(if (valueType) readRTID(rtonFile) else readRTID(rtonFile))
            0x84.toUByte() -> JsonPrimitive(if (valueType) RTID_0 else RTID_0)
            0x85.toUByte() -> readObject(rtonFile, jsonObject)
            0x86.toUByte() -> readArray(rtonFile, jsonObject)
            0x87.toUByte() -> JsonPrimitive(if (valueType) readBinary(rtonFile) else readBinary(rtonFile))
            0x90.toUByte() -> {
                val num = rtonFile.readVarInt32()
                tempString = rtonFile.readString(num)
                R0x90List.add(tempString)
                JsonPrimitive(if (valueType) tempString else tempString)
            }

            0x91.toUByte() -> JsonPrimitive(if (valueType) R0x90List[rtonFile.readVarInt32()] else R0x90List[rtonFile.readVarInt32()])
            0x92.toUByte() -> {
                rtonFile.readVarInt32()
                tempString = rtonFile.readString(rtonFile.readVarInt32())
                R0x92List.add(tempString)
                JsonPrimitive(if (valueType) tempString else tempString)
            }

            0x93.toUByte() -> JsonPrimitive(if (valueType) R0x92List[rtonFile.readVarInt32()] else R0x92List[rtonFile.readVarInt32()])
            0xB0.toUByte(),
            0xB1.toUByte(),
            0xB2.toUByte(),
            0xB3.toUByte(),
            0xB4.toUByte(),
            0xB5.toUByte(),
            0xB6.toUByte(),
            0xB7.toUByte(),
            0xB8.toUByte(),
            0xB9.toUByte(),
            0xBA.toUByte(),
            0xBB.toUByte() -> throw RTONException("Not a RTON", rtonFile.filePath?.pathString ?: "Undefined")

            0xBC.toUByte() -> JsonPrimitive(rtonFile.readUInt8() != 0.toUByte())
            else -> throw RTONException(
                "Bytecode Error: $byte in offset: ${rtonFile.readOffset}",
                rtonFile.filePath?.pathString ?: "Undefined"
            )
        }
    }

    private fun readBinary(rtonFile: SenBuffer): String {
        rtonFile.readUInt8()
        val s = rtonFile.readStringByVarInt32()
        val i = rtonFile.readVarInt32()
        return String.format(BINARY, s, i)
    }

    private fun readRTID(rtonFile: SenBuffer): String {
        val temp = rtonFile.readUInt8()
        return when (temp) {
            0x00.toUByte() -> RTID_0
            0x01.toUByte() -> {
                val value12 = rtonFile.readVarInt32()
                val value11 = rtonFile.readVarInt32()
                val x161 = rtonFile.readUInt32LE()
                String.format(RTID_2, value11, value12, x161, "")
            }

            0x02.toUByte() -> {
                rtonFile.readVarInt32()
                val str = rtonFile.readStringByVarInt32()
                val value22 = rtonFile.readVarInt32()
                val value21 = rtonFile.readVarInt32()
                val x162 = rtonFile.readUInt32LE()
                String.format(RTID_2, value21, value22, x162, str)
            }

            0x03.toUByte() -> {
                rtonFile.readVarInt32()
                val str2 = rtonFile.readStringByVarInt32()
                rtonFile.readVarInt32()
                val str1 = rtonFile.readStringByVarInt32()
                String.format(RTID_3, str1, str2)
            }

            else -> throw RTONException("Not a RTON", rtonFile.filePath?.pathString ?: "Undefined")
        }
    }

    private fun readObject(rtonFile: SenBuffer, jsonObject: JsonObject): JsonObject {
        val tempObject = mutableMapOf<String, JsonElement>()
        var byteCode = rtonFile.readUInt8()
        while (byteCode != 0xFF.toUByte()) {
            val key = readByteCode(byteCode, false, rtonFile, jsonObject) as JsonPrimitive
            val value = readByteCode(rtonFile.readUInt8(), true, rtonFile, jsonObject)
            byteCode = rtonFile.readUInt8()
            tempObject[key.content] = value
        }
        return JsonObject(tempObject)
    }

    private fun readArray(rtonFile: SenBuffer, jsonObject: JsonObject): JsonArray {
        val tempArray = mutableListOf<JsonElement>()
        var byteCode = rtonFile.readUInt8()
        if (byteCode != 0xFD.toUByte()) throw RTONException("Not a RTON", rtonFile.filePath?.pathString ?: "Undefined")
        val number = rtonFile.readVarInt32()
        for (i in 0 until number) {
            byteCode = rtonFile.readUInt8()
            tempArray.add(readByteCode(byteCode, true, rtonFile, jsonObject))
        }
        byteCode = rtonFile.readUInt8()
        if (byteCode != 0xFE.toUByte()) throw RTONException("Not a RTON", rtonFile.filePath?.pathString ?: "Undefined")
        return JsonArray(tempArray)
    }

    //----------------------- rton 2 json end -----------------------


    //---------------------- json 2 rton start ----------------------

    fun encode(jsonString: String): SenBuffer {
        val r0x90 = StringPool()
        val r0x92 = StringPool()
        val jsonObject = Json.by(JsonFeature.AllowTrailingComma).parseToJsonElement(jsonString) as JsonObject
        val rtonFile = SenBuffer()
        rtonFile.writeString(HEADER)
        rtonFile.writeUInt32LE(VERSION)
        writeObject(rtonFile, jsonObject, r0x90, r0x92)
        rtonFile.writeString(EOR)
        return rtonFile
    }

    fun isASCII(str: String): Boolean {
        for (char in str) {
            if (char.code > 127) return false
        }
        return true
    }

    private fun writeString(rtonFile: SenBuffer, str: String?, r0x90: StringPool, r0x92: StringPool) {
        if (str == NULL) rtonFile.writeUInt8(2.toUByte())
        else if (writeRTID(rtonFile, str)) {
            /* must do nothing */
        } else if (writeBinary(rtonFile, str)) {
            /* must do nothing */
        } else if (isASCII(str)) {
            if (r0x90.exist(str)) {
                rtonFile.writeUInt8(0x91.toUByte())
                rtonFile.writeVarInt32(r0x90[str]!!.index)
            } else {
                rtonFile.writeUInt8(0x90.toUByte())
                rtonFile.writeStringByVarInt32(str)
                r0x90.throwInPool(str)
            }
        } else {
            if (r0x92.exist(str)) {
                rtonFile.writeUInt8(0x93.toUByte())
                rtonFile.writeVarInt32(r0x92[str]!!.index)
            } else {
                rtonFile.writeUInt8(0x92.toUByte())
                rtonFile.writeVarInt32(str.length)
                rtonFile.writeStringByVarInt32(str)
                r0x92.throwInPool(str)
            }
        }
    }

    @Suppress("SameReturnValue")
    private fun writeBinary(rtonFile: SenBuffer, str: String): Boolean {
        if (str.startsWith(BINARY_BEGIN) && str.endsWith(BINARY_END)) {
            val index = str.lastIndexOf(BINARY_MIDDLE)
            if (index == -1) return false
            val v = str.substring(index + 3, str.length - 1).toIntOrNull() ?: return false
            val mString = str.substring(9, index)
            rtonFile.writeUInt8(0x87.toUByte())
            rtonFile.writeUInt8(0.toUByte())
            rtonFile.writeStringByVarInt32(mString)
            rtonFile.writeVarInt32(v)
        }
        return false
    }

    private fun writeRTID(rtonFile: SenBuffer, str: String): Boolean {
        if (str.startsWith(RTID_BEGIN) && str.endsWith(RTID_END)) {
            if (str == RTID_0) {
                rtonFile.writeUInt8(0x84.toUByte())
                return true
            }
            val newStr = str.substring(5, str.length - 1)
            if (newStr.contains("@")) {
                val nameStr = newStr.split("@")
                val dotCount = nameStr[0].count { it == '.' }
                rtonFile.writeUInt8(0x83.toUByte())
                if (dotCount == 2) {
                    val intStr = nameStr[0].split(".")
                    rtonFile.writeUInt8(0x02.toUByte())
                    rtonFile.writeVarInt32(nameStr[1].length)
                    rtonFile.writeStringByVarInt32(nameStr[1])
                    rtonFile.writeVarInt32(intStr[1].toInt())
                    rtonFile.writeVarInt32(intStr[0].toInt())
                    val hexBytes = intStr[2].toByteArray(Charsets.UTF_8)
                    hexBytes.reverse()
                    rtonFile.writeBytes(hexBytes)
                } else {
                    rtonFile.writeUInt8(0x03.toUByte())
                    rtonFile.writeVarInt32(nameStr[1].length)
                    rtonFile.writeStringByVarInt32(nameStr[1])
                    rtonFile.writeVarInt32(nameStr[0].length)
                    rtonFile.writeStringByVarInt32(nameStr[0])
                }
                return true
            }
        }
        return false
    }

    private fun writeNumber(rtonFile: SenBuffer, value: JsonPrimitive) {
        if (value.content.indexOf('.') > -1) {
            val doubleVal = value.double
            if (doubleVal == 0.0) {
                rtonFile.writeUInt8(0x23.toUByte())
            } else {
                val floatVal = value.float
                if (floatVal.toDouble() == doubleVal) {
                    rtonFile.writeUInt8(0x22.toUByte())
                    rtonFile.writeFloatLE(floatVal)
                } else {
                    rtonFile.writeUInt8(0x42.toUByte())
                    rtonFile.writeDoubleLE(doubleVal)
                }
            }
        } else {
            val longVal = value.longOrNull
            if (longVal != null) {
                if (longVal == 0L) {
                    rtonFile.writeUInt8(0x21.toUByte())
                } else if (longVal > 0L) {
                    if (longVal <= Int.MAX_VALUE) {
                        rtonFile.writeUInt8(0x24.toUByte())
                        rtonFile.writeVarInt32(longVal.toInt())
                    } else {
                        rtonFile.writeUInt8(0x44.toUByte())
                        rtonFile.writeVarInt64(longVal)
                    }
                } else {
                    if (longVal + 0x40000000 >= 0) {
                        rtonFile.writeUInt8(0x25.toUByte())
                        rtonFile.writeZigZag32(longVal.toInt())
                    } else {
                        rtonFile.writeUInt8(0x45.toUByte())
                        rtonFile.writeZigZag64(longVal)
                    }
                }
            } else {
                val v = value.content.toULong()
                rtonFile.writeUInt8(0x46.toUByte())
                rtonFile.writeUInt64LE(v)
            }
        }
    }

    private fun writeValueJson(rtonFile: SenBuffer, value: JsonElement, r0x90: StringPool, r0x92: StringPool) {
        fun String.isNumber(): Boolean {
            return try {
                this.toDouble()
                true
            } catch (_: Exception) {
                false
            }
        }
        when (value) {
            is JsonObject -> {
                rtonFile.writeUInt8(0x85.toUByte())
                writeObject(rtonFile, value, r0x90, r0x92)
            }

            is JsonArray -> {
                rtonFile.writeUInt8(0x86.toUByte())
                writeArray(rtonFile, value, r0x90, r0x92)
            }

            is JsonPrimitive -> {
                when {
                    value is JsonNull -> rtonFile.writeUInt8(0x84.toUByte())
                    value.booleanOrNull != null -> rtonFile.writeBool(value.boolean)
                    value.content.isNumber() -> writeNumber(rtonFile, value)
                    value.isString -> writeString(rtonFile, value.content, r0x90, r0x92)

                    else -> throw RTONException(
                        "Unexpected JsonPrimitive: $value",
                        rtonFile.filePath?.pathString ?: "Undefined"
                    )
                }
            }

            else -> throw RTONException("Not a RTON", rtonFile.filePath?.pathString ?: "Undefined")
        }
    }

    private fun writeObject(rtonFile: SenBuffer, jsonObject: JsonObject, r0x90: StringPool, r0x92: StringPool) {
        jsonObject.entries.forEach {
            writeString(rtonFile, it.key, r0x90, r0x92)
            writeValueJson(rtonFile, it.value, r0x90, r0x92)
        }
        rtonFile.writeUInt8(0xFF.toUByte())
    }

    private fun writeArray(rtonFile: SenBuffer, jsonArray: JsonArray, r0x90: StringPool, r0x92: StringPool) {
        rtonFile.writeUInt8(0xFD.toUByte())
        val arrayLength = jsonArray.size
        rtonFile.writeVarInt32(arrayLength)
        for (i in 0 until arrayLength) writeValueJson(rtonFile, jsonArray[i], r0x90, r0x92)
        rtonFile.writeUInt8(0xFE.toUByte())
    }
}