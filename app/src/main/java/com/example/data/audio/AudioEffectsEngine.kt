package com.example.data.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Spatializer
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.domain.model.AudioEngineStatus
import com.example.domain.model.HomeTheatreState
import com.example.domain.model.ReferenceToneMode
import com.example.domain.model.SoundStudioState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class AudioEffectsEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var legacyEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var currentSessionId: Int = 0
    private var lastSoundState: SoundStudioState = SoundStudioState()
    private var lastTheatreState: HomeTheatreState = HomeTheatreState()

    private var audioTrack: AudioTrack? = null
    private var toneJob: Job? = null
    private var currentToneMode: ReferenceToneMode = ReferenceToneMode.AMBIENT_CHORD

    private val _engineStatus = MutableStateFlow(AudioEngineStatus())
    val engineStatus: StateFlow<AudioEngineStatus> = _engineStatus.asStateFlow()

    private val _spectrumBins = MutableStateFlow(List(24) { 0.05f })
    val spectrumBins: StateFlow<List<Float>> = _spectrumBins.asStateFlow()

    private val _volumeSnapshot = MutableStateFlow(readVolumeSnapshot())
    val volumeSnapshot: StateFlow<VolumeSnapshot> = _volumeSnapshot.asStateFlow()

    private var spatializerListenerHandle: Any? = null
    private var headTrackerListenerHandle: Any? = null
    private val spatialExecutor = Executors.newSingleThreadExecutor()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                _volumeSnapshot.value = readVolumeSnapshot()
            }
        }
    }

    data class VolumeSnapshot(
        val currentStep: Int,
        val maxStep: Int,
        val ratio: Float,
    )

    init {
        registerVolumeReceiver()
        setupSpatializerMonitoring()
        attachEffectsToSession(0)
    }

    private fun registerVolumeReceiver() {
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(volumeReceiver, filter)
        }
    }

    private fun setupSpatializerMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33SpatialHelper.setup(
                audioManager = audioManager,
                executor = spatialExecutor,
                onUpdate = { available, enabled, headTracker, levelLabel ->
                    _engineStatus.update {
                        it.copy(
                            isSystemSpatializerAvailable = available,
                            isSystemSpatializerEnabled = enabled,
                            isHeadTrackerAvailable = headTracker,
                            immersiveAudioLevelLabel = levelLabel,
                        )
                    }
                },
                onStoreHandles = { stateL, headL ->
                    spatializerListenerHandle = stateL
                    headTrackerListenerHandle = headL
                },
            )
        } else {
            _engineStatus.update {
                it.copy(
                    isSystemSpatializerAvailable = false,
                    isSystemSpatializerEnabled = false,
                    isHeadTrackerAvailable = false,
                    immersiveAudioLevelLabel = "Android 13+ required for OS Spatializer",
                )
            }
        }
    }

    fun readVolumeSnapshot(): VolumeSnapshot {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, max)
        return VolumeSnapshot(cur, max, cur.toFloat() / max.toFloat())
    }

    fun setMediaVolumeRatio(ratio: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (ratio.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
        _volumeSnapshot.value = readVolumeSnapshot()
    }

    fun openSystemSoundSettings() {
        val intent = Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun toggleReferenceTone(mode: ReferenceToneMode? = null) {
        if (mode != null) currentToneMode = mode
        if (_engineStatus.value.isReferenceTonePlaying) {
            stopReferenceTone()
        } else {
            startReferenceTone(currentToneMode)
        }
    }

    fun setReferenceToneMode(mode: ReferenceToneMode) {
        currentToneMode = mode
        _engineStatus.update { it.copy(referenceTrackMode = mode) }
    }

    private fun startReferenceTone(mode: ReferenceToneMode) {
        stopReferenceTone()
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBuf * 2)
                .build()
        }.getOrNull() ?: return

        audioTrack = track
        val sessionId = track.audioSessionId
        attachEffectsToSession(sessionId)
        track.play()

        _engineStatus.update {
            it.copy(
                isReferenceTonePlaying = true,
                referenceTrackMode = mode,
                sessionScope = "Session #$sessionId (Acoustic Audition)",
            )
        }

        toneJob = scope.launch(Dispatchers.Default) {
            val frames = 1024
            val pcm = ShortArray(frames * 2)
            var phase = 0.0
            var pulsePhase = 0.0
            while (isActive) {
                val activeMode = currentToneMode
                val freqs = when (activeMode) {
                    ReferenceToneMode.AMBIENT_CHORD -> doubleArrayOf(110.0, 220.0, 329.63, 440.0, 659.25, 1318.5)
                    ReferenceToneMode.SUB_BASS_CINEMA -> doubleArrayOf(45.0, 62.5, 90.0, 180.0, 360.0)
                    ReferenceToneMode.VOCAL_PRESENCE -> doubleArrayOf(220.0, 440.0, 880.0, 1760.0, 3520.0)
                }
                val lGain = (1f - max(0f, lastSoundState.balance)).coerceIn(0.1f, 1f)
                val rGain = (1f + min(0f, lastSoundState.balance)).coerceIn(0.1f, 1f)

                for (i in 0 until frames) {
                    val env = 0.7 + 0.3 * sin(pulsePhase)
                    var sample = 0.0
                    for (idx in freqs.indices) {
                        val f = freqs[idx]
                        val weight = 1.0 / (idx + 1.5)
                        sample += sin(phase * f * 2.0 * PI) * weight
                    }
                    val normalized = (sample * 0.24 * env).coerceIn(-0.95, 0.95)
                    pcm[i * 2] = (normalized * lGain * Short.MAX_VALUE).toInt().toShort()
                    pcm[i * 2 + 1] = (normalized * rGain * Short.MAX_VALUE).toInt().toShort()
                    phase += 1.0 / sampleRate
                    pulsePhase += (2.0 * PI * 0.65) / sampleRate
                }
                runCatching { track.write(pcm, 0, pcm.size) }

                // Update real-time 24-bin acoustic visualizer reflecting active DSP shaping
                val eq = lastSoundState.eqBandsDb
                val spatialEnergy = lastTheatreState.spatialMode.virtualizerStrength / 1000f
                val subBoost = lastTheatreState.virtualSubwoofer * 0.35f + lastSoundState.bassBoost * 0.35f
                val bins = List(24) { b ->
                    val eqIdx = ((b / 24f) * 9f).toInt().coerceIn(0, 9)
                    val eqFactor = ((eq[eqIdx] + 12f) / 24f).coerceIn(0.1f, 1f)
                    val bassWeight = if (b < 6) subBoost else 0f
                    val wave = abs(sin(pulsePhase * 1.4 + b * 0.45)).toFloat()
                    (0.12f + eqFactor * 0.45f + wave * 0.25f + bassWeight + spatialEnergy * 0.14f)
                        .coerceIn(0.06f, 0.98f)
                }
                _spectrumBins.value = bins
            }
        }
    }

    fun stopReferenceTone() {
        toneJob?.cancel()
        toneJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        _spectrumBins.value = List(24) { 0.05f }
        _engineStatus.update {
            it.copy(
                isReferenceTonePlaying = false,
                sessionScope = "Session 0 (Global Mix)",
            )
        }
        attachEffectsToSession(0)
    }

    private fun attachEffectsToSession(sessionId: Int) {
        releaseEffectsOnly()
        currentSessionId = sessionId
        var dpActive = false
        var eqActive = false
        var errorMsg: String? = null
        var bandCount = 10

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val dpResult = runCatching {
                Api28DynamicsHelper.create(sessionId)
            }
            if (dpResult.isSuccess) {
                dynamicsProcessing = dpResult.getOrNull()
                dynamicsProcessing?.enabled = !lastSoundState.masterBypass
                dpActive = dynamicsProcessing != null
            } else {
                errorMsg = dpResult.exceptionOrNull()?.localizedMessage
            }
        }

        if (!dpActive) {
            val eqResult = runCatching {
                Equalizer(0, sessionId).apply { enabled = !lastSoundState.masterBypass }
            }
            if (eqResult.isSuccess) {
                legacyEqualizer = eqResult.getOrNull()
                eqActive = legacyEqualizer != null
                bandCount = legacyEqualizer?.numberOfBands?.toInt() ?: 5
            }
            bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
            loudnessEnhancer = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        }

        virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()
        presetReverb = runCatching { PresetReverb(0, sessionId) }.getOrNull()

        val engineName = when {
            dpActive -> "DynamicsProcessing 10-Band + MBC (API ${Build.VERSION.SDK_INT})"
            eqActive -> "Android Equalizer ($bandCount-Band) + BassBoost"
            else -> "AudioEffects unavailable on current session"
        }

        _engineStatus.update {
            it.copy(
                engineName = engineName,
                sessionScope = if (sessionId == 0) "Session 0 (Global Mix)" else "Session #$sessionId (Acoustic Audition)",
                bandCount = bandCount,
                isDynamicsProcessingActive = dpActive,
                isLegacyEqActive = eqActive,
                isVirtualizerSupported = virtualizer != null,
                isReverbSupported = presetReverb != null,
                lastError = errorMsg,
            )
        }

        applyProcessing(lastSoundState, lastTheatreState)
    }

    fun applyProcessing(sound: SoundStudioState, theatre: HomeTheatreState) {
        lastSoundState = sound
        lastTheatreState = theatre

        val bypassed = sound.masterBypass
        val combinedBands = computeCombined10BandGain(sound, theatre)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing?.let { dp ->
                runCatching {
                    dp.enabled = !bypassed
                    if (!bypassed) {
                        Api28DynamicsHelper.apply(
                            dp = dp,
                            bandsDb = combinedBands,
                            balance = sound.balance,
                            clarity = sound.clarityEnhancer,
                            vocal = max(sound.vocalEnhancer, theatre.dialogueEnhancement),
                            subwoofer = if (theatre.cinemaModeEnabled) theatre.virtualSubwoofer else 0f,
                        )
                    }
                }
            }
        }

        legacyEqualizer?.let { eq ->
            runCatching {
                eq.enabled = !bypassed
                if (!bypassed) {
                    val range = eq.bandLevelRange
                    val minMb = range[0].toInt()
                    val maxMb = range[1].toInt()
                    val numBands = eq.numberOfBands.toInt()
                    for (b in 0 until numBands) {
                        val mappedIdx = ((b.toFloat() / max(1, numBands - 1)) * 9f).toInt().coerceIn(0, 9)
                        val db = combinedBands[mappedIdx]
                        val mb = (db * 100f).toInt().coerceIn(minMb, maxMb)
                        eq.setBandLevel(b.toShort(), mb.toShort())
                    }
                }
            }
        }

        bassBoost?.let { bb ->
            runCatching {
                val strength = ((sound.bassBoost * 0.7f + if (theatre.cinemaModeEnabled) theatre.deepBass * 0.3f else 0f) * 1000f)
                    .toInt()
                    .coerceIn(0, 1000)
                bb.enabled = !bypassed && strength > 0
                if (bb.strengthSupported && strength > 0) {
                    bb.setStrength(strength.toShort())
                }
            }
        }

        loudnessEnhancer?.let { le ->
            runCatching {
                val gainMb = ((sound.vocalEnhancer * 450f + sound.clarityEnhancer * 350f)).toInt()
                le.enabled = !bypassed && gainMb > 0
                if (gainMb > 0) le.setTargetGain(gainMb)
            }
        }

        virtualizer?.let { virt ->
            runCatching {
                val baseVirt = theatre.spatialMode.virtualizerStrength
                val widthBoost = if (theatre.cinemaModeEnabled) (theatre.stereoWidening * 250f).toInt() else 0
                val totalStrength = max(baseVirt, widthBoost).coerceIn(0, 1000)
                virt.enabled = !bypassed && totalStrength > 0
                if (virt.strengthSupported && totalStrength > 0) {
                    virt.setStrength(totalStrength.toShort())
                }
            }
        }

        presetReverb?.let { rev ->
            runCatching {
                val preset = theatre.roomSimulation.presetId
                rev.enabled = !bypassed && preset > 0
                if (preset > 0) {
                    rev.preset = preset
                }
            }
        }

        if (!_engineStatus.value.isReferenceTonePlaying) {
            // Reflect static EQ response curve gently on visualizer when idle
            val idleBins = List(24) { b ->
                val idx = ((b / 24f) * 9f).toInt().coerceIn(0, 9)
                val normalized = ((combinedBands[idx] + 12f) / 24f).coerceIn(0.08f, 0.92f)
                normalized * 0.45f
            }
            _spectrumBins.value = idleBins
        }

        // Update AudioTrack stereo volume for instant L/R balance during acoustic audition
        audioTrack?.let { track ->
            val lGain = (1f - max(0f, sound.balance)).coerceIn(0.05f, 1f)
            val rGain = (1f + min(0f, sound.balance)).coerceIn(0.05f, 1f)
            runCatching { track.setStereoVolume(lGain, rGain) }
        }
    }

    private fun computeCombined10BandGain(
        sound: SoundStudioState,
        theatre: HomeTheatreState,
    ): List<Float> {
        val bassTilt = sound.bassBoost * 6.5f +
            (if (theatre.cinemaModeEnabled) theatre.deepBass * 4.5f + theatre.theatreBassDb else 0f)
        val subTilt = if (theatre.cinemaModeEnabled) theatre.virtualSubwoofer * 5.0f else 0f
        val trebleTilt = sound.trebleBoost * 6.0f +
            (if (theatre.cinemaModeEnabled) theatre.theatreTrebleDb else 0f)
        val vocalTilt = max(sound.vocalEnhancer, if (theatre.cinemaModeEnabled) theatre.dialogueEnhancement else 0f) * 5.0f
        val clarityTilt = sound.clarityEnhancer * 4.5f

        val weightsBass = listOf(1.0f, 0.9f, 0.65f, 0.25f, 0f, 0f, 0f, 0f, 0f, 0f)
        val weightsSub = listOf(1.0f, 0.7f, 0.2f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val weightsVocal = listOf(0f, 0f, 0f, 0.2f, 0.55f, 0.95f, 1.0f, 0.7f, 0.2f, 0f)
        val weightsClarity = listOf(0f, 0f, 0f, 0f, 0f, 0.2f, 0.6f, 0.95f, 0.9f, 0.6f)
        val weightsTreble = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0.2f, 0.6f, 0.95f, 1.0f)

        return List(10) { i ->
            val base = sound.eqBandsDb.getOrElse(i) { 0f }
            val shaped = base +
                bassTilt * weightsBass[i] +
                subTilt * weightsSub[i] +
                vocalTilt * weightsVocal[i] +
                clarityTilt * weightsClarity[i] +
                trebleTilt * weightsTreble[i]
            shaped.coerceIn(-12f, 12f)
        }
    }

    private fun releaseEffectsOnly() {
        runCatching { dynamicsProcessing?.release() }
        runCatching { legacyEqualizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { presetReverb?.release() }
        runCatching { loudnessEnhancer?.release() }
        dynamicsProcessing = null
        legacyEqualizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
    }

    fun release() {
        stopReferenceTone()
        releaseEffectsOnly()
        runCatching { context.unregisterReceiver(volumeReceiver) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33SpatialHelper.teardown(audioManager, spatializerListenerHandle, headTrackerListenerHandle)
        }
        spatialExecutor.shutdownNow()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private object Api28DynamicsHelper {
        private val bandCutoffs = floatArrayOf(
            45f, 90f, 180f, 355f, 710f, 1400f, 2800f, 5600f, 11200f, 20000f
        )
        private val mbcCutoffs = floatArrayOf(250f, 3500f, 20000f)

        fun create(sessionId: Int): DynamicsProcessing {
            val cfg = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,
                true,
                10,
                true,
                3,
                true,
                10,
                true,
            ).build()
            return DynamicsProcessing(0, sessionId, cfg)
        }

        fun apply(
            dp: DynamicsProcessing,
            bandsDb: List<Float>,
            balance: Float,
            clarity: Float,
            vocal: Float,
            subwoofer: Float,
        ) {
            for (i in 0 until 10) {
                val gain = bandsDb.getOrElse(i) { 0f }.coerceIn(-12f, 12f)
                dp.setPreEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(true, bandCutoffs[i], gain))
                val postGain = when (i) {
                    0 -> subwoofer * 3.5f
                    1 -> subwoofer * 2.2f
                    5, 6 -> vocal * 1.8f
                    else -> 0f
                }
                dp.setPostEqBandAllChannelsTo(i, DynamicsProcessing.EqBand(true, bandCutoffs[i], postGain))
            }

            // Per-channel Left/Right balance attenuation in dB
            val leftDb = if (balance > 0f) (-balance * 18f) else 0f
            val rightDb = if (balance < 0f) (balance * 18f) else 0f
            dp.setInputGainbyChannel(0, leftDb)
            dp.setInputGainbyChannel(1, rightDb)

            // Multi-band compressor tuning for Clarity & Vocal presence
            for (m in 0 until 3) {
                val ratio = when (m) {
                    1 -> 1.2f + vocal * 1.8f
                    2 -> 1.2f + clarity * 2.0f
                    else -> 1.15f
                }
                val post = when (m) {
                    1 -> vocal * 2.5f
                    2 -> clarity * 3.0f
                    else -> 0f
                }
                dp.setMbcBandAllChannelsTo(
                    m,
                    DynamicsProcessing.MbcBand(
                        true,
                        mbcCutoffs[m],
                        5f,
                        80f,
                        ratio,
                        -20f,
                        6f,
                        -75f,
                        1f,
                        0f,
                        post,
                    )
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33SpatialHelper {
        fun setup(
            audioManager: AudioManager,
            executor: java.util.concurrent.Executor,
            onUpdate: (available: Boolean, enabled: Boolean, headTracker: Boolean, levelLabel: String) -> Unit,
            onStoreHandles: (stateListener: Any, headListener: Any) -> Unit,
        ) {
            val sp = audioManager.spatializer
            fun emitNow() {
                val levelLabel = when (sp.immersiveAudioLevel) {
                    Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL -> "Multichannel Spatial"
                    Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_OTHER -> "Object / Other Spatial"
                    else -> "Stereo / Virtualized"
                }
                val headAvail = runCatching { sp.isHeadTrackerAvailable }.getOrDefault(false)
                onUpdate(sp.isAvailable, sp.isEnabled, headAvail, levelLabel)
            }
            emitNow()
            val stateListener = object : Spatializer.OnSpatializerStateChangedListener {
                override fun onSpatializerEnabledChanged(spat: Spatializer, enabled: Boolean) {
                    emitNow()
                }

                override fun onSpatializerAvailableChanged(spat: Spatializer, available: Boolean) {
                    emitNow()
                }
            }
            val headListener = object : Spatializer.OnHeadTrackerAvailableListener {
                override fun onHeadTrackerAvailableChanged(spat: Spatializer, available: Boolean) {
                    emitNow()
                }
            }
            runCatching {
                sp.addOnSpatializerStateChangedListener(executor, stateListener)
                sp.addOnHeadTrackerAvailableListener(executor, headListener)
            }
            onStoreHandles(stateListener, headListener)
        }

        fun teardown(
            audioManager: AudioManager,
            stateListenerHandle: Any?,
            headListenerHandle: Any?,
        ) {
            val sp = audioManager.spatializer
            (stateListenerHandle as? Spatializer.OnSpatializerStateChangedListener)?.let {
                runCatching { sp.removeOnSpatializerStateChangedListener(it) }
            }
            (headListenerHandle as? Spatializer.OnHeadTrackerAvailableListener)?.let {
                runCatching { sp.removeOnHeadTrackerAvailableListener(it) }
            }
        }
    }
}
