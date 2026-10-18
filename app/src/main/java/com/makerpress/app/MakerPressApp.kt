package com.makerpress.app

import android.app.Application
import com.makerpress.app.core.bluetooth.BluetoothPrinterManager
import com.makerpress.app.core.printing.*
import com.makerpress.app.core.rendering.*
import com.makerpress.app.data.*

class MakerPressApp : Application() {
    val database by lazy { MakerDatabase.open(this) }
    val settings by lazy { SettingsRepository(this) }
    val jobs by lazy { JobRepository(database.jobs()) }
    val printer by lazy { BluetoothPrinterManager(this) }
    val renderer by lazy { PrintDocumentRenderer(this) }
    val queue by lazy { PrintQueueManager(jobs, renderer, EscPosPrintEngine(printer.transport), printer.transport, printer::log) }
}
