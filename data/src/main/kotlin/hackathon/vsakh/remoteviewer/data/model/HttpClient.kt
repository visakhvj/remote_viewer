package hackathon.vsakh.remoteviewer.data.model

data class HttpClient(
    val id: Long,
    val clientAddressAndPort: String,
    val isSlowConnection: Boolean,
    val isDisconnected: Boolean
)