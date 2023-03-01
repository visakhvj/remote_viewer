package hackathon.vsakh.remoteviewer.service.helper

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.elvishew.xlog.XLog
import hackathon.vsakh.remoteviewer.TouchEventService
import hackathon.vsakh.remoteviewer.data.other.getLog
import hackathon.vsakh.remoteviewer.service.AppService
import hackathon.vsakh.remoteviewer.ui.activity.AppActivity
import kotlinx.android.parcel.Parcelize

sealed class IntentAction : Parcelable {
    internal companion object {
        private const val EXTRA_PARCELABLE = "EXTRA_PARCELABLE"
        fun fromIntent(intent: Intent?): IntentAction? = intent?.getParcelableExtra(EXTRA_PARCELABLE)
    }

    fun toAppServiceIntent(context: Context): Intent = AppService.getAppServiceIntent(context).apply {
        putExtra(EXTRA_PARCELABLE, this@IntentAction)
    }

    fun toAppActivityIntent(context: Context): Intent = AppActivity.getAppActivityIntent(context).apply {
        putExtra(EXTRA_PARCELABLE, this@IntentAction)
    }

    fun sendToAppService(context: Context) {
        XLog.i(context.getLog("sendToAppService", this.toString()))
        AppService.startForeground(context, this.toAppServiceIntent(context))
        //Start TouchDetech

        val intent = Intent(context, TouchEventService::class.java)
        context.startForegroundService(intent);

    }

    @Parcelize object GetServiceState : IntentAction()
    @Parcelize object StartStream : IntentAction()
    @Parcelize object StopStream : IntentAction()
    @Parcelize object Exit : IntentAction()
    @Parcelize data class CastIntent(val intent: Intent) : IntentAction()
    @Parcelize object CastPermissionsDenied : IntentAction()
    @Parcelize object StartOnBoot : IntentAction()
    @Parcelize object RecoverError : IntentAction()

    override fun toString(): String = javaClass.simpleName
}