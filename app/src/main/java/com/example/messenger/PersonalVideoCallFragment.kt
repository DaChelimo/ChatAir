package com.example.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.messenger.custom_webrtc.CustomPeerConnectionObserver
import com.example.messenger.custom_webrtc.CustomSdpObserver
import com.example.messenger.databinding.FragmentPersonalVideoCallBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import timber.log.Timber


class PersonalVideoCallFragment: Fragment() {

    lateinit var binding: FragmentPersonalVideoCallBinding
    lateinit var localVideoTrack: VideoTrack
    lateinit var localAudioTrack: AudioTrack
    lateinit var peerConnectionFactory: PeerConnectionFactory
    lateinit var localVideoView: SurfaceViewRenderer
    lateinit var remoteVideoView: SurfaceViewRenderer

    var localPeer: PeerConnection? = null
    var remotePeer: PeerConnection? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_personal_video_call,
            container,
            false
        )

        localVideoView = binding.localVideoViewContainer
        remoteVideoView = binding.remoteVideoViewContainer

        start()
        call()

        binding.endVideoCallBtn.setOnClickListener {
            hangup()
        }

        return binding.root
    }

    private fun start() {

        //Initialize PeerConnectionFactory globals.
        //Params are context, initAudio,initVideo and videoCodecHwAcceleration
        PeerConnectionFactory.initializeAndroidGlobals(this.requireContext(), true, true, true)

        //Create a new PeerConnectionFactory instance.
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory(options)


        //Now create a VideoCapturer instance. Callback methods are there if you want to do something! Duh!
        val videoCapturerAndroid = createVideoCapturer()
        val constraints = MediaConstraints()


        val videoSource: VideoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid)
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource)

        //create an AudioSource instance
        val audioSource: AudioSource = peerConnectionFactory.createAudioSource(constraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource)

        //we will start capturing the video from the camera
        //params are width,height and fps
        videoCapturerAndroid!!.startCapture(1000, 1000, 30)

        //create surface renderer, init it and add the renderer to the track
        localVideoView = binding.localVideoViewContainer
        localVideoView.setMirror(true)

        val rootEglBase = EglBase.create()
        localVideoView.init(rootEglBase.eglBaseContext, null)

        localVideoTrack.addRenderer(VideoRenderer(localVideoView))
    }

    private fun createVideoCapturer(): VideoCapturer? {
        return createCameraCapturer(Camera1Enumerator(false))
    }


    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // Trying to find a front facing camera!
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // We were not able to find a front cam. Look for other cameras
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private fun call() {
        //we already have video and audio tracks. Now create peerconnections
        val iceServers: List<IceServer> = ArrayList()

        //create sdpConstraints
        val sdpConstraints = MediaConstraints()
        sdpConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        sdpConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))

        //creating localPeer
        localPeer = peerConnectionFactory.createPeerConnection(
            iceServers,
            sdpConstraints,
            object : CustomPeerConnectionObserver("localPeerCreation") {
                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    super.onIceCandidate(iceCandidate)
                    onIceCandidateReceived(localPeer, iceCandidate)
                }
            })

        //creating remotePeer
        remotePeer = peerConnectionFactory.createPeerConnection(
            iceServers,
            sdpConstraints,
            object : CustomPeerConnectionObserver("remotePeerCreation") {
                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    super.onIceCandidate(iceCandidate)
                    onIceCandidateReceived(remotePeer, iceCandidate)
                }

                override fun onAddStream(mediaStream: MediaStream) {
                    super.onAddStream(mediaStream)
                    gotRemoteStream(mediaStream)
                }
            })

        //creating local mediastream
        val stream: MediaStream = peerConnectionFactory.createLocalMediaStream("102")
        stream.addTrack(localAudioTrack)
        stream.addTrack(localVideoTrack)
        localPeer?.addStream(stream)

        //creating Offer
        localPeer?.createOffer(object : CustomSdpObserver("localCreateOffer") {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                localPeer?.setLocalDescription(
                    CustomSdpObserver("localSetLocalDesc"),
                    sessionDescription
                )
                remotePeer?.setRemoteDescription(
                    CustomSdpObserver("remoteSetRemoteDesc"),
                    sessionDescription
                )
                remotePeer?.createAnswer(object : CustomSdpObserver("remoteCreateOffer") {
                    override fun onCreateSuccess(sessionDescription: SessionDescription) {
                        //remote answer generated. Now set it as local desc for remote peer and remote desc for local peer.
//                        super.onCreateSuccess(sessionDescription)
                        remotePeer?.setLocalDescription(
                            CustomSdpObserver("remoteSetLocalDesc"),
                            sessionDescription
                        )
                        localPeer?.setRemoteDescription(
                            CustomSdpObserver("localSetRemoteDesc"),
                            sessionDescription
                        )
                    }
                }, MediaConstraints())
            }
        }, MediaConstraints())

    }


    private fun hangup() {
        localPeer?.close()
        remotePeer?.close()
        localPeer = null
        remotePeer = null
    }

    private fun gotRemoteStream(stream: MediaStream) {
        //we have remote video stream. add to the renderer.
        val videoTrack = stream.videoTracks.first
        val audioTrack = stream.audioTracks.first
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val remoteRenderer = VideoRenderer(remoteVideoView)
//                remoteVideoView.visibility = View.VISIBLE
                videoTrack.addRenderer(remoteRenderer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onIceCandidateReceived(peer: PeerConnection?, iceCandidate: IceCandidate?) {
        //we have received ice candidate. We can set it to the other peer.
        Timber.d("peer is $peer")
        if (peer == null) return
        if (peer === localPeer) {
            remotePeer?.addIceCandidate(iceCandidate)
        } else {
            localPeer?.addIceCandidate(iceCandidate)
        }
    }




}