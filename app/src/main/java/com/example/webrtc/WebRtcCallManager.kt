package com.example.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.IceCandidateData
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRtcCallManager(private val context: Context) {

    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var isInitialized = false

    fun initializeFactory() {
        if (isInitialized) return
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
        isInitialized = true
    }

    fun attachRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        localRenderer = local
        remoteRenderer = remote
        local?.let { renderer ->
            try {
                renderer.init(eglBase.eglBaseContext, null)
                renderer.setMirror(true)
                renderer.setEnableHardwareScaler(true)
                localVideoTrack?.addSink(renderer)
            } catch (e: Exception) {
                Log.w(TAG, "Local renderer init skipped: ${e.message}")
            }
        }
        remote?.let { renderer ->
            try {
                renderer.init(eglBase.eglBaseContext, null)
                renderer.setEnableHardwareScaler(true)
            } catch (e: Exception) {
                Log.w(TAG, "Remote renderer init skipped: ${e.message}")
            }
        }
    }

    fun startPeerConnection(
        isVideo: Boolean,
        onIceCandidate: (IceCandidateData) -> Unit,
        onConnectionStateChange: (PeerConnection.PeerConnectionState) -> Unit
    ) {
        initializeFactory()
        val factory = peerConnectionFactory ?: return

        val iceServers = mutableListOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )
        val turnUrl = readOptionalBuildConfig("TURN_SERVER_URL")
        if (turnUrl.isNotBlank() && !turnUrl.startsWith("MY_TURN_")) {
            iceServers.add(
                PeerConnection.IceServer.builder(turnUrl)
                    .setUsername(readOptionalBuildConfig("TURN_USERNAME"))
                    .setPassword(readOptionalBuildConfig("TURN_CREDENTIAL"))
                    .createIceServer()
            )
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    onIceCandidate(
                        IceCandidateData(
                            sdpMid = it.sdpMid ?: "",
                            sdpMLineIndex = it.sdpMLineIndex,
                            sdp = it.sdp
                        )
                    )
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.firstOrNull()?.let { track ->
                    remoteRenderer?.let { track.addSink(it) }
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    remoteRenderer?.let { track.addSink(it) }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                newState?.let(onConnectionStateChange)
            }
        })

        // Local Audio
        localAudioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("ALOK_AUDIO_TRACK", localAudioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection?.addTrack(localAudioTrack, listOf("ALOK_STREAM"))

        // Configure AudioManager for call
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = isVideo

        // Local Video if video call
        if (isVideo) {
            setupLocalVideo(factory)
        }
    }

    private fun setupLocalVideo(factory: PeerConnectionFactory) {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        val frontCamera = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: deviceNames.firstOrNull()
        if (frontCamera != null) {
            videoCapturer = enumerator.createCapturer(frontCamera, null)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            localVideoSource = factory.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
            try {
                videoCapturer?.startCapture(720, 1280, 30)
            } catch (e: Exception) {
                Log.w(TAG, "Camera capture start failed: ${e.message}")
            }
            localVideoTrack = factory.createVideoTrack("ALOK_VIDEO_TRACK", localVideoSource)
            localVideoTrack?.setEnabled(true)
            localRenderer?.let { localVideoTrack?.addSink(it) }
            peerConnection?.addTrack(localVideoTrack, listOf("ALOK_STREAM"))
        }
    }

    fun createOffer(isVideo: Boolean, onOfferCreated: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideo.toString()))
        }
        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                onOfferCreated(sdp.description)
            }
        }, constraints)
    }

    fun receiveOfferAndCreateAnswer(
        offerSdp: String,
        isVideo: Boolean,
        onAnswerCreated: (String) -> Unit
    ) {
        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideo.toString()))
                }
                peerConnection?.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        sdp ?: return
                        peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                        onAnswerCreated(sdp.description)
                    }
                }, constraints)
            }
        }, remoteDesc)
    }

    fun receiveAnswer(answerSdp: String) {
        val remoteDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), remoteDesc)
    }

    fun addRemoteIceCandidate(candidate: IceCandidateData) {
        if (candidate.sdp.isBlank()) return
        peerConnection?.addIceCandidate(
            IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
        )
    }

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun release() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {
        }
        videoCapturer?.dispose()
        videoCapturer = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        localVideoSource?.dispose()
        localVideoSource = null
        localAudioSource?.dispose()
        localAudioSource = null
        localRenderer?.release()
        localRenderer = null
        remoteRenderer?.release()
        remoteRenderer = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {
            Log.e(TAG, "SDP create failure: $error")
        }
        override fun onSetFailure(error: String?) {
            Log.e(TAG, "SDP set failure: $error")
        }
    }

    private fun readOptionalBuildConfig(fieldName: String): String {
        return try {
            BuildConfig::class.java.getField(fieldName).get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private companion object {
        const val TAG = "WebRtcCallManager"
    }
}
