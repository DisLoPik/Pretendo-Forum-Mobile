package com.dislopik.pretendo.data

/**
 * The small amount of state the app keeps on the phone: the forum API key, and every
 * accessibility preference.
 *
 * Accessibility settings live here rather than on the forum account because Discourse has
 * no place to put them, so they are per-device by design.
 */
expect class KeyValueStore() {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

fun KeyValueStore.getBoolean(key: String, default: Boolean): Boolean =
    getString(key)?.toBooleanStrictOrNull() ?: default

fun KeyValueStore.putBoolean(key: String, value: Boolean) = putString(key, value.toString())

fun KeyValueStore.getFloat(key: String, default: Float): Float =
    getString(key)?.toFloatOrNull() ?: default

fun KeyValueStore.putFloat(key: String, value: Float) = putString(key, value.toString())

fun KeyValueStore.getInt(key: String, default: Int): Int =
    getString(key)?.toIntOrNull() ?: default

/**
 * Reads an enum by name, falling back to [default] when the stored value belongs to an
 * older version of the app that named its options differently.
 */
inline fun <reified T : Enum<T>> KeyValueStore.getEnum(key: String, default: T): T {
    val stored = getString(key) ?: return default
    return enumValues<T>().firstOrNull { it.name == stored } ?: default
}

fun <T : Enum<T>> KeyValueStore.putEnum(key: String, value: T) = putString(key, value.name)
