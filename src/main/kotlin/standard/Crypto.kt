package standard

import org.bouncycastle.crypto.engines.RijndaelEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.paddings.ZeroBytePadding
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater

object Crypto {
    object Hash {
        fun getMD5(data: String): String {
            val messageDigest = MessageDigest.getInstance("MD5")
            messageDigest.update(data.toByteArray())
            val digest = messageDigest.digest()
            return Hex.toHexString(digest)
        }
    }

    class RTON(cipherKey: String = "com_popcap_pvz2_magento_product_2013_05_05") {
        val key = Hash.getMD5(cipherKey)
        val iv = key.substring(4, key.length - 4)

        @Throws(Exception::class)
        private fun rijndael(data: ByteArray, key: ByteArray, iv: ByteArray, isEncryptor: Boolean): ByteArray {
            val rijndael = RijndaelEngine(192)
            val zeroBytePadding = ZeroBytePadding()
            val parametersWithIV = ParametersWithIV(KeyParameter(key), iv)
            val cipher = PaddedBufferedBlockCipher(CBCBlockCipher.newInstance(rijndael), zeroBytePadding)
            cipher.init(isEncryptor, parametersWithIV)
            return cipher(cipher, data)
        }

        @Throws(Exception::class)
        private fun cipher(paddedBufferedBlockCipher: PaddedBufferedBlockCipher, bytes: ByteArray): ByteArray {
            val a = ByteArray(paddedBufferedBlockCipher.getOutputSize(bytes.size))
            val processBytes = paddedBufferedBlockCipher.processBytes(bytes, 0, bytes.size, a, 0)
            val doFinal = processBytes + paddedBufferedBlockCipher.doFinal(a, processBytes)
            val En = ByteArray(doFinal)
            System.arraycopy(a, 0, En, 0, doFinal)
            return En
        }

        fun encrypt(data: ByteArray): ByteArray = rijndael(
            data = data,
            key = key.toByteArray(),
            iv = iv.toByteArray(),
            isEncryptor = true
        )

        fun decrypt(data: ByteArray): ByteArray = rijndael(
            data = data,
            key = key.toByteArray(),
            iv = iv.toByteArray(),
            isEncryptor = false
        )
    }

    class Zlib(val deflateLevel: Int = Deflater.DEFAULT_COMPRESSION) {
        fun compress(data: ByteArray): ByteArray {
            val deflater = Deflater()
            deflater.setLevel(deflateLevel)
            deflater.setInput(data)
            deflater.finish()

            val outputStream = ByteArrayOutputStream(data.size)

            val buffer = ByteArray(1024)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                outputStream.write(buffer, 0, count)
            }

            outputStream.close()
            deflater.end()

            return outputStream.toByteArray()
        }

        fun decompress(data: ByteArray): ByteArray {
            val inflater = Inflater()
            inflater.setInput(data)

            val outputStream = ByteArrayOutputStream(data.size)

            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }

            outputStream.close()
            inflater.end()

            return outputStream.toByteArray()
        }

        object Popcap {
            fun compress(data: SenBuffer): SenBuffer {
                val result = SenBuffer()
                result.writeBytes(byteArrayOf(0xD4.toByte(), 0xFE.toByte(), 0xAD.toByte(), 0xDE.toByte()))
                result.writeUInt32LE(data.length.toUInt())
                result.writeBytes(Zlib(Deflater.BEST_COMPRESSION).compress(data.toBytes()))
                return result
            }

            fun decompress(data: SenBuffer): SenBuffer {
                val header = data.readUInt32LE()
                if (header != 0xDEADFED4.toUInt()) throw Exception("Mismatch PopCap Zlib header: $header")
                data.readUInt32LE()
                return SenBuffer(Zlib().decompress(data.readBytes((data.length - 8L).toInt())))
            }
        }
    }
}