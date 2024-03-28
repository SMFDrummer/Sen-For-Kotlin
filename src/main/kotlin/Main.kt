import RTON.RTONProcession
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import standard.SenBuffer
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString

fun main() {

}

fun dRTON(rtonFilePathString: String) {
    val rtonFilePath = Paths.get(rtonFilePathString)
    val rtonTmpFilePath = Paths.get(rtonFilePathString + "tmp")
    Files.deleteIfExists(rtonTmpFilePath)
    Files.copy(rtonFilePath, rtonTmpFilePath)
    var rtonBytes = SenBuffer(rtonTmpFilePath.pathString)
    rtonBytes = RTONProcession.decrypt(rtonBytes) //Annotate it if you don't want to encrypt/decrypt it
    val rtonFileName = rtonTmpFilePath.fileName.toString()
    val fileParentPath = rtonTmpFilePath.parent.toString()
    val jsonFileName = rtonFileName.substring(0, rtonFileName.lastIndexOf('.')) + ".JSON"
    val jsonString =
        RTONProcession.decode(rtonBytes, JSONWriter.Feature.WriteMapNullValue, JSONWriter.Feature.PrettyFormat)
    val fileOutputStream = FileOutputStream(fileParentPath + File.separator + jsonFileName)
    fileOutputStream.write(jsonString.toByteArray())
    fileOutputStream.close()
    Files.delete(rtonTmpFilePath)
}

fun eRTON(jsonFilePathString: String) {
    val jsonFilePath = Paths.get(jsonFilePathString)
    val jsonFileName = jsonFilePath.fileName.toString()
    val fileParentPath = jsonFilePath.parent.toString()
    val rtonFileName = jsonFileName.substring(0, jsonFileName.lastIndexOf('.')) + ".RTON"
    val jsonObject = JSON.parseObject(Files.readAllBytes(jsonFilePath))
    var rtonBytes = RTONProcession.encode(jsonObject.toJSONString(JSONWriter.Feature.WriteMapNullValue))
    rtonBytes = RTONProcession.encrypt(rtonBytes) //Annotate it if you don't want to encrypt/decrypt it
    val fileOutputStream = FileOutputStream(fileParentPath + File.separator + rtonFileName)
    fileOutputStream.write(rtonBytes.toBytes())
    fileOutputStream.close()
}