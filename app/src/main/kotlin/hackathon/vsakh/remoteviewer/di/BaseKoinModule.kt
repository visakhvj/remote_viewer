package hackathon.vsakh.remoteviewer.di

import com.elvishew.xlog.XLog
import com.ironz.binaryprefs.BinaryPreferencesBuilder
import com.ironz.binaryprefs.Preferences
import hackathon.vsakh.remoteviewer.data.settings.Settings
import hackathon.vsakh.remoteviewer.data.settings.SettingsImpl
import hackathon.vsakh.remoteviewer.data.settings.SettingsReadOnly
import hackathon.vsakh.remoteviewer.service.helper.NotificationHelper
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.bind
import org.koin.dsl.module

val baseKoinModule = module {

    single<Preferences> {
        BinaryPreferencesBuilder(androidApplication())
            .supportInterProcess(true)
            .exceptionHandler { ex -> XLog.e(ex) }
            .build()
    }

    single<Settings> { SettingsImpl(get()) } bind SettingsReadOnly::class

    single { NotificationHelper(androidApplication()) }
}