package standard

import RSB.RSBFunction
import RSG.RSGFunction
import RTON.RTONProcession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

object SenUtil {
    fun Path.trimBundle(): Path = Paths.get(
        this.parent.pathString,
        this.name.replace(".bundle", "")
    )

    fun Path.padBundle(): Path = Path(this.pathString + ".bundle")

    object RSB {
        fun pack(bundlePath: Path) = RSBFunction.pack(
            inFolder = bundlePath.pathString,
            outFile = bundlePath.trimBundle().pathString,
            manifestInfo = Json.decodeFromString(
                FileUtil.readContent(
                    Paths.get(
                        bundlePath.pathString,
                        "ManifestInfo.json"
                    ).pathString
                )
            )
        )

        fun pack(bundlePath: String) = pack(Path(bundlePath))

        fun unpack(rsbPath: Path) = FileUtil.writeContent(
            filePath = Paths.get(rsbPath.padBundle().pathString, "ManifestInfo.json").pathString,
            content = Json.by(JsonFeature.PrettyPrint, JsonFeature.ExplicitNulls).encodeToString(
                RSBFunction.unpack(
                    rsbFile = SenBuffer(rsbPath),
                    outFolder = rsbPath.padBundle().pathString
                )
            )
        )

        fun unpack(rsbPath: String) = unpack(Path(rsbPath))

        object SMF {
            fun compress(smfPath: Path) = Crypto.Zlib.Popcap.compress(SenBuffer(smfPath)).outFile(
                Paths.get(smfPath.pathString + ".smf")
            )

            fun compress(smfPath: String) = compress(Path(smfPath))

            fun decompress(smfPath: Path) = Crypto.Zlib.Popcap.decompress(SenBuffer(smfPath)).outFile(
                Paths.get(
                    smfPath.parent.pathString,
                    if (smfPath.name.endsWith(".rsb.smf")) smfPath.nameWithoutExtension else smfPath.name + ".rsb"
                )
            )

            fun decompress(smfPath: String) = decompress(Path(smfPath))
        }
    }

    object RSG {
        fun pack(bundlePath: Path) = RSGFunction.pack(
            inFolder = bundlePath.pathString,
            packetInfo = Json.decodeFromString(
                FileUtil.readContent(
                    Paths.get(
                        bundlePath.pathString,
                        "PacketInfo.json"
                    ).pathString
                )
            )
        )

        fun pack(bundlePath: String) = pack(Path(bundlePath))

        fun unpack(rsgPath: Path) = FileUtil.writeContent(
            filePath = Paths.get(rsgPath.padBundle().pathString, "PacketInfo.json").pathString,
            content = Json.by(JsonFeature.PrettyPrint, JsonFeature.ExplicitNulls).encodeToString(
                RSGFunction.unpack(
                    rsgFile = SenBuffer(rsgPath),
                    outFolder = rsgPath.padBundle().pathString
                )
            )
        )

        fun unpack(rsgPath: String) = unpack(Path(rsgPath))
    }

    object RTON {
        fun encode(jsonPath: Path) = RTONProcession.encode(FileUtil.readContent(jsonPath.pathString)).outFile(
            Paths.get(jsonPath.parent.pathString, jsonPath.nameWithoutExtension + ".rton")
        )

        fun encode(jsonPath: String) = encode(Path(jsonPath))

        fun decode(rtonPath: Path) = FileUtil.writeContent(
            filePath = Paths.get(rtonPath.parent.pathString, rtonPath.nameWithoutExtension + ".json").pathString,
            content = Json.by(JsonFeature.PrettyPrint, JsonFeature.ExplicitNulls).encodeToString(
                RTONProcession.decode(SenBuffer(rtonPath))
            )
        )

        fun decode(rtonPath: String) = decode(Path(rtonPath))

        fun encrypt(rtonPath: Path) = RTONProcession.encrypt(SenBuffer(rtonPath)).outFile(
            Paths.get(rtonPath.parent.pathString, rtonPath.nameWithoutExtension + ".cipher.rton")
        )

        fun encrypt(rtonPath: String) = encrypt(Path(rtonPath))

        fun decrypt(rtonPath: Path) = RTONProcession.decrypt(SenBuffer(rtonPath)).outFile(
            Paths.get(rtonPath.parent.pathString, rtonPath.nameWithoutExtension + ".plain.rton")
        )

        fun decrypt(rtonPath: String) = decrypt(Path(rtonPath))

        fun encodeWithEncrypt(jsonPath: Path) = RTONProcession.encrypt(
            RTONProcession.encode(FileUtil.readContent(jsonPath.pathString))
        ).outFile(Paths.get(jsonPath.parent.pathString, jsonPath.nameWithoutExtension + ".rton"))

        fun encodeWithEncrypt(jsonPath: String) = encodeWithEncrypt(Path(jsonPath))

        fun decodeWithDecrypt(rtonPath: Path) = FileUtil.writeContent(
            filePath = Paths.get(rtonPath.parent.pathString, rtonPath.nameWithoutExtension + ".json").pathString,
            content = Json.by(JsonFeature.PrettyPrint, JsonFeature.ExplicitNulls).encodeToString(
                RTONProcession.decode(
                    RTONProcession.decrypt(SenBuffer(rtonPath))
                )
            )
        )

        fun decodeWithDecrypt(rtonPath: String) = decodeWithDecrypt(Path(rtonPath))
    }
}