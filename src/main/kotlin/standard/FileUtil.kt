package standard

import java.io.*

object FileUtil {
    fun readContent(filePath: String): String {
        return File(filePath).bufferedReader().readText()
    }

    fun writeContent(filePath: String, content: String) {
        File(filePath).apply {
            parentFile?.mkdirs()
            bufferedWriter().use {
                it.write(content)
                it.flush()
            }
        }
    }

    fun readBytes(filePath: String): ByteArray {
        return BufferedInputStream(FileInputStream(filePath)).use { it.readBytes() }
    }

    fun writeBytes(filePath: String, bytes: ByteArray) {
        File(filePath).parentFile?.mkdirs()
        BufferedOutputStream(FileOutputStream(filePath)).use {
            it.write(bytes)
            it.flush()
        }
    }

    fun removeQuotes(str: String): String {
        if (str.length >= 2 && str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length - 1)
        }
        return str
    }

    fun addQuotes(str: String): String {
        return "\"" + str + "\""
    }
}