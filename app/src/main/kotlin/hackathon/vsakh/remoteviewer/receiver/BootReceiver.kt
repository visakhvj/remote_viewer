package hackathon.vsakh.remoteviewer.receiver


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.elvishew.xlog.XLog
import hackathon.vsakh.remoteviewer.data.other.getLog
import hackathon.vsakh.remoteviewer.data.settings.SettingsReadOnly
import hackathon.vsakh.remoteviewer.service.helper.IntentAction
import org.koin.core.KoinComponent
import org.koin.core.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val settingsReadOnly: SettingsReadOnly by inject()

    override fun onReceive(context: Context, intent: Intent) {
        XLog.d(getLog("onReceive", "Invoked"))

        if (settingsReadOnly.startOnBoot.not()) Runtime.getRuntime().exit(0)

        if (
            intent.action == "android.intent.action.BOOT_COMPLETED" ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            IntentAction.StartOnBoot.sendToAppService(context)
        }
    }
}