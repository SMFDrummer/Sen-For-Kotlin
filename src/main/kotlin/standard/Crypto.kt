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

@OptIn(ExperimentalUnsignedTypes::class)
fun encryptRTON(rtonData: UByteArray): UByteArray {
    return rijndaelEncrypt(
        rtonData, PaddedBufferedBlockCipher(
            CBCBlockCipher.newInstance(RijndaelEngine(192)), ZeroBytePadding()
        ), ParametersWithIV(KeyParameter(key), iv)
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
fun decryptRTON(rtonData: UByteArray): UByteArray {
    return rijndaelDecrypt(
        rtonData, PaddedBufferedBlockCipher(
            CBCBlockCipher.newInstance(RijndaelEngine(192)), ZeroBytePadding()
        ), ParametersWithIV(KeyParameter(key), iv)
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun rijndaelEncrypt(
    paramArrayOfbyte: UByteArray,
    paramPaddedBufferedBlockCipher: PaddedBufferedBlockCipher,
    paramParametersWithIV: ParametersWithIV
): UByteArray {
    try {
        paramPaddedBufferedBlockCipher.init(true, paramParametersWithIV)
        return cd(paramPaddedBufferedBlockCipher, paramArrayOfbyte)
    } catch (invalidCipherTextException: InvalidCipherTextException) {
        throw RuntimeException(invalidCipherTextException)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun rijndaelDecrypt(
    paramArrayOfbyte: UByteArray,
    paramPaddedBufferedBlockCipher: PaddedBufferedBlockCipher,
    paramParametersWithIV: ParametersWithIV?
): UByteArray {
    try {
        paramPaddedBufferedBlockCipher.init(false, paramParametersWithIV)
        return cd(paramPaddedBufferedBlockCipher, paramArrayOfbyte)
    } catch (invalidCipherTextException: InvalidCipherTextException) {
        throw RuntimeException(invalidCipherTextException)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Throws(InvalidCipherTextException::class)
private fun cd(paramPaddedBufferedBlockCipher: PaddedBufferedBlockCipher, paramArrayOfbyte: UByteArray): UByteArray {
    var i = paramPaddedBufferedBlockCipher.getOutputSize(paramArrayOfbyte.size)
    val arrayOfByte = ByteArray(i)
    i = paramPaddedBufferedBlockCipher.processBytes(
        paramArrayOfbyte.toByteArray(),
        0,
        paramArrayOfbyte.size,
        arrayOfByte,
        0
    )
    val j = paramPaddedBufferedBlockCipher.doFinal(arrayOfByte, i)
    return Arrays.copyOfRange(arrayOfByte, 0, i + j).toUByteArray()
}
