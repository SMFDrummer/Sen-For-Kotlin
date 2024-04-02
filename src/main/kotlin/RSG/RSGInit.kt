package RSG

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import standard.SenBuffer
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString

object RSGInit {
    fun unpack(rsgFilePathString: String) {
        val rsgFilePath = Paths.get(rsgFilePathString)
        val rsgBytes = SenBuffer(rsgFilePath)
        val bundlePath = Paths.get("$rsgFilePathString.bundle")
        val packetInfo = RSGFunction.unpack(rsgBytes, bundlePath.pathString)
        val packetInfoJSON =
            JSON.toJSONString(packetInfo, JSONWriter.Feature.WriteMapNullValue, JSONWriter.Feature.PrettyFormat)
        val writer = BufferedWriter(FileWriter(bundlePath.pathString + File.separator + "PacketInfo.json"))
        writer.write(packetInfoJSON)
        writer.flush()
        writer.close()
    }

    fun pack(bundlePathString: String) {
        val bundlePath = Paths.get(bundlePathString)
        val packetInfoJSONPath = Paths.get(bundlePath.pathString + File.separator + "PacketInfo.json")
        val packetInfo = JSON.parseObject(Files.readString(packetInfoJSONPath), PacketInfo::class.java)
        val rsgPathString =
            bundlePath.parent.toString() + File.separator + bundlePath.fileName.toString().replace(".bundle", ".rsg")
        val rsgBytes = RSGFunction.pack(bundlePath.pathString, packetInfo)
        val writer = BufferedOutputStream(FileOutputStream(rsgPathString))
        writer.write(rsgBytes.toBytes())
        writer.flush()
        writer.close()
    }
}