package RSB

import RSG.NameDict
import RSG.PathPosition
import RSG.RSGFunction
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import standard.SenBuffer
import standard.s
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@OptIn(ExperimentalUnsignedTypes::class)
object RSBFunction {
    fun unpack(rsbFile: SenBuffer, outFolder: String): ManifestInfo {
        val rsbHeadInfo = readHead(rsbFile)
        if (rsbHeadInfo.version != 3 && rsbHeadInfo.version != 4) {
            throw Exception("Invalid RSB version")
        }
        if (rsbHeadInfo.version == 3 && rsbHeadInfo.fileListBeginOffset != 0x6C) {
            throw Exception("Invalid file list offset")
        }
        if (rsbHeadInfo.version == 4 && rsbHeadInfo.fileListBeginOffset != 0x70) {
            throw Exception("Invalid file list offset")
        }
        val fileList = mutableListOf<FileListInfo>()
        fileListSplit(rsbFile, rsbHeadInfo.fileListBeginOffset, rsbHeadInfo.fileListLength, fileList)
        val rsgList = mutableListOf<FileListInfo>()
        fileListSplit(rsbFile, rsbHeadInfo.rsgListBeginOffset, rsbHeadInfo.rsgListLength, rsgList)
        val compositeInfo = mutableListOf<CompositeInfo>()
        readCompositeInfo(rsbFile, rsbHeadInfo, compositeInfo)
        val compositeList = mutableListOf<FileListInfo>()
        fileListSplit(rsbFile, rsbHeadInfo.compositeListBeginOffset, rsbHeadInfo.compositeListLength, compositeList)
        val rsgInfoList = mutableListOf<RSGInfo>()
        readRSGInfo(rsbFile, rsbHeadInfo, rsgInfoList)
        val autoPoolInfoList = mutableListOf<AutoPoolInfo>()
        readAutoPool(rsbFile, rsbHeadInfo, autoPoolInfoList)
        val ptxInfoList = mutableListOf<RSBPTXInfo>()
        readPTXInfo(rsbFile, rsbHeadInfo, ptxInfoList)
        if (rsbHeadInfo.version == 3) {
            if (rsbHeadInfo.part1BeginOffset == 0 && rsbHeadInfo.part2BeginOffset == 0 && rsbHeadInfo.part3BeginOffset == 0) {
                throw Exception("Invalid RSB ver 3 resource offset")
            }
            readResourcesDescription(rsbFile, rsbHeadInfo, outFolder)
        }
        // Unpack RSG
        val groupList = mutableListOf<GroupInfo>()
        val compositeLength = compositeInfo.size
        val rsgNameList = mutableListOf<String>()
        for (i in 0 until compositeLength) {
            if (compositeInfo[i].name.uppercase(Locale.getDefault()) != compositeList[i].namePath.uppercase(Locale.getDefault())
                    .replace("_COMPOSITESHELL", "")
            ) {
                throw Exception("Invalid composite name: ${compositeInfo[i].name}")
            }
            val subGroupList = mutableListOf<SubGroupInfo>()
            for (k in 0 until compositeInfo[i].packetNumber) {
                val packetIndex = compositeInfo[i].packetInfo!![k].packetIndex
                var rsgInfoCount = 0
                var rsgListCount = 0
                while (rsgInfoList[rsgInfoCount].poolIndex != packetIndex) {
                    if (rsgInfoCount >= rsgInfoList.size - 1) {
                        throw Exception("Out of range")
                    }
                    rsgInfoCount++
                }
                while (rsgList[rsgListCount].poolIndex != packetIndex) {
                    if (rsgListCount >= rsgList.size - 1) {
                        throw Exception("Out of range")
                    }
                    rsgListCount++
                }
                if (rsgInfoList[rsgInfoCount].name.uppercase(Locale.getDefault()) != rsgList[rsgListCount].namePath.uppercase(
                        Locale.getDefault()
                    )
                ) {
                    throw Exception("Invalid RSG name: ${rsgInfoList[rsgInfoCount].name}  ${rsgList[rsgListCount].namePath} in pool index: $packetIndex")
                }
                rsgNameList.add(rsgInfoList[rsgInfoCount].name)
                val packetFile =
                    rsbFile.getUBytes(rsgInfoList[rsgInfoCount].rsgLength, rsgInfoList[rsgInfoCount].rsgOffset)
                val RSGFile = SenBuffer(packetFile)
                val packetInfo = RSGFunction.unpack(RSGFile, "", useResFolder = false, getPacketInfo = true)
                val resInfoList = mutableListOf<RSBResInfo>()
                val fileListLength = fileList.size
                val ptxBeforeNumber = rsgInfoList[rsgInfoCount].ptxBeforeNumber
                for (h in 0 until fileListLength) {
                    if (fileList[h].poolIndex == packetIndex) {
                        val resInfo = RSBResInfo(
                            path = fileList[h].namePath
                        )
                        val resInfoLength = packetInfo.res.size
                        var existItemPacket = false
                        for (m in 0 until resInfoLength) {
                            if (packetInfo.res[m].path.equals(fileList[h].namePath, ignoreCase = true)) {
                                existItemPacket = true
                                if (fileList[h].namePath.endsWith(".PTX") && compositeInfo[i].isComposite) {
                                    if (ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].width != packetInfo.res[m].ptxInfo!!.width) {
                                        throw Exception("Invalid packet width: ${fileList[h].namePath}, In Manifest: ${ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].width}, In RSG: ${packetInfo.res[m].ptxInfo!!.width}")
                                    }
                                    if (ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].height != packetInfo.res[m].ptxInfo!!.height) {
                                        throw Exception("Invalid packet height: ${fileList[h].namePath}, In Manifest: ${ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].height}, In RSG: ${packetInfo.res[m].ptxInfo!!.height}")
                                    }
                                    resInfo.ptxInfo = RSG.PTXInfo(
                                        id = packetInfo.res[m].ptxInfo!!.id,
                                        width = ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].width,
                                        height = ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].height
                                    )
                                    resInfo.ptxProperty = PTXProperty(
                                        format = ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].format,
                                        pitch = ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].pitch,
                                        alphaSize = ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].alphaSize,
                                        alphaFormat = ptxInfoList[ptxBeforeNumber + packetInfo.res[m].ptxInfo!!.id].alphaFormat
                                    )
                                }
                                break
                            }
                        }
                        if (!existItemPacket) {
                            throw Exception("Invalid item packet: ${rsgList[rsgListCount].namePath}")
                        }
                        resInfoList.add(resInfo)
                    }
                    if (fileList[h].poolIndex > packetIndex) break
                }
                RSGFile.outFile(Paths.get("$outFolder${s}packet${s}${rsgInfoList[rsgInfoCount].name}.rsg"))
                val packetInfoList = RSBPacketInfo(
                    version = rsbFile.readInt32LE(rsgInfoList[rsgInfoCount].rsgOffset + 4),
                    compressionFlags = rsbFile.readInt32LE(rsgInfoList[rsgInfoCount].rsgOffset + 16),
                    res = resInfoList.toMutableList()
                )
                subGroupList.add(
                    SubGroupInfo(
                        namePacket = rsgInfoList[rsgInfoCount].name,
                        category = mutableListOf(
                            compositeInfo[i].packetInfo!![k].category[0],
                            compositeInfo[i].packetInfo!![k].category[1]
                        ),
                        packetInfo = packetInfoList
                    )
                )
            }
            groupList.add(
                GroupInfo(
                    name = compositeInfo[i].name,
                    isComposite = compositeInfo[i].isComposite,
                    subGroup = subGroupList.toMutableList()
                )
            )
        }
        val manifestInfo = ManifestInfo(
            version = rsbHeadInfo.version,
            ptxInfoSize = rsbHeadInfo.ptxInfoEachLength,
            path = RSBPathInfo(
                rsgs = rsgNameList.toMutableList(),
                packetPath = "$outFolder${s}packet"
            ),
            group = groupList.toMutableList()
        )
        rsbFile.close()
        return manifestInfo
    }

    private fun readResourcesDescription(RSBFile: SenBuffer, rsbHeadInfo: RSBHead, outFolder: String) {
        RSBFile.readOffset = rsbHeadInfo.part1BeginOffset
        val part2Offset = rsbHeadInfo.part2BeginOffset
        val part3Offset = rsbHeadInfo.part3BeginOffset
        val compositeResourcesInfo = mutableListOf<CompositeResourcesDescriptionInfo>()
        val DescriptionGroup = mutableMapOf<String, DescriptionGroup>()
        var i = 0
        while (RSBFile.readOffset < part2Offset) {
            val idOffsetPart3 = RSBFile.readInt32LE()
            val id = RSBFile.getStringByEmpty((part3Offset + idOffsetPart3))
            val rsgNumber = RSBFile.readInt32LE()
            val subgroup = mutableMapOf<String, DescriptionSubGroup>()
            if (RSBFile.readInt32LE() != 0x10) throw Exception("Invalid RSG number | Offset: ${RSBFile.readOffset}")
            val rsgInfoList = mutableListOf<ResourcesRSGInfo>()
            for (k in 0 until rsgNumber) {
                val resolutionRatio = RSBFile.readInt32LE()
                val language = RSBFile.readString(4).replace("\u0000", "")
                val rsgIdOffsetPart3 = RSBFile.readInt32LE()
                val resourcesNumber = RSBFile.readInt32LE()
                val resourcesInfoList = mutableListOf<ResourcesInfo>()
                for (l in 0 until resourcesNumber) {
                    val infoOffsetPart2 = RSBFile.readInt32LE()
                    resourcesInfoList.add(ResourcesInfo(infoOffsetPart2 = infoOffsetPart2))
                }
                val rsgId = RSBFile.getStringByEmpty(part3Offset + rsgIdOffsetPart3)
                subgroup[rsgId] =
                    DescriptionSubGroup(res = "$resolutionRatio", language = language, resources = mutableMapOf())
                rsgInfoList.add(
                    ResourcesRSGInfo(
                        resolutionRatio = resolutionRatio,
                        language = language,
                        id = rsgId,
                        resourcesNumber = resourcesNumber,
                        resourcesInfoList = resourcesInfoList.toMutableList()
                    )
                )
            }
            DescriptionGroup[id] = DescriptionGroup(composite = !id.endsWith("_CompositeShell"), subgroups = subgroup)
            compositeResourcesInfo.add(
                CompositeResourcesDescriptionInfo(
                    id = id,
                    rsgNumber = rsgNumber,
                    rsgInfoList = rsgInfoList.toMutableList()
                )
            )
            RSBFile.backupReadOffset()
            val resourcesRsgNumber = compositeResourcesInfo[i].rsgNumber
            for (k in 0 until resourcesRsgNumber) {
                val resourcesNumber = compositeResourcesInfo[i].rsgInfoList[k].resourcesNumber
                for (h in 0 until resourcesNumber) {
                    RSBFile.readOffset =
                        part2Offset + compositeResourcesInfo[i].rsgInfoList[k].resourcesInfoList[h].infoOffsetPart2
                    if (RSBFile.readInt32LE() != 0x0) throw Exception("Invalid Part2 Offset: ${RSBFile.readOffset}")
                    val type = RSBFile.readUInt16LE()
                    if (RSBFile.readUInt16LE() != 0x1C.toUShort()) throw Exception("Invalid head length : ${RSBFile.readOffset}")
                    val ptxInfoEndOffsetPart2 = RSBFile.readInt32LE()
                    val ptxInfoBeginOffsetPart2 = RSBFile.readInt32LE()
                    val resIdOffsetPart3 = RSBFile.readInt32LE()
                    val pathOffsetPart3 = RSBFile.readInt32LE()
                    val resId = RSBFile.getStringByEmpty(part3Offset + resIdOffsetPart3)
                    val resPath = RSBFile.getStringByEmpty(part3Offset + pathOffsetPart3)
                    val propertiesNumber = RSBFile.readInt32LE()
                    var ptxInfoList: PropertiesPTXInfo? = null
                    if (ptxInfoEndOffsetPart2 * ptxInfoBeginOffsetPart2 != 0) {
                        ptxInfoList = PropertiesPTXInfo(
                            imagetype = "${RSBFile.readUInt16LE()}",
                            aflags = "${RSBFile.readUInt16LE()}",
                            x = "${RSBFile.readUInt16LE()}",
                            y = "${RSBFile.readUInt16LE()}",
                            ax = "${RSBFile.readUInt16LE()}",
                            ay = "${RSBFile.readUInt16LE()}",
                            aw = "${RSBFile.readUInt16LE()}",
                            ah = "${RSBFile.readUInt16LE()}",
                            rows = "${RSBFile.readUInt16LE()}",
                            cols = "${RSBFile.readUInt16LE()}",
                            parent = RSBFile.getStringByEmpty(part3Offset + RSBFile.readInt32LE())
                        )
                    }
                    val propertiesInfoList = mutableMapOf<String, String>()
                    for (l in 0 until propertiesNumber) {
                        val keyOffsetPart3 = RSBFile.readInt32LE()
                        if (RSBFile.readInt32LE() != 0x0) {
                            throw Exception("RSB is corrupted")
                        }
                        val valueOffsetPart3 = RSBFile.readInt32LE()
                        val key = RSBFile.getStringByEmpty(part3Offset + keyOffsetPart3)
                        val value = RSBFile.getStringByEmpty(part3Offset + valueOffsetPart3)
                        propertiesInfoList[key] = value
                    }
                    val descriptionResources = DescriptionResources(
                        path = resPath,
                        type = type.toInt(),
                        ptxInfo = ptxInfoList,
                        properties = propertiesInfoList
                    )
                    DescriptionGroup.values.elementAt(i).subgroups.values.elementAt(k).resources[resId] =
                        descriptionResources
                }
            }
            RSBFile.restoreReadOffset()
            i++
        }
        val outFolderDir = File(outFolder)
        val writer = FileWriter("$outFolder${s}description.json")
        val resourcesDescription = ResourcesDescription(groups = DescriptionGroup)
        val json = JSON.toJSONString(
            resourcesDescription,
            JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.PrettyFormat
        )
        if (!outFolderDir.exists()) outFolderDir.mkdirs()
        writer.write(json)
        writer.flush()
        writer.close()
        return
    }

    private fun readHead(rsbFile: SenBuffer): RSBHead {
        val rsbHeadInfo = RSBHead()
        val magic = rsbFile.readString(4)
        if (magic != "1bsr") throw Exception("This file is not RSB")
        val version = rsbFile.readInt32LE()
        rsbHeadInfo.version = version
        rsbFile.readUBytes(4)
        rsbHeadInfo.fileOffset = rsbFile.readInt32LE()
        rsbHeadInfo.fileListLength = rsbFile.readInt32LE()
        rsbHeadInfo.fileListBeginOffset = rsbFile.readInt32LE()
        rsbFile.readUBytes(8)
        rsbHeadInfo.rsgListLength = rsbFile.readInt32LE()
        rsbHeadInfo.rsgListBeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.rsgNumber = rsbFile.readInt32LE()
        rsbHeadInfo.rsgInfoBeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.rsgInfoEachLength = rsbFile.readInt32LE()
        rsbHeadInfo.compositeNumber = rsbFile.readInt32LE()
        rsbHeadInfo.compositeInfoBeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.compositeInfoEachLength = rsbFile.readInt32LE()
        rsbHeadInfo.compositeListLength = rsbFile.readInt32LE()
        rsbHeadInfo.compositeListBeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.autoPoolNumber = rsbFile.readInt32LE()
        rsbHeadInfo.autoPoolInfoBeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.autoPoolInfoEachLength = rsbFile.readInt32LE()
        rsbHeadInfo.ptxNumber = rsbFile.readInt32LE()
        rsbHeadInfo.ptxInfoBeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.ptxInfoEachLength = rsbFile.readInt32LE()
        rsbHeadInfo.part1BeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.part2BeginOffset = rsbFile.readInt32LE()
        rsbHeadInfo.part3BeginOffset = rsbFile.readInt32LE()
        if (version == 4 || version == 5) {
            rsbHeadInfo.fileOffset = rsbFile.readInt32LE()
        }
        return rsbHeadInfo
    }

    private fun fileListSplit(
        rsbFile: SenBuffer,
        tempOffset: Int,
        tempLength: Int,
        fileList: MutableList<FileListInfo>
    ) {
        rsbFile.readOffset = tempOffset
        val nameDict = mutableListOf<NameDict>()
        var namePath = ""
        val offsetLimit = tempOffset + tempLength
        while (rsbFile.readOffset < offsetLimit) {
            val characterByte = rsbFile.readString(1)
            val offsetByte = rsbFile.readInt24LE() * 4
            if (characterByte == "\u0000") {
                if (offsetByte != 0) {
                    nameDict.add(NameDict(namePath = namePath, offsetByte = offsetByte))
                }
                fileList.add(FileListInfo(namePath = namePath, poolIndex = rsbFile.readInt32LE()))
                for (i in nameDict.indices) {
                    if ((nameDict[i].offsetByte + tempOffset) == rsbFile.readOffset) {
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
        fileList.sortBy { it.poolIndex }
        checkEndOffset(rsbFile, offsetLimit)
    }

    private fun readCompositeInfo(
        rsbFile: SenBuffer,
        rsbHeadInfo: RSBHead,
        compositeInfoList: MutableList<CompositeInfo>
    ) {
        rsbFile.readOffset = rsbHeadInfo.compositeInfoBeginOffset
        for (i in 0 until rsbHeadInfo.compositeNumber) {
            val startOffset = rsbFile.readOffset
            val compositeName = rsbFile.readStringByEmpty()
            val packetNumber = rsbFile.readInt32LE(startOffset + rsbHeadInfo.compositeInfoEachLength - 4)
            rsbFile.backupReadOffset()
            rsbFile.readOffset = startOffset + 128
            val packetInfo = mutableListOf<CompositePacketInfo>()
            for (k in 0 until packetNumber) {
                packetInfo.add(
                    CompositePacketInfo(
                        packetIndex = rsbFile.readInt32LE(),
                        category = mutableListOf(
                            rsbFile.readInt32LE().toString(),
                            rsbFile.readString(4).replace("\u0000", "")
                        )
                    )
                )
                rsbFile.readUBytes(4)
            }
            compositeInfoList.add(
                CompositeInfo(
                    name = compositeName.replace("_CompositeShell", ""),
                    isComposite = !compositeName.endsWith("_CompositeShell"),
                    packetNumber = packetNumber,
                    packetInfo = packetInfo
                )
            )
            rsbFile.restoreReadOffset()
        }
        val endOffset =
            rsbHeadInfo.compositeInfoEachLength * rsbHeadInfo.compositeNumber + rsbHeadInfo.compositeInfoBeginOffset
        checkEndOffset(rsbFile, endOffset)
    }

    private fun readRSGInfo(rsbFile: SenBuffer, rsbHeadInfo: RSBHead, rsgInfoList: MutableList<RSGInfo>) {
        rsbFile.readOffset = rsbHeadInfo.rsgInfoBeginOffset
        for (i in 0 until rsbHeadInfo.rsgNumber) {
            val startOffset = rsbFile.readOffset
            val packetName = rsbFile.readStringByEmpty()
            rsbFile.readOffset = startOffset + 128
            val rsgOffset = rsbFile.readInt32LE()
            val rsgLength = rsbFile.readInt32LE()
            val rsgIndex = rsbFile.readInt32LE()
            val ptxNumber = rsbFile.readInt32LE(startOffset + rsbHeadInfo.rsgInfoEachLength - 8)
            val ptxBeforeNumber = rsbFile.readInt32LE()
            rsgInfoList.add(
                RSGInfo(
                    name = packetName,
                    rsgOffset = rsgOffset,
                    rsgLength = rsgLength,
                    poolIndex = rsgIndex,
                    ptxNumber = ptxNumber,
                    ptxBeforeNumber = ptxBeforeNumber
                )
            )
        }
        val endOffset = rsbHeadInfo.rsgInfoEachLength * rsbHeadInfo.rsgNumber + rsbHeadInfo.rsgInfoBeginOffset
        checkEndOffset(rsbFile, endOffset)
    }

    private fun readAutoPool(rsbFile: SenBuffer, rsbHeadInfo: RSBHead, autoPoolList: MutableList<AutoPoolInfo>) {
        rsbFile.readOffset = rsbHeadInfo.autoPoolInfoBeginOffset
        for (i in 0 until rsbHeadInfo.autoPoolNumber) {
            val startOffset = rsbFile.readOffset
            val autoPoolName = rsbFile.readStringByEmpty()
            rsbFile.readOffset = startOffset + 128
            autoPoolList.add(
                AutoPoolInfo(
                    name = autoPoolName,
                    part0Size = rsbFile.readInt32LE(),
                    part1Size = rsbFile.readInt32LE()
                )
            )
            rsbFile.readOffset = startOffset + rsbHeadInfo.autoPoolInfoEachLength
        }
        val endOffset =
            rsbHeadInfo.autoPoolInfoEachLength * rsbHeadInfo.autoPoolNumber + rsbHeadInfo.autoPoolInfoBeginOffset
        checkEndOffset(rsbFile, endOffset)
    }

    private fun readPTXInfo(rsbFile: SenBuffer, rsbHeadInfo: RSBHead, ptxInfoList: MutableList<RSBPTXInfo>) {
        rsbFile.readOffset = rsbHeadInfo.ptxInfoBeginOffset
        if (rsbHeadInfo.ptxInfoEachLength != 0x10 && rsbHeadInfo.ptxInfoEachLength != 0x14 && rsbHeadInfo.ptxInfoEachLength != 0x18) {
            throw Exception("Invalid PTX info each length")
        }
        for (i in 0 until rsbHeadInfo.ptxNumber) {
            val width = rsbFile.readInt32LE()
            val height = rsbFile.readInt32LE()
            val pitch = rsbFile.readInt32LE()
            val format = rsbFile.readInt32LE()
            val ptxInfo = RSBPTXInfo(
                ptxIndex = i,
                width = width,
                height = height,
                pitch = pitch,
                format = format
            )
            if (rsbHeadInfo.ptxInfoEachLength >= 0x14) {
                ptxInfo.alphaSize = rsbFile.readInt32LE()
                ptxInfo.alphaFormat =
                    if (rsbHeadInfo.ptxInfoEachLength == 0x18) rsbFile.readInt32LE() else if (ptxInfo.alphaSize == 0) 0 else 0x64
            } else {
                ptxInfo.alphaSize = null
                ptxInfo.alphaFormat = null
            }
            ptxInfoList.add(ptxInfo)
        }
        val endOffset = rsbHeadInfo.ptxInfoEachLength * rsbHeadInfo.ptxNumber + rsbHeadInfo.ptxInfoBeginOffset
        checkEndOffset(rsbFile, endOffset)
    }

    private fun checkEndOffset(rsbFile: SenBuffer, endOffset: Int) {
        if (rsbFile.readOffset != endOffset) {
            throw Exception("Invalid offset: ${rsbFile.readOffset} | $endOffset")
        }
    }

    fun pack(inFolder: String, outFile: String, manifestInfo: ManifestInfo) {
        val rsbFile = SenBuffer()
        rsbFile.writeString("1bsr")
        val version = manifestInfo.version
        val fileListBeginOffset = when (version) {
            3 -> 0x6C
            4 -> 0x70
            else -> throw Exception("invalid_rsb_version")
        }
        val rsbHeadInfo = RSBHead()
        rsbFile.writeInt32LE(version)
        rsbFile.writeNull(fileListBeginOffset - 8)
        rsbHeadInfo.ptxInfoEachLength = manifestInfo.ptxInfoSize
        if (rsbHeadInfo.ptxInfoEachLength != 0x10 && rsbHeadInfo.ptxInfoEachLength != 0x14 && rsbHeadInfo.ptxInfoEachLength != 0x18) {
            throw Exception("Invalid ptx info each length")
        }
        val fileList = mutableListOf<FileListInfo>()
        val rsgFileList = mutableListOf<FileListInfo>()
        val compositeList = mutableListOf<FileListInfo>()
        val compositeInfo = SenBuffer()
        val rsgInfo = SenBuffer()
        val autoPoolInfo = SenBuffer()
        val ptxInfo = SenBuffer()
        val rsgFileBank = SenBuffer()
        var rsgPacketIndex = 0
        val groupLength = manifestInfo.group.size
        var ptxBeforeNumber = 0
        for (i in 0 until groupLength) {
            val kFirst = manifestInfo.group[i]
            val compositeName = if (kFirst.isComposite) kFirst.name else "${kFirst.name}_CompositeShell"
            compositeList.add(
                FileListInfo(
                    namePath = compositeName.uppercase(Locale.getDefault()),
                    poolIndex = i
                )
            )
            compositeInfo.writeString(compositeName)
            compositeInfo.writeNull(128 - compositeName.length)
            val subgroupLength = kFirst.subGroup.size
            for (k in 0 until subgroupLength) {
                val kSecond = kFirst.subGroup[k]
                val rsgName = kSecond.namePacket
                var rsgComposite = false
                rsgFileList.add(
                    FileListInfo(
                        namePath = rsgName.uppercase(Locale.getDefault()),
                        poolIndex = rsgPacketIndex
                    )
                )
                val rsgFile = SenBuffer(Paths.get("$inFolder${s}packet${s}$rsgName.rsg"))
                comparePacketInfo(kSecond.packetInfo, rsgFile)
                var ptxNumber = 0
                val resInfoLength = kSecond.packetInfo.res.size
                for (l in 0 until resInfoLength) {
                    val kThird = kSecond.packetInfo.res[l]
                    fileList.add(
                        FileListInfo(
                            namePath = kThird.path.uppercase(Locale.getDefault()),
                            poolIndex = rsgPacketIndex
                        )
                    )
                    if (kThird.ptxInfo != null) {
                        ptxNumber++
                        rsgComposite = true
                        // Write PTXInfo
                        val id = kThird.ptxInfo!!.id
                        val ptxOffset = (ptxBeforeNumber + id) * rsbHeadInfo.ptxInfoEachLength
                        ptxInfo.writeInt32LE(kThird.ptxInfo!!.width, ptxOffset)
                        ptxInfo.writeInt32LE(kThird.ptxInfo!!.height)
                        val format = kThird.ptxProperty!!.format
                        val pitch = kThird.ptxProperty!!.pitch
                        ptxInfo.writeInt32LE(pitch)
                        ptxInfo.writeInt32LE(format)
                        val alphaSize = kThird.ptxProperty?.alphaSize
                        val alphaFormat = kThird.ptxProperty?.alphaFormat
                        if (rsbHeadInfo.ptxInfoEachLength != 0x10) {
                            ptxInfo.writeInt32LE(alphaSize ?: 0)
                        }
                        if (rsbHeadInfo.ptxInfoEachLength == 0x18) {
                            ptxInfo.writeInt32LE(alphaFormat ?: 0)
                        }
                    }
                }
                // Write CompositeInfo
                compositeInfo.writeInt32LE(rsgPacketIndex)
                compositeInfo.writeInt32LE(Integer.parseInt(kSecond.category[0]))
                if (kSecond.category[1] != "") {
                    if (kSecond.category[1].length != 4) {
                        throw Exception("Category out of length")
                    }
                    compositeInfo.writeString(kSecond.category[1])
                } else {
                    compositeInfo.writeNull(4)
                }
                compositeInfo.writeNull(4)
                // Write RSGInfo
                rsgInfo.writeString(rsgName)
                rsgInfo.writeNull(128 - rsgName.length)
                rsgInfo.writeInt32LE(rsgFileBank.writeOffset)
                // WritePacket
                rsgFileBank.writeUBytes(rsgFile.toUBytes())
                rsgInfo.writeInt32LE(rsgFile.size)
                rsgInfo.writeInt32LE(rsgPacketIndex)
                rsgInfo.writeUBytes(rsgFile.getUBytes(56, 0x10))
                val rsgWriteOffset = rsgInfo.writeOffset
                rsgInfo.writeInt32LE(rsgFile.readInt32LE(0x20), rsgWriteOffset - 36)
                rsgInfo.writeInt32LE(ptxNumber, rsgWriteOffset)
                rsgInfo.writeInt32LE(ptxBeforeNumber)
                ptxBeforeNumber += ptxNumber
                // Write AutoPoolInfo
                val autoPoolName = "${rsgName}_AutoPool"
                autoPoolInfo.writeString(autoPoolName)
                autoPoolInfo.writeNull(128 - autoPoolName.length)
                if (rsgComposite) {
                    autoPoolInfo.writeInt32LE(rsgFile.readInt32LE(0x18))
                    autoPoolInfo.writeInt32LE(rsgFile.readInt32LE(0x30))
                } else {
                    autoPoolInfo.writeInt32LE(rsgFile.readInt32LE(0x18) + rsgFile.readInt32LE(0x20))
                    autoPoolInfo.writeInt32LE(0)
                }
                autoPoolInfo.writeInt32LE(1)
                autoPoolInfo.writeNull(12)
                rsgPacketIndex++
                rsgFile.readOffset = 0
            }
            compositeInfo.writeNull(1024 - (subgroupLength * 16))
            compositeInfo.writeInt32LE(subgroupLength)
            val fileListPathTemp = mutableListOf<RSBPathTemp>()
            fileListPack(fileList, fileListPathTemp)
            val rsgListPathTemp = mutableListOf<RSBPathTemp>()
            fileListPack(rsgFileList, rsgListPathTemp)
            val compositeListPathTemp = mutableListOf<RSBPathTemp>()
            fileListPack(compositeList, compositeListPathTemp)
            val fileListPathTempLength = fileListPathTemp.size
            rsbHeadInfo.fileListBeginOffset = fileListBeginOffset
            // FileList
            for (j in 0 until fileListPathTempLength) {
                writeFileList(rsbFile, fileListPathTemp[j])
            }
            rsbHeadInfo.fileListLength = rsbFile.writeOffset - fileListBeginOffset
            // RSGList
            val rsgListPathTempLength = rsgListPathTemp.size
            rsbHeadInfo.rsgListBeginOffset = rsbFile.writeOffset
            for (k in 0 until rsgListPathTempLength) {
                writeFileList(rsbFile, rsgListPathTemp[k])
            }
            rsbHeadInfo.rsgListLength = rsbFile.writeOffset - rsbHeadInfo.rsgListBeginOffset
            // CompositeInfo
            rsbHeadInfo.compositeNumber = groupLength
            rsbHeadInfo.compositeInfoBeginOffset = rsbFile.writeOffset
            rsbFile.writeUBytes(compositeInfo.toUBytes())
            compositeInfo.close()
            // CompositeList
            val compositeListPathTempLength = compositeListPathTemp.size
            rsbHeadInfo.compositeListBeginOffset = rsbFile.writeOffset
            for (j in 0 until compositeListPathTempLength) {
                writeFileList(rsbFile, compositeListPathTemp[j])
            }
            rsbHeadInfo.compositeListLength = rsbFile.writeOffset - rsbHeadInfo.compositeListBeginOffset
            // RSGInfo
            rsbHeadInfo.rsgInfoBeginOffset = rsbFile.writeOffset
            rsbHeadInfo.rsgNumber = rsgPacketIndex
            rsbFile.writeUBytes(rsgInfo.toUBytes())
            rsgInfo.close()
            // AutoPoolInfo
            rsbHeadInfo.autoPoolInfoBeginOffset = rsbFile.writeOffset
            rsbHeadInfo.autoPoolNumber = rsgPacketIndex
            rsbFile.writeUBytes(autoPoolInfo.toUBytes())
            autoPoolInfo.close()
            // PTXInfo
            rsbHeadInfo.ptxInfoBeginOffset = rsbFile.writeOffset
            rsbHeadInfo.ptxNumber = ptxBeforeNumber
            rsbFile.writeUBytes(ptxInfo.toUBytes())
            ptxInfo.close()
            if (version == 3) {
                // Description
                writeResourcesDescription(rsbFile, rsbHeadInfo, inFolder)
            }
            rsbFile.writeNull(RSGFunction.beautifyLength(rsbFile.writeOffset))
            // Packet
            val fileOffset = rsbFile.writeOffset
            rsbHeadInfo.fileOffset = fileOffset
            rsbFile.writeUBytes(rsgFileBank.toUBytes())
            rsgFileBank.close()
            // Rewrite PacketOffset
            rsbFile.readOffset = rsbHeadInfo.rsgInfoBeginOffset
            for (j in 0 until rsbHeadInfo.rsgNumber) {
                val rsgInfoFileOffset = (rsbHeadInfo.rsgInfoBeginOffset + j * rsbHeadInfo.rsgInfoEachLength) + 128
                val packetOffset = rsbFile.readInt32LE(rsgInfoFileOffset)
                rsbFile.writeInt32LE(packetOffset + fileOffset, rsgInfoFileOffset)
            }
            // WriteHead
            rsbHeadInfo.version = version
            writeHead(rsbFile, rsbHeadInfo)
        }
        rsbFile.outFile(Paths.get(outFile))
        return
    }

    private fun writeResourcesDescription(rsbFile: SenBuffer, rsbHeadInfo: RSBHead, inFolder: String) {
        val jsonString = Files.readString(Paths.get("$inFolder${s}description.json"))
        val resourcesDescription = JSON.parseObject(jsonString, ResourcesDescription::class.java)
        val groupKeys = resourcesDescription.groups.keys
        val part1Res = SenBuffer()
        val part2Res = SenBuffer()
        val part3Res = SenBuffer()
        val stringPool = mutableMapOf<String, Int>()
        fun throwInPool(poolKey: String): Int {
            if (!stringPool.containsKey(poolKey)) {
                stringPool[poolKey] = part3Res.writeOffset
                part3Res.writeStringByEmpty(poolKey)
            }
            return stringPool[poolKey]!!
        }
        part3Res.writeNull(1)
        stringPool[""] = 0
        for (gKey in groupKeys) {
            var idOffsetPart3 = throwInPool(gKey)
            part1Res.writeInt32LE(idOffsetPart3)
            val subgroupKeys = resourcesDescription.groups[gKey]!!.subgroups.keys
            part1Res.writeInt32LE(subgroupKeys.size)
            part1Res.writeInt32LE(0x10)
            for (gpKey in subgroupKeys) {
                part1Res.writeInt32LE(Integer.parseInt(resourcesDescription.groups[gKey]!!.subgroups[gpKey]!!.res))
                val language = resourcesDescription.groups[gKey]!!.subgroups[gpKey]!!.language
                if (language == "") {
                    part1Res.writeInt32LE(0x0)
                } else {
                    part1Res.writeString(("$language    ").substring(0, 4))
                }
                val rsgIdOffsetPart3 = throwInPool(gpKey)
                part1Res.writeInt32LE(rsgIdOffsetPart3)
                val resourcesKeys = resourcesDescription.groups[gKey]!!.subgroups[gpKey]!!.resources.keys
                part1Res.writeInt32LE(resourcesKeys.size)
                for (rsKey in resourcesKeys) {
                    val idOffsetPart2 = part2Res.writeOffset
                    part1Res.writeInt32LE(idOffsetPart2)
                    // Start writePart2
                    run {
                        part2Res.writeInt32LE(0x0)
                        val type = resourcesDescription.groups[gKey]!!.subgroups[gpKey]!!.resources[rsKey]!!.type
                        part2Res.writeUInt16LE(type.toUShort())
                        part2Res.writeUInt16LE(0x1C.toUShort())
                        part2Res.backupWriteOffset()
                        part2Res.writeOffset += 0x8
                        idOffsetPart3 = throwInPool(rsKey)
                        val pathOffsetPart3 =
                            throwInPool(resourcesDescription.groups[gKey]!!.subgroups[gpKey]!!.resources[rsKey]!!.path)
                        part2Res.writeInt32LE(idOffsetPart3)
                        part2Res.writeInt32LE(pathOffsetPart3)
                        val properties =
                            resourcesDescription.groups[gKey]!!.subgroups[gpKey]!!.resources[rsKey]!!.properties
                        val propertiesNumber = properties.size
                        part2Res.writeInt32LE(propertiesNumber)
                        if (type == 0) {
                            val ptxInfoBeginOffsetPart2 = part2Res.writeOffset
                            // Write PTXInfo
                            run {
                                val ptxInfo =
                                    resourcesDescription.groups[gKey]!!.subgroups[gpKey]!!.resources[rsKey]!!.ptxInfo
                                part2Res.writeUInt16LE(ptxInfo?.imagetype?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.aflags?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.x?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.y?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.ax?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.ay?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.aw?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.ah?.toUShort() ?: 0u)
                                part2Res.writeUInt16LE(ptxInfo?.rows?.toUShort() ?: 1u)
                                part2Res.writeUInt16LE(ptxInfo?.cols?.toUShort() ?: 1u)
                                val parentOffsetInPart3 = throwInPool(ptxInfo?.parent ?: "")
                                part2Res.writeInt32LE(parentOffsetInPart3)
                            }
                            val ptxInfoEndOffsetPart2 = part2Res.writeOffset
                            part2Res.restoreWriteOffset()
                            part2Res.writeInt32LE(ptxInfoEndOffsetPart2)
                            part2Res.writeInt32LE(ptxInfoBeginOffsetPart2)
                            part2Res.writeOffset = ptxInfoEndOffsetPart2
                        }
                        for ((key, value) in properties) {
                            val keyOffsetInPart3 = throwInPool(key)
                            val valueOffsetInPart3 = throwInPool(value)
                            part2Res.writeInt32LE(keyOffsetInPart3)
                            part2Res.writeInt32LE(0x0)
                            part2Res.writeInt32LE(valueOffsetInPart3)
                        }
                    }
                    //-----------
                }
            }
        }
        rsbHeadInfo.part1BeginOffset = rsbFile.writeOffset
        rsbFile.writeUBytes(part1Res.toUBytes())
        part1Res.close()
        rsbHeadInfo.part2BeginOffset = rsbFile.writeOffset
        rsbFile.writeUBytes(part2Res.toUBytes())
        part2Res.close()
        rsbHeadInfo.part3BeginOffset = rsbFile.writeOffset
        rsbFile.writeUBytes(part3Res.toUBytes())
        part3Res.close()
    }

    private fun writeHead(rsbFile: SenBuffer, rsbHeadInfo: RSBHead) {
        rsbFile.writeInt32LE(rsbHeadInfo.fileOffset, 12)
        rsbFile.writeInt32LE(rsbHeadInfo.fileListLength)
        rsbFile.writeInt32LE(rsbHeadInfo.fileListBeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.rsgListLength, 32)
        rsbFile.writeInt32LE(rsbHeadInfo.rsgListBeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.rsgNumber)
        rsbFile.writeInt32LE(rsbHeadInfo.rsgInfoBeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.rsgInfoEachLength)
        rsbFile.writeInt32LE(rsbHeadInfo.compositeNumber)
        rsbFile.writeInt32LE(rsbHeadInfo.compositeInfoBeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.compositeInfoEachLength)
        rsbFile.writeInt32LE(rsbHeadInfo.compositeListLength)
        rsbFile.writeInt32LE(rsbHeadInfo.compositeListBeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.autoPoolNumber)
        rsbFile.writeInt32LE(rsbHeadInfo.autoPoolInfoBeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.autoPoolInfoEachLength)
        rsbFile.writeInt32LE(rsbHeadInfo.ptxNumber)
        rsbFile.writeInt32LE(rsbHeadInfo.ptxInfoBeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.ptxInfoEachLength)
        rsbFile.writeInt32LE(rsbHeadInfo.part1BeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.part2BeginOffset)
        rsbFile.writeInt32LE(rsbHeadInfo.part3BeginOffset)
        if (rsbHeadInfo.version == 4) {
            rsbFile.writeInt32LE(rsbHeadInfo.fileOffset)
        }
    }

    private fun writeFileList(rsbFile: SenBuffer, pathTemp: RSBPathTemp) {
        val beginOffset = rsbFile.writeOffset
        rsbFile.writeStringFourUByte(pathTemp.pathSlice)
        rsbFile.backupWriteOffset()
        for (h in pathTemp.positions.indices) {
            rsbFile.writeInt24LE(pathTemp.positions[h].position, beginOffset + pathTemp.positions[h].offset * 4 + 1)
        }
        rsbFile.restoreWriteOffset()
        rsbFile.writeInt32LE(pathTemp.poolIndex)
    }

    private fun comparePacketInfo(modifyPacketInfo: RSBPacketInfo, rsgFile: SenBuffer) {
        fun <T> throwError(typeError: String, oriValue: T, modifyValue: T) {
            throw Exception("RSG $typeError is not the same. In ManiFest: $modifyValue | In RSGFile: $oriValue. RSG path: ${rsgFile.filePath}")
        }

        val oriPacketInfo = RSGFunction.unpack(rsgFile, "", useResFolder = false, getPacketInfo = true)
        if (oriPacketInfo.version != modifyPacketInfo.version) throwError(
            "version",
            oriPacketInfo.version,
            modifyPacketInfo.version
        )
        if (oriPacketInfo.compressionFlags != modifyPacketInfo.compressionFlags) throwError(
            "compression flags",
            oriPacketInfo.compressionFlags,
            modifyPacketInfo.compressionFlags
        )
        if (oriPacketInfo.res.size != modifyPacketInfo.res.size) throwError(
            "item index",
            oriPacketInfo.res.size,
            modifyPacketInfo.res.size
        )
        val oriPacketResInfo = oriPacketInfo.res.sortedBy { it.path }
        val modifyPacketResInfo = modifyPacketInfo.res.sortedBy { it.path }
        for (i in modifyPacketResInfo.indices) {
            if (oriPacketResInfo[i].path != modifyPacketResInfo[i].path) throwError(
                "item path",
                oriPacketResInfo[i].path,
                modifyPacketResInfo[i].path
            )
            if (oriPacketResInfo[i].ptxInfo != null && modifyPacketResInfo[i].ptxInfo != null) {
                if (oriPacketResInfo[i].ptxInfo!!.id != modifyPacketResInfo[i].ptxInfo!!.id) throwError(
                    "item id",
                    oriPacketResInfo[i].ptxInfo!!.id,
                    modifyPacketResInfo[i].ptxInfo!!.id
                )
                if (oriPacketResInfo[i].ptxInfo!!.width != modifyPacketResInfo[i].ptxInfo!!.width) throwError(
                    "item width",
                    oriPacketResInfo[i].ptxInfo!!.width,
                    modifyPacketResInfo[i].ptxInfo!!.width
                )
                if (oriPacketResInfo[i].ptxInfo!!.height != modifyPacketResInfo[i].ptxInfo!!.height) throwError(
                    "item height",
                    oriPacketResInfo[i].ptxInfo!!.height,
                    modifyPacketResInfo[i].ptxInfo!!.height
                )
            }
        }
    }

    private fun fileListPack(fileList: MutableList<FileListInfo>, pathTempList: MutableList<RSBPathTemp>) {
        fileList.sortBy { it.namePath }
        fileList.add(0, FileListInfo(namePath = "", poolIndex = -1))
        val listLength = fileList.size - 1
        var wPosition = 0
        for (i in 0 until listLength) {
            val path1 = fileList[i].namePath.uppercase()
            val path2 = fileList[i + 1].namePath.uppercase()
            if (RSGFunction.isNotASCII(path2)) {
                throw Exception("Item part must be ascii")
            }
            val strLongestLength = maxOf(path1.length, path2.length)
            for (k in 0 until strLongestLength) {
                if (k >= path1.length || k >= path2.length || path1[k] != path2[k]) {
                    for (h in pathTempList.indices.reversed()) {
                        if (k >= pathTempList[h].key) {
                            pathTempList[h].positions.add(
                                PathPosition(
                                    position = wPosition,
                                    offset = (k - pathTempList[h].key)
                                )
                            )
                            break
                        }
                    }
                    wPosition += path2.length - k + 2
                    pathTempList.add(
                        RSBPathTemp(
                            pathSlice = path2.substring(k),
                            key = k,
                            poolIndex = fileList[i + 1].poolIndex
                        )
                    )
                    break
                }
            }
        }
    }

    fun rsbObfuscate(rsbFile: SenBuffer) {
        val headInfo = readHead(rsbFile)
        val rsgNumber = headInfo.rsgNumber
        rsbFile.readOffset = headInfo.rsgInfoBeginOffset
        for (i in 0 until rsgNumber) {
            val startOffset = rsbFile.readOffset
            val autopoolStartOffset = headInfo.autoPoolInfoBeginOffset + i * 152
            rsbFile.writeNull(128, startOffset)
            rsbFile.writeNull(128, autopoolStartOffset)
            rsbFile.writeNull(4, startOffset + 132)
            val packetOffset = rsbFile.readUInt32LE(startOffset + 128)
            rsbFile.writeNull(64, packetOffset.toInt())
            rsbFile.readOffset = startOffset + headInfo.rsgInfoEachLength
        }
    }

    enum class Version(val value: Int) {
        Other(3),
        PvZ2(4)
    }

    fun unpackByLooseConstraints(rsbFile: SenBuffer, outFolder: String, version: Version): ManifestInfo {
        val rsbHeadInfo = readHead(rsbFile)
        if (version != Version.Other && version != Version.PvZ2) {
            throw Exception("Unsupported RSB structure")
        }
        rsbHeadInfo.version = version.value
        val rsgList = mutableListOf<FileListInfo>()
        fileListSplit(rsbFile, rsbHeadInfo.rsgListBeginOffset, rsbHeadInfo.rsgListLength, rsgList)
        val compositeInfo = mutableListOf<CompositeInfo>()
        readCompositeInfo(rsbFile, rsbHeadInfo, compositeInfo)
        val rsgInfoList = mutableListOf<RSGInfo>()
        readRSGInfoByLooseConstraints(rsbFile, rsbHeadInfo, rsgInfoList)
        val ptxInfoList = mutableListOf<RSBPTXInfo>()
        readPTXInfo(rsbFile, rsbHeadInfo, ptxInfoList)
        if (rsbHeadInfo.version == 3) {
            if (rsbHeadInfo.part1BeginOffset == 0 && rsbHeadInfo.part2BeginOffset == 0 && rsbHeadInfo.part3BeginOffset == 0) {
                throw Exception("Invalid RSB ver 3 resource offset")
            }
            readResourcesDescription(rsbFile, rsbHeadInfo, outFolder)
        }
        val groupList = mutableListOf<GroupInfo>()
        val compositeLength = compositeInfo.size
        val rsgNameList = mutableListOf<String>()
        for (i in 0 until compositeLength) {
            val subGroupList = mutableListOf<SubGroupInfo>()
            for (k in 0 until compositeInfo[i].packetNumber) {
                val packetIndex = compositeInfo[i].packetInfo!![k].packetIndex
                var rsgInfoCount = 0
                var rsgListCount = 0
                while (rsgInfoList[rsgInfoCount].poolIndex != packetIndex) {
                    if (rsgInfoCount >= rsgInfoList.size - 1) {
                        throw Exception("Out of range")
                    }
                    rsgInfoCount++
                }
                while (rsgList[rsgListCount].poolIndex != packetIndex) {
                    if (rsgListCount >= rsgList.size - 1) {
                        throw Exception("Out of range")
                    }
                    rsgListCount++
                }
                if (rsgInfoList[rsgInfoCount].name == "break") {
                    continue
                }
                val packetFile =
                    rsbFile.getUBytes(rsgInfoList[rsgInfoCount].rsgLength, rsgInfoList[rsgInfoCount].rsgOffset)
                val rsgFile = SenBuffer(packetFile)
                fixRSG(
                    rsgFile,
                    rsbHeadInfo.version,
                    SenBuffer(rsgInfoList[rsgInfoCount].packetHeadInfo!!.toUByteArray())
                )
                val packetInfo = RSGFunction.unpack(
                    rsgFile,
                    "$outFolder${s}unpack",
                    useResFolder = false,
                    getPacketInfo = false
                )
                val resInfoList = mutableListOf<RSBResInfo>()
                val ptxBeforeNumber = rsgInfoList[rsgInfoCount].ptxBeforeNumber
                for (h in 0 until packetInfo.res.size) {
                    val resInfo = RSBResInfo(
                        path = packetInfo.res[h].path
                    )
                    if (packetInfo.res[h].ptxInfo != null) {
                        resInfo.ptxInfo = packetInfo.res[h].ptxInfo
                        resInfo.ptxProperty = PTXProperty(
                            format = ptxInfoList[ptxBeforeNumber + packetInfo.res[h].ptxInfo!!.id].format,
                            pitch = ptxInfoList[ptxBeforeNumber + packetInfo.res[h].ptxInfo!!.id].pitch,
                            alphaSize = ptxInfoList[ptxBeforeNumber + packetInfo.res[h].ptxInfo!!.id].alphaSize,
                            alphaFormat = ptxInfoList[ptxBeforeNumber + packetInfo.res[h].ptxInfo!!.id].alphaFormat,
                        )
                    }
                    resInfoList.add(resInfo)
                }
                val packetInfoList = RSBPacketInfo(
                    version = packetInfo.version,
                    compressionFlags = packetInfo.compressionFlags,
                    res = resInfoList.toMutableList(),
                )
                subGroupList.add(
                    SubGroupInfo(
                        namePacket = rsgList[rsgListCount].namePath,
                        category = mutableListOf(
                            compositeInfo[i].packetInfo!![k].category[0],
                            compositeInfo[i].packetInfo!![k].category[1]
                        ),
                        packetInfo = packetInfoList,
                    )
                )
            }
            groupList.add(
                GroupInfo(
                    name = compositeInfo[i].name,
                    isComposite = compositeInfo[i].isComposite,
                    subGroup = subGroupList.toMutableList(),
                )
            )
        }
        val manifestInfo = ManifestInfo(
            version = rsbHeadInfo.version,
            ptxInfoSize = rsbHeadInfo.ptxInfoEachLength,
            path = RSBPathInfo(
                rsgs = rsgNameList.toMutableList(),
                packetPath = "$outFolder${s}packet",
            ),
            group = groupList.toMutableList(),
        )
        rsbFile.close()
        return manifestInfo
    }

    private fun fixRSG(rsgFile: SenBuffer, version: Int, rsgInfo: SenBuffer) {
        val rsgMagic = rsgFile.readString(4)
        val rsgVersion = rsgFile.readInt32LE()
        val rsgCompressionFlag = rsgFile.readInt32LE(0x10)
        rsgFile.readOffset = 0
        if (rsgMagic == "pgsr" && rsgVersion == version && rsgCompressionFlag in 0..3 && rsgCompressionFlag == rsgInfo.readInt32LE()) {
            return
        } else {
            rsgFile.writeString("pgsr")
            rsgFile.writeInt32LE(version)
            rsgFile.writeNull(8)
            rsgFile.writeUBytes(rsgInfo.toUBytes())
            rsgFile.writeNull(16)
            rsgInfo.close()
        }
        if (rsgCompressionFlag < 0 || rsgCompressionFlag > 3) {
            val part0Zlib = rsgFile.readInt32LE(0x1C)
            val part0Size = rsgFile.readInt32LE()
            val part1Zlib = rsgFile.readInt32LE(0x1C)
            val part1Size = rsgFile.readInt32LE()
            rsgFile.readOffset = 0
            val compressionFlags = when {
                (part0Zlib == part0Size && part1Zlib == 0) || (part1Zlib != part1Size && part0Size == 0) -> 1
                (part0Zlib != part0Size && part1Zlib == 0) || (part1Zlib == part1Size && part0Size == 0) -> 2
                else -> 3
            }
            rsgFile.writeInt32LE(compressionFlags, 0x10)
        }
    }

    private fun readRSGInfoByLooseConstraints(
        rsbFile: SenBuffer,
        rsbHeadInfo: RSBHead,
        rsgInfoList: MutableList<RSGInfo>
    ) {
        rsbFile.readOffset = rsbHeadInfo.rsgInfoBeginOffset
        for (i in 0 until rsbHeadInfo.rsgNumber) {
            val startOffset = rsbFile.readOffset
            val rsgOffset = rsbFile.readInt32LE(startOffset + 128)
            val rsgIndex = rsbFile.readInt32LE(startOffset + 136)
            if (isNotRSG(rsbFile, rsgOffset)) {
                rsgInfoList.add(
                    RSGInfo(
                        name = "break",
                        rsgOffset = 0,
                        rsgLength = 0,
                        poolIndex = rsgIndex,
                        ptxNumber = 0,
                        ptxBeforeNumber = 0
                    )
                )
                rsbFile.readOffset = startOffset + rsbHeadInfo.rsgInfoEachLength
                continue
            }
            val packetHeadInfo = rsbFile.readUBytes(32, startOffset + 140)
            var rsgLength =
                rsbFile.readInt32LE(startOffset + 148) + rsbFile.readInt32LE(startOffset + 152) + rsbFile.readInt32LE(
                    startOffset + 168
                )
            if (rsgLength <= 1024) rsgLength = 4096
            val ptxNumber = rsbFile.readInt32LE(startOffset + rsbHeadInfo.rsgInfoEachLength - 8)
            val ptxBeforeNumber = rsbFile.readInt32LE()
            rsgInfoList.add(
                RSGInfo(
                    name = "",
                    rsgOffset = rsgOffset,
                    rsgLength = rsgLength,
                    poolIndex = rsgIndex,
                    ptxNumber = ptxNumber,
                    ptxBeforeNumber = ptxBeforeNumber,
                    packetHeadInfo = packetHeadInfo.map { it }.toMutableList()
                )
            )
        }
        val endOffset = rsbHeadInfo.rsgInfoEachLength * rsbHeadInfo.rsgNumber + rsbHeadInfo.rsgInfoBeginOffset
        checkEndOffset(rsbFile, endOffset)
    }

    private fun isNotRSG(rsbFile: SenBuffer, rsgOffset: Int): Boolean {
        rsbFile.backupReadOffset()
        val fileListOffset = rsbFile.readInt32LE(rsgOffset + 76)
        rsbFile.restoreReadOffset()
        return fileListOffset != 0x5C && fileListOffset != 0x1000
    }
}