package com.mahasiswa.sigma.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.mahasiswa.sigma.VolunteerData
import java.io.InputStream
import java.io.OutputStream

object VolunteerDataSerializer : Serializer<VolunteerData> {
    override val defaultValue: VolunteerData = VolunteerData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): VolunteerData {
        try {
            return VolunteerData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: VolunteerData, output: OutputStream) {
        t.writeTo(output)
    }
}
