package com.mahasiswa.sigma.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Custom Gson deserializer for VolunteerReportDto.
 *
 * The `report_data` column in Supabase is a `jsonb` type, which means the API
 * returns it as an actual JSON object (not a string). However, the app expects
 * it as a String? so it can be parsed by VolunteerReportParser.
 *
 * This deserializer handles both cases:
 * - If report_data is a string → use as-is
 * - If report_data is a JSON object → convert to its string representation
 */
class VolunteerReportDtoDeserializer : JsonDeserializer<VolunteerReportDto> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): VolunteerReportDto {
        val obj = json.asJsonObject

        val reportData = when {
            !obj.has("report_data") || obj.get("report_data").isJsonNull -> null
            obj.get("report_data").isJsonPrimitive -> obj.get("report_data").asString
            else -> obj.get("report_data").toString() // JSON object → stringify
        }

        return VolunteerReportDto(
            id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString,
            volunteerId = obj.get("volunteer_id")?.takeIf { !it.isJsonNull }?.asString,
            disasterId = obj.get("disaster_id")?.takeIf { !it.isJsonNull }?.asString,
            skillType = obj.get("skill_type")?.takeIf { !it.isJsonNull }?.asString,
            reportData = reportData,
            notes = obj.get("notes")?.takeIf { !it.isJsonNull }?.asString,
            photoUrls = if (obj.has("photo_urls") && obj.get("photo_urls").isJsonArray) {
                obj.getAsJsonArray("photo_urls").map { it.asString }
            } else null,
            createdAt = obj.get("created_at")?.takeIf { !it.isJsonNull }?.asString
        )
    }
}
