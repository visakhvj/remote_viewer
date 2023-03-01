package hackathon.vsakh.remoteviewer.data.state

import android.content.Intent
import hackathon.vsakh.remoteviewer.data.model.AppError
import hackathon.vsakh.remoteviewer.data.model.HttpClient
import hackathon.vsakh.remoteviewer.data.model.NetInterface
import hackathon.vsakh.remoteviewer.data.model.TrafficPoint
import kotlinx.coroutines.flow.StateFlow


interface AppStateMachine {

    open class Event {
        object StartStream : Event()
        object CastPermissionsDenied : Event()
        class StartProjection(val intent: Intent) : Event()
        object StopStream : Event()
        object RequestPublicState : Event()
        object RecoverError : Event()

        override fun toString(): String = javaClass.simpleName
    }

    sealed class Effect {
        object ConnectionChanged : Effect()
        data class PublicState(
            val isStreaming: Boolean,
            val isBusy: Boolean,
            val isWaitingForPermission: Boolean,
            val netInterfaces: List<NetInterface>,
            val appError: AppError?
        ) : Effect()
    }

    val statisticFlow: StateFlow<Pair<List<HttpClient>, List<TrafficPoint>>>

    fun sendEvent(event: Event, timeout: Long = 0)

    suspend fun destroy()
}