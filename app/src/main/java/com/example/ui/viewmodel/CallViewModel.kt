package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallDirection
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.UserProfile
import com.example.data.repository.CallRepository
import com.example.webrtc.WebRtcCallManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

data class ActiveCallUiState(
    val session: CallSession? = null,
    val isOutgoing: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = true,
    val connectionLabel: String = "Connecting...",
    val durationSeconds: Long = 0L,
    val isEnded: Boolean = false
)

class CallViewModel(
    private val context: Context,
    private val currentUid: String,
    private val callRepository: CallRepository
) : ViewModel() {

    val webRtc = WebRtcCallManager(context)

    private val _uiState = MutableStateFlow(ActiveCallUiState())
    val uiState: StateFlow<ActiveCallUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var iceJob: Job? = null
    private var timerJob: Job? = null
    private var answerProcessed = false
    private var historyLogged = false

    fun attachVideoRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        webRtc.attachRenderers(local, remote)
    }

    fun startOutgoingCall(caller: UserProfile, callee: UserProfile, isVideo: Boolean) {
        viewModelScope.launch {
            _uiState.value = ActiveCallUiState(
                isOutgoing = true,
                isSpeakerOn = isVideo,
                isCameraOn = isVideo,
                connectionLabel = "Calling..."
            )
            callRepository.startCall(caller, callee, isVideo).onSuccess { session ->
                _uiState.update { it.copy(session = session) }
                webRtc.startPeerConnection(
                    isVideo = isVideo,
                    onIceCandidate = { candidate ->
                        callRepository.pushIceCandidate(session.callId, true, candidate)
                    },
                    onConnectionStateChange = { state ->
                        if (state == PeerConnection.PeerConnectionState.CONNECTED) {
                            startDurationTimer()
                            _uiState.update { it.copy(connectionLabel = "Connected") }
                        } else if (state == PeerConnection.PeerConnectionState.FAILED) {
                            _uiState.update { it.copy(connectionLabel = "Reconnecting...") }
                        }
                    }
                )
                webRtc.createOffer(isVideo) { offerSdp ->
                    viewModelScope.launch {
                        callRepository.setOfferSdp(session.callId, offerSdp)
                    }
                }
                observeCallUpdates(session.callId, isCaller = true)
            }.onFailure {
                _uiState.update { it.copy(isEnded = true, connectionLabel = "Call failed") }
            }
        }
    }

    fun attachIncomingCall(session: CallSession) {
        _uiState.value = ActiveCallUiState(
            session = session,
            isOutgoing = false,
            isSpeakerOn = session.video,
            isCameraOn = session.video,
            connectionLabel = if (session.video) "Incoming video call" else "Incoming voice call"
        )
        observeCallUpdates(session.callId, isCaller = false)
    }

    fun acceptIncomingCall() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(connectionLabel = "Connecting...") }
        webRtc.startPeerConnection(
            isVideo = session.video,
            onIceCandidate = { candidate ->
                callRepository.pushIceCandidate(session.callId, false, candidate)
            },
            onConnectionStateChange = { state ->
                if (state == PeerConnection.PeerConnectionState.CONNECTED) {
                    startDurationTimer()
                    _uiState.update { it.copy(connectionLabel = "Connected") }
                }
            }
        )
        viewModelScope.launch {
            val latest = _uiState.value.session ?: session
            if (latest.offerSdp.isNotBlank()) {
                webRtc.receiveOfferAndCreateAnswer(latest.offerSdp, latest.video) { answerSdp ->
                    viewModelScope.launch {
                        callRepository.setAnswerSdp(latest.callId, answerSdp)
                        startDurationTimer()
                        _uiState.update { it.copy(connectionLabel = "Connected") }
                    }
                }
            }
        }
    }

    fun rejectIncomingCall() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            callRepository.updateCallStatus(session, CallStatus.REJECTED)
            logHistoryOnce(session, CallDirection.MISSED, 0L)
            finishCall()
        }
    }

    fun endCall() {
        val session = _uiState.value.session
        val duration = _uiState.value.durationSeconds
        viewModelScope.launch {
            if (session != null) {
                val finalStatus = if (duration == 0L && _uiState.value.isOutgoing && session.status == CallStatus.RINGING.name) {
                    CallStatus.MISSED
                } else {
                    CallStatus.ENDED
                }
                callRepository.updateCallStatus(session, finalStatus)
                val direction = if (session.callerId == currentUid) CallDirection.OUTGOING else CallDirection.INCOMING
                logHistoryOnce(session, direction, duration)
            }
            finishCall()
        }
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        webRtc.setMuted(next)
        _uiState.update { it.copy(isMuted = next) }
    }

    fun toggleSpeaker() {
        val next = !_uiState.value.isSpeakerOn
        webRtc.setSpeakerEnabled(next)
        _uiState.update { it.copy(isSpeakerOn = next) }
    }

    fun toggleCamera() {
        val next = !_uiState.value.isCameraOn
        webRtc.setCameraEnabled(next)
        _uiState.update { it.copy(isCameraOn = next) }
    }

    fun switchCamera() {
        webRtc.switchCamera()
    }

    private fun observeCallUpdates(callId: String, isCaller: Boolean) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            callRepository.observeCallSession(callId).collect { updated ->
                if (updated == null) return@collect
                _uiState.update { it.copy(session = updated) }
                if (isCaller && updated.answerSdp.isNotBlank() && !answerProcessed) {
                    answerProcessed = true
                    webRtc.receiveAnswer(updated.answerSdp)
                    startDurationTimer()
                    _uiState.update { it.copy(connectionLabel = "Connected") }
                }
                when (updated.status) {
                    CallStatus.REJECTED.name -> {
                        _uiState.update { it.copy(connectionLabel = "Declined") }
                        logHistoryOnce(updated, CallDirection.OUTGOING, 0L)
                        delay(700)
                        finishCall()
                    }
                    CallStatus.ENDED.name, CallStatus.MISSED.name -> {
                        val dir = if (updated.callerId == currentUid) CallDirection.OUTGOING
                        else if (updated.status == CallStatus.MISSED.name) CallDirection.MISSED else CallDirection.INCOMING
                        logHistoryOnce(updated, dir, _uiState.value.durationSeconds)
                        finishCall()
                    }
                }
            }
        }
        iceJob?.cancel()
        iceJob = viewModelScope.launch {
            callRepository.observeRemoteIceCandidates(callId, isCaller).collect { candidate ->
                webRtc.addRemoteIceCandidate(candidate)
            }
        }
    }

    private fun startDurationTimer() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    private suspend fun logHistoryOnce(session: CallSession, direction: CallDirection, durationSeconds: Long) {
        if (historyLogged) return
        historyLogged = true
        val isCaller = session.callerId == currentUid
        val peerId = if (isCaller) session.calleeId else session.callerId
        val peerName = if (isCaller) session.calleeName else session.callerName
        val peerPhoto = if (isCaller) session.calleePhoto else session.callerPhoto
        callRepository.logCallHistory(
            currentUid = currentUid,
            peerId = peerId,
            peerName = peerName,
            peerPhoto = peerPhoto,
            isVideo = session.video,
            direction = direction,
            durationSeconds = durationSeconds
        )
    }

    private fun finishCall() {
        timerJob?.cancel()
        sessionJob?.cancel()
        iceJob?.cancel()
        webRtc.release()
        _uiState.update { it.copy(isEnded = true) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        sessionJob?.cancel()
        iceJob?.cancel()
        webRtc.release()
    }
}
