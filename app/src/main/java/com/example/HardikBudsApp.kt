package com.example

import android.app.Application
import com.example.data.audio.AudioEffectsEngine
import com.example.data.bluetooth.BluetoothBudsManager
import com.example.data.local.HardikBudsDatabase
import com.example.data.local.LocalBudsRepository
import com.example.data.local.PreferencesRepository
import com.example.data.update.AppUpdateRepository

class HardikBudsApp : Application() {
    lateinit var database: HardikBudsDatabase
        private set
    lateinit var localRepository: LocalBudsRepository
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var bluetoothManager: BluetoothBudsManager
        private set
    lateinit var audioEngine: AudioEffectsEngine
        private set
    lateinit var updateRepository: AppUpdateRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = HardikBudsDatabase.getInstance(this)
        localRepository = LocalBudsRepository(database.dao())
        preferencesRepository = PreferencesRepository(this)
        bluetoothManager = BluetoothBudsManager(this, localRepository)
        audioEngine = AudioEffectsEngine(this)
        updateRepository = AppUpdateRepository(this)
    }
}
