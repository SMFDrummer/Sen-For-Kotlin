package RTON

class RTONException(message: String, filePath: String) :
    RuntimeException(
        """
    $message
    @ FilePath: $filePath
""".trimIndent()
    )

class RTONDecodeException(message: String, errorCode: String, expected: String, exception: RTONListException) :
    RuntimeException(
        """
    $message
    @ ErrorCode: $errorCode
    @ Expected: $expected
    @ Exception: $exception
""".trimIndent()
    )

enum class RTONListException {
    Header,
    Version,
    Ends
}
