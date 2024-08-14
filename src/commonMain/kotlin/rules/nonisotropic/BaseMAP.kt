package rules.nonisotropic

import rules.RuleFamily
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

abstract class BaseMAP: RuleFamily() {
    val mapRegex = "[A-Za-z0-9+/]*"

    @OptIn(ExperimentalEncodingApi::class)
    fun encodeTransition(transitions: BooleanArray) = Base64.encode(
        transitions.map { if (it) 1 else 0 }.joinToString("").chunked(8).map {
            it.toInt(2).toByte()
        }.toByteArray()
    )

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeTransition(base64: String) = Base64.decode(base64.encodeToByteArray()).map { byte ->
        (0..<8).map { (byte.toUInt() and ((1 shl (7 - it)).toUInt())) != 0u }
    }.flatten().toBooleanArray()
}