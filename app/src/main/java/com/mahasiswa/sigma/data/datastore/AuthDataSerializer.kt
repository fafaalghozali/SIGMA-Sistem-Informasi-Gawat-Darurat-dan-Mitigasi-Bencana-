package com.mahasiswa.sigma.data.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.mahasiswa.sigma.AuthData
import java.io.InputStream
import java.io.OutputStream

object AuthDataSerializer : Serializer<AuthData> {
    override val defaultValue: AuthData = AuthData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AuthData {
        try {
            return AuthData.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: AuthData, output: OutputStream) {
        t.writeTo(output)
    }
}
