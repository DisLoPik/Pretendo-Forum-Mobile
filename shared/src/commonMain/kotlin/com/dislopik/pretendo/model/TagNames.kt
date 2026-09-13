package com.dislopik.pretendo.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads a topic's tags, whichever shape they arrive in.
 *
 * Stock Discourse sends plain names, `["outage", "mh4u"]`, but forum.pretendo.network
 * sends objects carrying an id and a slug as well. Either is read as a list of names,
 * because a name is all the app shows, and accepting both means the forum changing its
 * mind about this does not take the topic list down with it.
 */
object TagNamesSerializer : KSerializer<List<String>> {

    private val delegate = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val json = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val array = json.decodeJsonElement() as? JsonArray ?: return emptyList()
        return array.mapNotNull { entry ->
            when (entry) {
                is JsonPrimitive -> entry.contentOrNull
                is JsonObject -> entry["name"]?.jsonPrimitive?.contentOrNull
                else -> null
            }
        }.filter { it.isNotBlank() }
    }

    override fun serialize(encoder: Encoder, value: List<String>) =
        delegate.serialize(encoder, value)
}
