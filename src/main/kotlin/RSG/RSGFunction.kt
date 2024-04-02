package RSG

import RSB.PTXProperty
import RSB.RSBPacketInfo
import RSB.RSBResInfo
import RTON.RTONProcession
import standard.*
import java.nio.file.Paths

object RSGFunction {
    private var part0List = mutableListOf<Part0List>()

    private var part1List = mutableListOf<Part1List>()

    data class Part0List(
        var path: String,
        var offset: Int,
        var size: Int
    )

    data class Part1List(
        var path: String,
        var offset: Int,
        var size: Int,
        var id: Int,
        var width: Int,
        var height: Int
    )

    private val ZlibLevelCompression = arrayOf(
        intArrayOf(120, 1),
        intArrayOf(120, 94),
        intArrayOf(120, 156),
        intArrayOf(120, 218)
    )

    fun unpack(
        rsgFile: SenBuffer,
        outFolder: String,
        useResFolder: Boolean = true,
        getPacketInfo: Boolean = false
    ): PacketInfo {
        val headInfo = readRSGHead(rsgFile)
        part0List.clear()
        part1List.clear()
        fileListSplit(rsgFile, headInfo)
        var part0RawData = SenBuffer()
        var part1RawData = SenBuffer()
        val resInfo = mutableListOf<ResInfo>()
        val part0Length = part0List.size
        var fileData: SenBuffer?
        if (part0Length > 0) {
            if (!getPacketInfo) part0RawData = SenBuffer(checkZlib(rsgFile, headInfo, false))
            for (i in 0 until part0Length) {
                if (!getPacketInfo) {
                    fileData = SenBuffer(part0RawData.getBytes(part0List[i].size, part0List[i].offset.toLong()))
                    fileData.outFile(
                        Paths.get(
                            if (useResFolder) "$outFolder${s}res${s}${part0List[i].path}" else "$outFolder${s}${part0List[i].path}"
                        )
                    )
                }
                resInfo.add(ResInfo(path = part0List[i].path))
            }
            part0RawData.close()
        }
        val part1Length = part1List.size
        if (part1Length > 0) {
            if (!getPacketInfo) part1RawData = SenBuffer(checkZlib(rsgFile, headInfo, true))
            for (i in 0 until part1Length) {
                if (!getPacketInfo) {
                    fileData = SenBuffer(part1RawData.getBytes(part1List[i].size, part1List[i].offset.toLong()))
                    fileData.outFile(
                        Paths.get(
                            if (useResFolder) "$outFolder${s}res${s}${part1List[i].path}" else "$outFolder${s}${part1List[i].path}"
                        )
                    )
                }
                resInfo.add(
                    ResInfo(
                        path = part1List[i].path,
                        ptxInfo = PTXInfo(
                            id = part1List[i].id,
                            width = part1List[i].width,
                            height = part1List[i].height
                        )
                    )
                )
            }
            part1RawData.close()
        }
        val packetInfo = PacketInfo(
            version = headInfo.version,
            compressionFlags = headInfo.flags,
            res = resInfo.toMutableList()
        )
        if (!getPacketInfo) {
            rsgFile.close()
        }
        return packetInfo
    }

    private fun readRSGHead(rsgFile: SenBuffer): RSGHead {
        val headInfo = RSGHead()
        val header = rsgFile.readString(4)
        if (header != "pgsr") {
            throw Exception("Mismatch RSG header")
        }
        val version = rsgFile.readInt32LE()
        if (version != 3 && version != 4) {
            throw Exception("Unsupported RSG version")
        }
        headInfo.version = version
        rsgFile.readBytes(8)
        val flags = rsgFile.readInt32LE()
        if (flags > 3 || flags < 0) {
            throw Exception("Invalid RSG compression flag")
        }
        headInfo.flags = flags
        headInfo.fileOffset = rsgFile.readInt32LE()
        headInfo.part0Offset = rsgFile.readInt32LE()
        headInfo.part0Zlib = rsgFile.readInt32LE()
        headInfo.part0Size = rsgFile.readInt32LE()
        rsgFile.readInt32LE()
        headInfo.part1Offset = rsgFile.readInt32LE()
        headInfo.part1Zlib = rsgFile.readInt32LE()
        headInfo.part1Size = rsgFile.readInt32LE()
        rsgFile.readBytes(20)
        headInfo.fileListLength = rsgFile.readInt32LE()
        headInfo.fileListOffset = rsgFile.readInt32LE()
        return headInfo
    }

    private fun checkZlib(rsgFile: SenBuffer, headInfo: RSGHead, atlasInfo: Boolean): ByteArray {
        fun zlibHeaderCheck(rsgData: ByteArray): Boolean {
            for (i in ZlibLevelCompression.indices) {
                if (rsgData[0].toInt() == ZlibLevelCompression[i][0] && rsgData[1].toInt() == ZlibLevelCompression[i][1]) {
                    return false
                }
            }
            return true
        }

        return if (atlasInfo) {
            if (headInfo.flags == 0 || headInfo.flags == 2 || zlibHeaderCheck(
                    rsgFile.getBytes(
                        2,
                        headInfo.part1Offset.toLong()
                    )
                )
            ) {
                rsgFile.getBytes(headInfo.part1Size, headInfo.part1Offset.toLong())
            } else {
                uncompressZlib(rsgFile.getBytes(headInfo.part1Zlib, headInfo.part1Offset.toLong()))
            }
        } else {
            if (headInfo.flags < 2 || zlibHeaderCheck(rsgFile.getBytes(2, headInfo.part0Offset.toLong()))) {
                rsgFile.getBytes(headInfo.part0Size, headInfo.part0Offset.toLong())
            } else {
                uncompressZlib(rsgFile.getBytes(headInfo.part0Zlib, headInfo.part0Offset.toLong()))
            }
        }
    }

    private fun fileListSplit(rsgFile: SenBuffer, headInfo: RSGHead) {
        val nameDict = mutableListOf<NameDict>()
        var namePath = ""
        val tempOffset = headInfo.fileListOffset
        rsgFile.readOffset = tempOffset.toLong()
        val offsetLimit = tempOffset + headInfo.fileListLength
        while (rsgFile.readOffset < offsetLimit) {
            val characterByte = rsgFile.readString(1)
            val offsetByte = rsgFile.readInt24LE() * 4
            if (characterByte == "\u0000") {
                if (offsetByte != 0) {
                    nameDict.add(NameDict(namePath = namePath, offsetByte = offsetByte))
                }
                val typeByte = rsgFile.readInt32LE() == 1
                if (typeByte) {
                    part1List.add(
                        Part1List(
                            path = namePath,
                            offset = rsgFile.readInt32LE(),
                            size = rsgFile.readInt32LE(),
                            id = rsgFile.readInt32LE(),
                            width = rsgFile.readInt32LE(rsgFile.readOffset + 8),
                            height = rsgFile.readInt32LE()
                        )
                    )
                } else {
                    part0List.add(
                        Part0List(
                            path = namePath,
                            offset = rsgFile.readInt32LE(),
                            size = rsgFile.readInt32LE()
                        )
                    )
                }
                for (i in nameDict.indices) {
                    if ((nameDict[i].offsetByte + tempOffset).toLong() == rsgFile.readOffset) {
                        namePath = nameDict[i].namePath
                        nameDict.removeAt(i)
                        break
                    }
                }
            } else {
                if (offsetByte != 0) {
                    nameDict.add(NameDict(namePath = namePath, offsetByte = offsetByte))
                    namePath += characterByte
                } else {
                    namePath += characterByte
                }
            }
        }
    }

    // Pack RSG
    fun pack(inFolder: String, packetInfo: PacketInfo, useResFolder: Boolean = true): SenBuffer {
        if (packetInfo.version != 3 && packetInfo.version != 4) {
            throw Exception("Unsupported RSG version")
        }
        if (packetInfo.compressionFlags < 0 || packetInfo.compressionFlags > 3) {
            throw Exception("Unsupported RSG pack compression flag")
        }
        val rsgFile = SenBuffer()
        rsgFile.writeString(RSGHead().header)
        rsgFile.writeInt32LE(packetInfo.version)
        rsgFile.writeNull(8)
        rsgFile.writeInt32LE(packetInfo.compressionFlags)
        rsgFile.writeNull(72)
        val pathTemps = fileListPack(packetInfo.res)
        writeRSG(rsgFile, pathTemps, packetInfo.compressionFlags, inFolder, useResFolder)
        return rsgFile
    }

    fun isNotASCII(str: String): Boolean {
        for (char in str) {
            if (char.code > 127) {
                return true
            }
        }
        return false
    }

    private fun fileListPack(resInfo: MutableList<ResInfo>): List<PathTemp> {
        resInfo.add(0, ResInfo(path = ""))
        resInfo.sortBy { it.path.uppercase() }
        val listLength = resInfo.size - 1
        val pathTemps = mutableListOf<PathTemp>()
        var wPosition = 0
        for (i in 0 until listLength) {
            val path1 = resInfo[i].path.uppercase()
            val path2 = resInfo[i + 1].path.uppercase()
            if (isNotASCII(path2)) {
                throw Exception("item_part_must_be_ascii")
            }
            val strLongestLength = maxOf(path1.length, path2.length)
            for (k in 0 until strLongestLength) {
                if (k >= path1.length || k >= path2.length || path1[k] != path2[k]) {
                    for (h in pathTemps.size downTo 1) {
                        if (k >= pathTemps[h - 1].key) {
                            pathTemps[h - 1].positions.add(
                                PathPosition(
                                    position = wPosition,
                                    offset = k - pathTemps[h - 1].key
                                )
                            )
                            break
                        }
                    }
                    wPosition += if (path2.endsWith(".PTX")) path2.length - k + 9 else path2.length - k + 4
                    pathTemps.add(
                        PathTemp(
                            pathSlice = path2.substring(k),
                            key = k,
                            resInfo = resInfo[i + 1],
                            isAtlas = path2.endsWith(".PTX")
                        )
                    )
                    break
                }
            }
        }
        return pathTemps
    }

    private fun writeRSG(
        rsgFile: SenBuffer,
        pathTemps: List<PathTemp>,
        compressionFlags: Int,
        inFolder: String,
        useResFolder: Boolean = true
    ) {
        val pathTempLength = pathTemps.size
        val fileListBeginOffset = rsgFile.writeOffset
        if (fileListBeginOffset != 92.toLong()) {
            throw Exception("Invalid file list offset")
        }
        val dataGroup = SenBuffer()
        val atlasGroup = SenBuffer()
        var dataPos = 0
        var atlasPos = 0
        for (i in 0 until pathTempLength) {
            val beginOffset = rsgFile.writeOffset
            val packetResInfo = pathTemps[i].resInfo
            rsgFile.writeStringFourUByte(pathTemps[i].pathSlice)
            rsgFile.backupWriteOffset()
            for (h in pathTemps[i].positions.indices) {
                rsgFile.writeInt24LE(
                    pathTemps[i].positions[h].position,
                    beginOffset + pathTemps[i].positions[h].offset * 4 + 1
                )
            }

            val senFile = SenBuffer(
                Paths.get(
                    if (useResFolder) "$inFolder${s}res${s}${packetResInfo.path}" else "$inFolder${s}${packetResInfo.path}"
                )
            )
            val dataItem = senFile.toBytes()
            senFile.close()
            val appendBytes = beautifyLengthForFile(dataItem.size)
            if (pathTemps[i].isAtlas) {
                atlasGroup.writeBytes(dataItem)
                atlasGroup.writeNull(appendBytes)
                rsgFile.restoreWriteOffset()
                rsgFile.writeInt32LE(1)
                rsgFile.writeInt32LE(atlasPos)
                rsgFile.writeInt32LE(dataItem.size)
                rsgFile.writeInt32LE(packetResInfo.ptxInfo!!.id)
                rsgFile.writeNull(8)
                rsgFile.writeInt32LE(packetResInfo.ptxInfo!!.width)
                rsgFile.writeInt32LE(packetResInfo.ptxInfo!!.height)
                atlasPos += (dataItem.size + appendBytes)
            } else {
                dataGroup.writeBytes(dataItem)
                dataGroup.writeNull(appendBytes)
                rsgFile.restoreWriteOffset()
                rsgFile.writeInt32LE(0)
                rsgFile.writeInt32LE(dataPos)
                rsgFile.writeInt32LE(dataItem.size)
                dataPos += (dataItem.size + appendBytes)
            }
        }
        val fileListLength = rsgFile.writeOffset - fileListBeginOffset
        rsgFile.writeNull(beautifyLength(rsgFile.writeOffset.toInt()))
        rsgFile.backupWriteOffset()
        rsgFile.writeInt32LE(rsgFile.writeOffset.toInt(), 0x14)
        rsgFile.writeInt32LE(fileListLength.toInt(), 0x48)
        rsgFile.writeInt32LE(fileListBeginOffset.toInt())
        rsgFile.restoreWriteOffset()
        compressor(rsgFile, dataGroup, atlasGroup, compressionFlags)
    }

    private fun compressor(rsgFile: SenBuffer, dataGroup: SenBuffer, atlasGroup: SenBuffer, compressionFlags: Int) {
        fun dataWrite(dataBytes: ByteArray, flags: Int, isAtlas: Boolean) {
            val part0Offset = rsgFile.writeOffset
            val part0Size = dataBytes.size
            if (flags < 2) {
                rsgFile.writeBytes(dataBytes)
                rsgFile.backupWriteOffset()
                rsgFile.writeInt32LE(part0Offset.toInt(), 0x18)
                rsgFile.writeInt32LE(part0Size)
                if (isAtlas) rsgFile.writeInt32LE(0)
                else rsgFile.writeInt32LE(part0Size)
                rsgFile.restoreWriteOffset()
            } else {
                val zlibBytes = compressZlib(
                    dataBytes,
                    if (flags == 3) ZlibCompressionLevel.BEST_COMPRESSION else ZlibCompressionLevel.DEFAULT_COMPRESSION
                )
                val zlibAppendLength = beautifyLength(zlibBytes.size)
                rsgFile.writeBytes(zlibBytes)
                rsgFile.writeNull(zlibAppendLength)
                val part0Zlib = zlibBytes.size + zlibAppendLength
                rsgFile.backupWriteOffset()
                rsgFile.writeInt32LE(part0Offset.toInt(), 0x18)
                rsgFile.writeInt32LE(part0Zlib)
                rsgFile.writeInt32LE(part0Size)
                rsgFile.restoreWriteOffset()
            }
        }
        if (dataGroup.length != 0L) {
            val dataBytes = dataGroup.toBytes()
            dataGroup.close()
            dataWrite(dataBytes, compressionFlags, false)
        }
        if (atlasGroup.length != 0L) {
            val atlasBytes = atlasGroup.toBytes()
            atlasGroup.close()
            val part1Offset: Int
            val part1Size = atlasBytes.size
            val dataEmpty = SenBuffer()
            dataEmpty.writeInt32LE(252536)
            dataEmpty.writeInt32BE(1)
            dataEmpty.writeNull(4088)
            if (compressionFlags == 0 || compressionFlags == 2) {
                if (compressionFlags == 2 && dataGroup.length == 0L) {
                    dataWrite(dataEmpty.toBytes(), 1, true)
                } else {
                    dataWrite(byteArrayOf(), 1, true)
                }
                part1Offset = rsgFile.writeOffset.toInt()
                rsgFile.writeBytes(atlasBytes)
                rsgFile.backupWriteOffset()
                rsgFile.writeInt32LE(part1Offset, 0x28)
                rsgFile.writeInt32LE(part1Size)
                rsgFile.writeInt32LE(part1Size)
                rsgFile.restoreWriteOffset()
            } else {
                if (compressionFlags == 3 && dataGroup.length == 0L) {
                    dataWrite(dataEmpty.toBytes(), 1, true)
                } else {
                    dataWrite(byteArrayOf(), 1, true)
                }
                part1Offset = rsgFile.writeOffset.toInt()
                val zlibBytes = compressZlib(
                    atlasBytes,
                    if (compressionFlags == 3) ZlibCompressionLevel.BEST_COMPRESSION else ZlibCompressionLevel.DEFAULT_COMPRESSION
                )
                val zlibAppendLength = beautifyLength(zlibBytes.size)
                rsgFile.writeBytes(zlibBytes)
                rsgFile.writeNull(zlibAppendLength)
                val part1Zlib = zlibBytes.size + zlibAppendLength
                rsgFile.backupWriteOffset()
                rsgFile.writeInt32LE(part1Offset, 0x28)
                rsgFile.writeInt32LE(part1Zlib)
                rsgFile.writeInt32LE(part1Size)
                rsgFile.restoreWriteOffset()
            }
            dataEmpty.close()
        } else {
            rsgFile.writeInt32LE(rsgFile.length.toInt(), 0x28)
        }
    }

    fun beautifyLength(oriLength: Int): Int {
        return if (oriLength % 4096 == 0) {
            4096
        } else {
            4096 - (oriLength % 4096)
        }
    }

    private fun beautifyLengthForFile(oriLength: Int): Int {
        return if (oriLength % 4096 == 0) {
            0
        } else {
            4096 - (oriLength % 4096)
        }
    }

    fun getRSBPacketInfo(rsgFile: SenBuffer): RSBPacketInfo {
        val packetInfo = unpack(rsgFile, "", useResFolder = false, getPacketInfo = true)
        val resInfo = mutableListOf<RSBResInfo>()
        for (i in packetInfo.res.indices) {
            val rsgRsgInfo = RSBResInfo(
                path = packetInfo.res[i].path,
            )
            if (packetInfo.res[i].ptxInfo != null) {
                for (k in part1List.indices) {
                    if (packetInfo.res[i].path == part1List[k].path) {
                        rsgRsgInfo.ptxInfo = packetInfo.res[i].ptxInfo
                        rsgRsgInfo.ptxProperty = PTXProperty(
                            format = getFormat(
                                part1List[k].size,
                                packetInfo.res[i].ptxInfo!!.width * packetInfo.res[i].ptxInfo!!.height
                            ),
                            pitch = packetInfo.res[i].ptxInfo!!.width * 4,
                        )
                    }
                }
            }
            resInfo.add(rsgRsgInfo)
        }
        return RSBPacketInfo(
            version = packetInfo.version,
            compressionFlags = packetInfo.compressionFlags,
            res = resInfo.toMutableList(),
        )
    }

    private fun getFormat(fileLength: Int, square: Int): Int {
        val d = (fileLength / square) + 0.4
        val ratio = kotlin.math.ceil(d)
        return when (ratio) {
            5.0 -> 0
            2.0 -> 147
            1.0 -> 30
            else -> throw Exception("Invalid PTX format")
        }
    }

    enum class RSGAbnormal {
        Header,
        NotASCIISmartPath,
        None,
    }

    fun isPopCapRSG(rsgFile: SenBuffer, closeRSG: Boolean = true): RSGAbnormal {
        val headInfo = readRSGHead(rsgFile)
        if (headInfo.fileListOffset != 0x5C) return RSGAbnormal.Header
        part0List.clear()
        part1List.clear()
        fileListSplit(rsgFile, headInfo)
        if (closeRSG) {
            rsgFile.close()
        }
        if (part0List.isNotEmpty()) {
            for (i in part0List.indices) {
                if (!RTONProcession.isASCII(part0List[i].path)) return RSGAbnormal.NotASCIISmartPath
            }
        }
        if (part1List.isNotEmpty()) {
            for (i in part1List.indices) {
                if (!RTONProcession.isASCII(part1List[i].path)) return RSGAbnormal.NotASCIISmartPath
            }
        }
        return RSGAbnormal.None
    }
}