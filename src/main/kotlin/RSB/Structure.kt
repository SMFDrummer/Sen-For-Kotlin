package RSB

import RSG.PTXInfo
import RSG.PathPosition
import kotlinx.serialization.Serializable

@Serializable
data class ManifestInfo(
    val version: Int,
    val ptxInfoSize: Int,
    val path: RSBPathInfo,
    val group: MutableList<GroupInfo>
)

@Serializable
data class RSBPathInfo(
    val rsgs: MutableList<String>,
    val packetPath: String
)

@Serializable
data class GroupInfo(
    val name: String,
    val isComposite: Boolean,
    val subGroup: MutableList<SubGroupInfo>
)

@Serializable
data class SubGroupInfo(
    val namePacket: String,
    val category: MutableList<String>,
    val packetInfo: RSBPacketInfo
)

@Serializable
data class RSBPacketInfo(
    val version: Int,
    val compressionFlags: Int,
    val res: MutableList<RSBResInfo>
)

@Serializable
data class RSBResInfo(
    val path: String,
    var ptxInfo: PTXInfo? = null,
    var ptxProperty: PTXProperty? = null
)

@Serializable
data class PTXProperty(
    val format: Int,
    val pitch: Int,
    val alphaSize: Int? = null,
    val alphaFormat: Int? = null
)

@Serializable
data class RSBHead(
    val header: String = "1bsr",
    var version: Int = 0,
    var fileOffset: Int = 0,
    var fileListLength: Int = 0,
    var fileListBeginOffset: Int = 0,
    var rsgListLength: Int = 0,
    var rsgListBeginOffset: Int = 0,
    var rsgNumber: Int = 0,
    var rsgInfoBeginOffset: Int = 0,
    var rsgInfoEachLength: Int = 204,
    var compositeNumber: Int = 0,
    var compositeInfoBeginOffset: Int = 0,
    var compositeInfoEachLength: Int = 1156,
    var compositeListLength: Int = 0,
    var compositeListBeginOffset: Int = 0,
    var autoPoolNumber: Int = 0,
    var autoPoolInfoBeginOffset: Int = 0,
    var autoPoolInfoEachLength: Int = 152,
    var ptxNumber: Int = 0,
    var ptxInfoBeginOffset: Int = 0,
    var ptxInfoEachLength: Int = 0,
    var part1BeginOffset: Int = 0,
    var part2BeginOffset: Int = 0,
    var part3BeginOffset: Int = 0
)

@Serializable
data class FileListInfo(
    var namePath: String,
    var poolIndex: Int
)

@Serializable
data class CompositeInfo(
    var name: String,
    var isComposite: Boolean,
    var packetNumber: Int,
    var packetInfo: MutableList<CompositePacketInfo>? = null
)

@Serializable
data class CompositePacketInfo(
    var packetIndex: Int,
    var category: MutableList<String>
)

@Serializable
data class RSGInfo(
    var name: String,
    var rsgOffset: Int,
    var rsgLength: Int,
    var poolIndex: Int,
    var ptxNumber: Int,
    var ptxBeforeNumber: Int,
    var packetHeadInfo: MutableList<Byte>? = null
)

@Serializable
data class AutoPoolInfo(
    var name: String,
    var part0Size: Int,
    var part1Size: Int
)

@Serializable
data class RSBPTXInfo(
    var ptxIndex: Int,
    var width: Int,
    var height: Int,
    var pitch: Int,
    var format: Int,
    var alphaSize: Int? = null,
    var alphaFormat: Int? = null
)

@Serializable
data class ResourcesDescription(
    var groups: MutableMap<String, DescriptionGroup>
)

@Serializable
data class DescriptionGroup(
    var composite: Boolean,
    var subgroups: MutableMap<String, DescriptionSubGroup>
)

@Serializable
data class DescriptionSubGroup(
    var res: String,
    var language: String,
    var resources: MutableMap<String, DescriptionResources>
)

@Serializable
data class DescriptionResources(
    var type: Int,
    var path: String,
    var ptxInfo: PropertiesPTXInfo? = null,
    var properties: MutableMap<String, String>
)

@Serializable
data class PropertiesPTXInfo(
    var imagetype: String,
    var aflags: String,
    var x: String,
    var y: String,
    var ax: String,
    var ay: String,
    var aw: String,
    var ah: String,
    var rows: String,
    var cols: String,
    var parent: String
)

@Serializable
data class CompositeResourcesDescriptionInfo(
    var id: String,
    var rsgNumber: Int,
    var rsgInfoList: MutableList<ResourcesRSGInfo>
)

@Serializable
data class ResourcesRSGInfo(
    var resolutionRatio: Int,
    var language: String,
    var id: String,
    var resourcesNumber: Int,
    var resourcesInfoList: MutableList<ResourcesInfo>
)

@Serializable
data class ResourcesInfo(
    var infoOffsetPart2: Int,
    var propertiesNumber: Int? = null,
    var id: String? = null,
    var path: String? = null,
    var ptxInfo: ResourcesPTXInfo? = null,
    var propertiesInfoList: MutableList<ResourcesPropertiesInfo>? = null
)

@Serializable
data class ResourcesPTXInfo(
    var imagetype: UShort,
    var aflags: UShort,
    var x: UShort,
    var y: UShort,
    var ax: UShort,
    var ay: UShort,
    var aw: UShort,
    var ah: UShort,
    var rows: UShort,
    var cols: UShort,
    var parent: String?
)

@Serializable
data class ResourcesPropertiesInfo(
    var key: String,
    var value: String
)

@Serializable
data class RSBPathTemp(
    var pathSlice: String,
    var key: Int,
    var poolIndex: Int,
    var positions: MutableList<PathPosition> = mutableListOf()
)
