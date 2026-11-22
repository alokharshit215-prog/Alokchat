package com.example.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class AudioPlayerManager {

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun togglePlayPause(messageId: String, url: String, scope: CoroutineScope) {
        val current = _state.value
        if (current.activeMessageId == messageId && player != null) {
            if (current.isPlaying) {
                player?.pause()
                _state.value = current.copy(isPlaying = false)
            } else {
                player?.start()
                _state.value = current.copy(isPlaying = true)
                startProgressLoop(scope)
            }
            return
        }

        stop()
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { prepared ->
                    prepared.start()
                    _state.value = AudioPlaybackState(
                        activeMessageId = messageId,
                        isPlaying = true,
                        positionMs = 0L,
                        durationMs = prepared.duration.toLong().coerceAtLeast(1L)
                    )
                    startProgressLoop(scope)
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Audio playback error: $what / $extra")
                    stop()
                    true
                }
                prepareAsync()
            }
            player = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
            stop()
        }
    }

    fun seekTo(messageId: String, positionMs: Long) {
        if (_state.value.activeMessageId == messageId) {
            player?.seekTo(positionMs.toInt())
            _state.value = _state.value.copy(positionMs = positionMs)
        }
    }

    private fun startProgressLoop(scope: CoroutineScope) {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && player != null && _state.value.isPlaying) {
                val pos = try {
                    player?.currentPosition?.toLong() ?: 0L
                } catch (_: Exception) {
                    0L
                }
                _state.value = _state.value.copy(positionMs = pos)
                delay(150)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        _state.value = AudioPlaybackState()
    }

    private companion object {
        const val TAG = "AudioPlayerManager"
    }
}
