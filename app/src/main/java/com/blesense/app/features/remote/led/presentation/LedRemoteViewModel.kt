package com.blesense.app.features.remote.led.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blesense.app.features.bluetooth.domain.usecase.SendBleCommandUseCase
import com.blesense.app.features.bluetooth.domain.usecase.StopBleAdvertisingUseCase
import com.blesense.app.features.remote.led.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.sqrt

class LedRemoteViewModel(
    private val sendBleCommand: SendBleCommandUseCase,
    private val stopBleAdvertising: StopBleAdvertisingUseCase
) : ViewModel() {

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val _activeCommand = MutableStateFlow(LEDCommand.SOLID_MAGENTA)
    val activeCommand = _activeCommand.asStateFlow()

    private val _secondaryCommand = MutableStateFlow(LEDCommand.SOLID_YELLOW)
    val secondaryCommand = _secondaryCommand.asStateFlow()

    private val _customInterval = MutableStateFlow(2) // 2 seconds default
    val customInterval = _customInterval.asStateFlow()

    private val _isCustomCycleEnabled = MutableStateFlow(false)
    val isCustomCycleEnabled = _isCustomCycleEnabled.asStateFlow()

    private val _selectedCommands = MutableStateFlow<List<LEDCommand>>(listOf(LEDCommand.SOLID_MAGENTA))
    val selectedCommands = _selectedCommands.asStateFlow()

    private val _isCycleModeActive = MutableStateFlow(false)
    val isCycleModeActive = _isCycleModeActive.asStateFlow()

    private val _selectedRemote = MutableStateFlow(RemoteType.REMOTE_1)
    val selectedRemote = _selectedRemote.asStateFlow()

    private val _isDiscoEnabled = MutableStateFlow(false)
    val isDiscoEnabled = _isDiscoEnabled.asStateFlow()

    private val _isVisualizerActive = MutableStateFlow(false)
    val isVisualizerActive = _isVisualizerActive.asStateFlow()

    private val _selectedMood = MutableStateFlow<Mood?>(null)
    val selectedMood = _selectedMood.asStateFlow()

    private val _sliderValue = MutableStateFlow(128)
    val sliderValue = _sliderValue.asStateFlow()

    private val _audioHue = MutableStateFlow(0f)
    val audioHue = _audioHue.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel = _audioLevel.asStateFlow()

    private val _advertisingHistory = MutableStateFlow<List<AdvertisingHistory>>(emptyList())
    val advertisingHistory = _advertisingHistory.asStateFlow()

    private var discoJob: Job? = null
    private var moodJob: Job? = null
    private var audioJob: Job? = null
    private var advertTimerJob: Job? = null
    
    private val _advertisingTime = MutableStateFlow(0)
    val advertisingTime = _advertisingTime.asStateFlow()

    fun onRemoteTypeChange(type: RemoteType) {
        _selectedRemote.value = type
        if (type != RemoteType.REMOTE_1) {
            _isDiscoEnabled.value = false
            _isCycleModeActive.value = false
        }
        if (type != RemoteType.REMOTE_4) {
            _isVisualizerActive.value = false
            _isCustomCycleEnabled.value = false
            _selectedMood.value = null
        }
        if (_isAdvertising.value) retransmit()
    }

    fun onCommandChange(command: LEDCommand, isSecondary: Boolean = false) {
        if (_isCycleModeActive.value && !isSecondary) {
            toggleCommand(command)
            return
        }

        if (isSecondary) {
            _secondaryCommand.value = command
        } else {
            _activeCommand.value = command
            _selectedCommands.value = listOf(command)
            _selectedMood.value = null
        }
        if (_isAdvertising.value) retransmit()
    }

    private fun toggleCommand(command: LEDCommand) {
        val current = _selectedCommands.value.toMutableList()
        if (current.contains(command)) {
            if (current.size > 1) current.remove(command) // Keep at least one
        } else {
            current.add(command)
        }
        _selectedCommands.value = current
        _activeCommand.value = command // For the 'Last active' display
        if (_isAdvertising.value) retransmit()
    }

    fun toggleCycleMode(enabled: Boolean) {
        _isCycleModeActive.value = enabled
        if (enabled) {
            _isDiscoEnabled.value = false
            _selectedMood.value = null
            startPlaylistCycle()
        } else {
            moodJob?.cancel()
            _selectedCommands.value = listOf(_activeCommand.value)
            if (_isAdvertising.value) retransmit()
        }
    }

    fun onSliderChange(value: Int) {
        _sliderValue.value = value
        if (_isAdvertising.value) retransmit()
    }

    fun onCustomIntervalChange(interval: Int) {
        _customInterval.value = interval
        if (_isCustomCycleEnabled.value) startCustomCycle()
    }

    fun toggleDisco(enabled: Boolean) {
        _isDiscoEnabled.value = enabled
        if (enabled) {
            _isVisualizerActive.value = false
            _selectedMood.value = null
            startDiscoCycle()
        } else {
            discoJob?.cancel()
        }
    }

    fun toggleVisualizer(enabled: Boolean, context: Context) {
        _isVisualizerActive.value = enabled
        if (enabled) {
            _isDiscoEnabled.value = false
            _isCustomCycleEnabled.value = false
            _selectedMood.value = null
            startAudioEngine(context)
        } else {
            audioJob?.cancel()
        }
    }

    fun toggleCustomCycle(enabled: Boolean) {
        _isCustomCycleEnabled.value = enabled
        if (enabled) {
            _isDiscoEnabled.value = false
            _isVisualizerActive.value = false
            _selectedMood.value = null
            startCustomCycle()
        } else {
            moodJob?.cancel()
        }
    }

    fun selectMood(mood: Mood?) {
        _selectedMood.value = mood
        if (mood != null) {
            _isDiscoEnabled.value = false
            _isVisualizerActive.value = false
            startMoodCycle(mood)
        } else {
            moodJob?.cancel()
        }
    }

    fun startAdvertising() {
        if (_isAdvertising.value) {
            stopAdvertising()
            return
        }

        _isAdvertising.value = true
        retransmit()
        startAdvertTimer()
    }

    private fun retransmit() {
        val remote = _selectedRemote.value
        
        // Handle automated modes (Disco/Audio/Mood/Custom/Playlist)
        if (_isDiscoEnabled.value || _isVisualizerActive.value || _selectedMood.value != null || _isCustomCycleEnabled.value || _isCycleModeActive.value) {
            return
        }

        val command = _activeCommand.value
        val key = when(remote) {
            RemoteType.REMOTE_1 -> SECRET_KEY_SOLID
            RemoteType.REMOTE_2 -> SECRET_KEY_ANIMATION
            RemoteType.REMOTE_3 -> SECRET_KEY_SLIDER // slider-specific protocol
            RemoteType.REMOTE_4 -> SECRET_KEY_SOLID
        }

        val data = if (remote == RemoteType.REMOTE_3) {
            byteArrayOf(key, _sliderValue.value.toByte())
        } else {
            byteArrayOf(key, command.id)
        }

        sendBleCommand(data, 10000, if (remote == RemoteType.REMOTE_3) 0x0001 else command.companyId)
        addToHistory(if (remote == RemoteType.REMOTE_3) "Slider: ${_sliderValue.value}" else command.displayName, remote, command.companyId, "${key.toString(16).uppercase()}")
    }

    fun stopAdvertising() {
        stopBleAdvertising()
        _isAdvertising.value = false
        _advertisingTime.value = 0
        advertTimerJob?.cancel()
    }

    private fun startAdvertTimer() {
        advertTimerJob?.cancel()
        _advertisingTime.value = 0
        advertTimerJob = viewModelScope.launch {
            while (_isAdvertising.value) {
                delay(1000)
                _advertisingTime.value++
            }
        }
    }

    private fun startDiscoCycle() {
        discoJob?.cancel()
        discoJob = viewModelScope.launch {
            val commands = LEDCommand.values().filter { it.mode == "Solid" && it != LEDCommand.SOLID_OFF }
            var idx = 0
            while (isActive) {
                _activeCommand.value = commands[idx % commands.size]
                if (_isAdvertising.value) {
                    val cmd = _activeCommand.value
                    sendBleCommand(byteArrayOf(SECRET_KEY_SOLID, cmd.id), 2000, cmd.companyId)
                }
                delay(1000)
                idx++
            }
        }
    }

    private fun startMoodCycle(mood: Mood) {
        moodJob?.cancel()
        moodJob = viewModelScope.launch {
            var colorIdx = 0
            while (isActive) {
                val cmd = mood.colors[colorIdx % mood.colors.size]
                _activeCommand.value = cmd
                if (_isAdvertising.value) {
                    sendBleCommand(byteArrayOf(SECRET_KEY_SOLID, cmd.id), 10000, cmd.companyId)
                }
                delay(mood.interval * 1000L)
                colorIdx++
            }
        }
    }

    private fun startCustomCycle() {
        moodJob?.cancel()
        moodJob = viewModelScope.launch {
            var toggle = true
            while (isActive) {
                val cmd = if (toggle) _activeCommand.value else _secondaryCommand.value
                if (_isAdvertising.value) {
                    sendBleCommand(byteArrayOf(SECRET_KEY_SOLID, cmd.id), 10000, cmd.companyId)
                }
                delay(_customInterval.value * 1000L)
                toggle = !toggle
            }
        }
    }

    private fun startPlaylistCycle() {
        moodJob?.cancel()
        moodJob = viewModelScope.launch {
            var idx = 0
            while (isActive) {
                val list = _selectedCommands.value
                if (list.isEmpty()) { delay(1000); continue }
                val cmd = list[idx % list.size]
                _activeCommand.value = cmd
                if (_isAdvertising.value) {
                    sendBleCommand(byteArrayOf(SECRET_KEY_SOLID, cmd.id), 10000, cmd.companyId)
                }
                delay(_customInterval.value * 1000L)
                idx++
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioEngine(context: Context) {
        audioJob?.cancel()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _isVisualizerActive.value = false
            return
        }

        audioJob = viewModelScope.launch(Dispatchers.Default) {
            val bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
            val buffer = ShortArray(bufferSize)

            try {
                recorder.startRecording()
                var hue = 0f
                var lastTransmittedHue = -1f

                while (isActive && _isVisualizerActive.value) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) sum += buffer[i] * buffer[i]
                        val rms = sqrt(sum / read) / 32767.0
                        
                        _audioLevel.value = rms.toFloat().coerceIn(0f, 1f)
                        hue = (hue + (0.5f + rms.toFloat() * 5f)) % 360f
                        _audioHue.value = hue

                        // Throttled BLE Transmission (every 200ms approx)
                        if (_isAdvertising.value && Math.abs(hue - lastTransmittedHue) > 20f) {
                            val color = findClosestLEDCommand(Color.hsv(hue, 1f, 1f))
                            sendBleCommand(byteArrayOf(SECRET_KEY_SOLID, color.id), 500, color.companyId)
                            lastTransmittedHue = hue
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                recorder.stop()
                recorder.release()
            }
        }
    }

    private fun addToHistory(label: String, remote: RemoteType, companyId: Int, dataHex: String) {
        val newHistory = _advertisingHistory.value.toMutableList()
        newHistory.add(0, AdvertisingHistory(label, System.currentTimeMillis(), remote, companyId, dataHex))
        if (newHistory.size > 10) newHistory.removeAt(newHistory.lastIndex)
        _advertisingHistory.value = newHistory
    }

    private fun findClosestLEDCommand(target: Color): LEDCommand {
        val commands = LEDCommand.values().filter { it.mode == "Solid" && it != LEDCommand.SOLID_OFF }
        return commands.minByOrNull { cmd ->
            val dr = target.red - cmd.color.red
            val dg = target.green - cmd.color.green
            val db = target.blue - cmd.color.blue
            dr * dr + dg * dg + db * db
        } ?: LEDCommand.SOLID_WHITE
    }

    override fun onCleared() {
        super.onCleared()
        discoJob?.cancel()
        moodJob?.cancel()
        audioJob?.cancel()
        advertTimerJob?.cancel()
    }
}
