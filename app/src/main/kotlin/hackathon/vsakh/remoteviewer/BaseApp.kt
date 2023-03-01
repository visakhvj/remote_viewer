package hackathon.vsakh.remoteviewer

import android.app.Application
import com.elvishew.xlog.flattener.ClassicFlattener
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import hackathon.vsakh.remoteviewer.data.settings.SettingsReadOnly
import hackathon.vsakh.remoteviewer.di.baseKoinModule
import hackathon.vsakh.remoteviewer.logging.DateSuffixFileNameGenerator
import hackathon.vsakh.remoteviewer.logging.getLogFolder
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

abstract class BaseApp : Application() {

    protected val settingsReadOnly: SettingsReadOnly by inject()
    protected val filePrinter: FilePrinter by lazy {
        FilePrinter.Builder(getLogFolder())
            .fileNameGenerator(DateSuffixFileNameGenerator(this@BaseApp.hashCode().toString()))
            .cleanStrategy(FileLastModifiedCleanStrategy(86400000)) // One day
            .flattener(ClassicFlattener())
            .build()
    }

    abstract fun initLogger()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BaseApp)
            modules(baseKoinModule)
        }

        initLogger()
    }
}