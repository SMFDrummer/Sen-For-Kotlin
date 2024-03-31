package RSB

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import standard.SenBuffer
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString

object RSBInit {
    fun unpack(rsbFilePathString: String) {
        val rsbFilePath = Paths.get(rsbFilePathString)
        val rsbBytes = SenBuffer(rsbFilePath)
        val bundlePath = Paths.get("$rsbFilePathString.bundle")
        val manifestInfo = RSBFunction.unpack(rsbBytes, bundlePath.pathString)
        val manifestInfoJSON =
            JSON.toJSONString(manifestInfo, JSONWriter.Feature.WriteMapNullValue, JSONWriter.Feature.PrettyFormat)
        val writer = BufferedWriter(FileWriter(bundlePath.pathString + File.separator + "ManifestInfo.json"))
        writer.write(manifestInfoJSON)
        writer.flush()
        writer.close()
    }

    fun pack(bundlePathString: String) {
        val bundlePath = Paths.get(bundlePathString)
        val manifestInfoJSONPath = Paths.get(bundlePath.pathString + File.separator + "ManifestInfo.json")
        val manifestInfo = JSON.parseObject(Files.readString(manifestInfoJSONPath), ManifestInfo::class.java)
        val rsgPathString =
            bundlePath.parent.toString() + File.separator + bundlePath.fileName.toString().replace(".bundle", ".rsb")
        RSBFunction.pack(bundlePath.pathString, rsgPathString, manifestInfo)
    }
}