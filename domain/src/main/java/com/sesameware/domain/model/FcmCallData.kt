package com.sesameware.domain.model

import com.squareup.moshi.Json
import java.io.Serializable

data class FcmCallData(
    val server: String,
    val port: String,
    val transport: FcmTransport,
    val extension: String,
    var pass: String = "",
    val dtmf: String,
    var image: String = "",
    var live: String = "",
    val timestamp: String,
    val ttl: Int,
    val callerId: String,
    val flatId: Int,
    val flatNumber: String,
    val stun: String? = null,
    val stun_transport: String? = null,
    val hash: String? = null,
) : Serializable

enum class FcmTransport {
    @Json(name = "udp") Udp,
    @Json(name = "tcp") Tcp,
    @Json(name = "tls") Tls
}
