package RSG

data class PacketInfo(
    var version: Int,
    var compressionFlags: Int,
    var res: MutableList<ResInfo>
)

data class ResInfo(
    var path: String,
    var ptxInfo: PTXInfo? = null
)

data class PTXInfo(
    var id: Int,
    var width: Int,
    var height: Int
)

data class RSGHead(
    val header: String = "pgsr",
    var version: Int = 0,
    var flags: Int = 0,
    var fileOffset: Int = 0,
    var part0Offset: Int = 0,
    var part0Zlib: Int = 0,
    var part0Size: Int = 0,
    var part1Offset: Int = 0,
    var part1Zlib: Int = 0,
    var part1Size: Int = 0,
    var fileListLength: Int = 0,
    var fileListOffset: Int = 0
)

data class NameDict(
    var namePath: String,
    var offsetByte: Int
)

data class PathTemp(
    var pathSlice: String,
    var key: Int,
    var resInfo: ResInfo,
    var isAtlas: Boolean,
    var positions: MutableList<PathPosition> = mutableListOf()
)

data class PathPosition(
    var position: Int,
    var offset: Int
)