package com.mahasiswa.sigma.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.mahasiswa.sigma.DisasterReportsData
import java.io.InputStream
import java.io.OutputStream

object DisasterReportsSerializer : Serializer<DisasterReportsData> {
    override val defaultValue: DisasterReportsData = DisasterReportsData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): DisasterReportsData {
        try {
            return DisasterReportsData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: DisasterReportsData, output: OutputStream) {
        t.writeTo(output)
    }
}
