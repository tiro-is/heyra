package `is`.tiro.heyra

import android.net.Uri
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

fun createChannel(serverAddress: String): ManagedChannel {
    val serverUri = Uri.parse(serverAddress)
    return ManagedChannelBuilder.forAddress(serverUri.host, serverUri.port).run {
        if (serverUri.scheme == "grpcs") {
            useTransportSecurity()
        } else {
            usePlaintext()
        }
        build()
    }
}
