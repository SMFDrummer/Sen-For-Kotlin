package standard

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonObject

enum class JsonFeature {
    PrettyPrint,
    ExplicitNulls,
    IgnoreUnknownKeys,
    AllowTrailingComma
}

@OptIn(ExperimentalSerializationApi::class)
private fun JsonBuilder.config(vararg features: JsonFeature): JsonBuilder {
    features.forEach {
        when (it) {
            JsonFeature.PrettyPrint -> this.prettyPrint = true
            JsonFeature.ExplicitNulls -> this.explicitNulls = true
            JsonFeature.IgnoreUnknownKeys -> this.ignoreUnknownKeys = true
            JsonFeature.AllowTrailingComma -> this.allowTrailingComma = true
        }
    }
    return this
}

fun Json.by(vararg features: JsonFeature): Json {
    return Json(builderAction = { config(*features) })
}

fun Json.isObject(data: String): Boolean {
    return try {
        Json.parseToJsonElement(data) is JsonObject
    } catch (_: Exception) {
        false
    }
}

fun Json.isArray(data: String): Boolean {
    return try {
        Json.parseToJsonElement(data) is JsonArray
    } catch (_: Exception) {
        false
    }
}
