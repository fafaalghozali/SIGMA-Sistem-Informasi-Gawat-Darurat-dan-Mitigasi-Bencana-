package com.mahasiswa.sigma.data.remote.api

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Custom Gson TypeAdapter for List<String> fields that may arrive from Supabase
 * in multiple formats:
 * - JSON array: ["Sembako", "Air Mineral", "Selimut"]
 * - Comma-separated string: "Sembako, Air Mineral, Selimut"
 * - PostgreSQL text array literal: {Sembako,"Air Mineral",Selimut}
 * - null
 *
 * This ensures robust deserialization regardless of the column type in Supabase
 * (text, text[], jsonb).
 */
class StringListTypeAdapter : TypeAdapter<List<String>>() {

    override fun write(out: JsonWriter, value: List<String>?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginArray()
        for (item in value) {
            out.value(item)
        }
        out.endArray()
    }

    override fun read(reader: JsonReader): List<String> {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                emptyList()
            }
            JsonToken.BEGIN_ARRAY -> {
                val list = mutableListOf<String>()
                reader.beginArray()
                while (reader.hasNext()) {
                    when (reader.peek()) {
                        JsonToken.STRING -> list.add(reader.nextString())
                        JsonToken.NULL -> { reader.nextNull() }
                        else -> reader.skipValue()
                    }
                }
                reader.endArray()
                list
            }
            JsonToken.STRING -> {
                val raw = reader.nextString()
                parseStringToList(raw)
            }
            else -> {
                reader.skipValue()
                emptyList()
            }
        }
    }

    private fun parseStringToList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        // Handle PostgreSQL array literal format: {item1,"item 2",item3}
        if (raw.startsWith("{") && raw.endsWith("}")) {
            val inner = raw.substring(1, raw.length - 1)
            return parseCommaSeparated(inner)
        }

        // Handle comma-separated values
        return raw.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }
    }

    private fun parseCommaSeparated(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in input) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim().removeSurrounding("\""))
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        val last = current.toString().trim().removeSurrounding("\"")
        if (last.isNotBlank()) result.add(last)
        return result
    }
}
