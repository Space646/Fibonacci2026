package com.fibonacci.fibohealth.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable(with = FoodLogEntrySerializer::class)
data class FoodLogEntry(
    val id: Int,
    val foodName: String,
    val weightG: Float,
    val calories: Float,
    val isHealthy: Boolean,
    val timestamp: String,
    val proteinG: Float? = null,
    val fatG: Float? = null,
    val sugarG: Float? = null,
    val fiberG: Float? = null
)

// Custom serializer handles is_healthy as Int or Boolean
object FoodLogEntrySerializer : kotlinx.serialization.KSerializer<FoodLogEntry> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("FoodLogEntry")

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: FoodLogEntry) {
        // Write-path not needed (Pi → phone only)
        throw UnsupportedOperationException()
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): FoodLogEntry {
        val obj = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        val isHealthy = obj["is_healthy"]?.let {
            when {
                it is JsonPrimitive && it.isString -> it.content == "true"
                it is JsonPrimitive -> it.intOrNull?.let { n -> n != 0 } ?: it.booleanOrNull ?: false
                else -> false
            }
        } ?: false
        return FoodLogEntry(
            id        = obj["id"]!!.jsonPrimitive.int,
            foodName  = obj["food_name"]!!.jsonPrimitive.content,
            weightG   = obj["weight_g"]!!.jsonPrimitive.float,
            calories  = obj["calories"]!!.jsonPrimitive.float,
            isHealthy = isHealthy,
            timestamp = obj["timestamp"]!!.jsonPrimitive.content,
            proteinG  = obj["protein_g"]?.jsonPrimitive?.floatOrNull,
            fatG      = obj["fat_g"]?.jsonPrimitive?.floatOrNull,
            sugarG    = obj["sugar_g"]?.jsonPrimitive?.floatOrNull,
            fiberG    = obj["fiber_g"]?.jsonPrimitive?.floatOrNull
        )
    }
}
