package standard

import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.engines.RijndaelEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.paddings.ZeroBytePadding
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.Arrays
import java.security.MessageDigest

val key = "65bd1b2305f46eb2806b935aab7630bb".toByteArray()
val iv = "1b2305f46eb2806b935aab76".toByteArray()

fun computeHash(data: String, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm).digest(data.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") {
        "%02x".format(it)
    }
}

fun encryptRTON(rtonData: ByteArray): ByteArray {
    return rijndaelEncrypt(
        rtonData, PaddedBufferedBlockCipher(
            CBCBlockCipher.newInstance(RijndaelEngine(192)), ZeroBytePadding()
        ), ParametersWithIV(KeyParameter(key), iv)
    )
}

fun decryptRTON(rtonData: ByteArray): ByteArray {
    return rijndaelDecrypt(
        rtonData, PaddedBufferedBlockCipher(
            CBCBlockCipher.newInstance(RijndaelEngine(192)), ZeroBytePadding()
        ), ParametersWithIV(KeyParameter(key), iv)
    )
}

private fun rijndaelEncrypt(
    paramArrayOfbyte: ByteArray,
    paramPaddedBufferedBlockCipher: PaddedBufferedBlockCipher,
    paramParametersWithIV: ParametersWithIV
): ByteArray {
    try {
        paramPaddedBufferedBlockCipher.init(true, paramParametersWithIV)
        return cd(paramPaddedBufferedBlockCipher, paramArrayOfbyte)
    } catch (invalidCipherTextException: InvalidCipherTextException) {
        throw RuntimeException(invalidCipherTextException)
    }
}

private fun rijndaelDecrypt(
    paramArrayOfbyte: ByteArray,
    paramPaddedBufferedBlockCipher: PaddedBufferedBlockCipher,
    paramParametersWithIV: ParametersWithIV?
): ByteArray {
    try {
        paramPaddedBufferedBlockCipher.init(false, paramParametersWithIV)
        return cd(paramPaddedBufferedBlockCipher, paramArrayOfbyte)
    } catch (invalidCipherTextException: InvalidCipherTextException) {
        throw RuntimeException(invalidCipherTextException)
    }
}

@Throws(InvalidCipherTextException::class)
private fun cd(paramPaddedBufferedBlockCipher: PaddedBufferedBlockCipher, paramArrayOfbyte: ByteArray): ByteArray {
    var i = paramPaddedBufferedBlockCipher.getOutputSize(paramArrayOfbyte.size)
    val arrayOfByte = ByteArray(i)
    i = paramPaddedBufferedBlockCipher.processBytes(
        paramArrayOfbyte,
        0,
        paramArrayOfbyte.size,
        arrayOfByte,
        0
    )
    val j = paramPaddedBufferedBlockCipher.doFinal(arrayOfByte, i)
    return Arrays.copyOfRange(arrayOfByte, 0, i + j)
}
