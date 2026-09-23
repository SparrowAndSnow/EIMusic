package com.eimsound.eimusic.serialization

import kotlinx.serialization.protobuf.ProtoBuf

val ProtoBuf = ProtoBuf {
    serializersModule = module
}