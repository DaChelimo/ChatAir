package com.example.messenger

import android.content.Context
import android.view.SurfaceView
import android.view.View
import com.example.messenger.databinding.ActivityPersonalVideoCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import timber.log.Timber

class AgoraEngine(private val applicationContext: Context, val binding: ActivityPersonalVideoCallBinding) {

    var mRtcEngine: RtcEngine? = null
    private val mRtcEventHandler = object : IRtcEngineEventHandler(){

        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            setupRemoteVideo(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            onRemoteUserLeft()
        }

        override fun onUserMuteVideo(uid: Int, muted: Boolean) {
            onRemoteUserVideoMuted(uid, muted)
        }
    }

    private fun initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(applicationContext, applicationContext.getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (e: Exception) {
            Timber.e(e)
            throw RuntimeException("NEED TO check rtc sdk init fatal error" + e.stackTrace)
        }
    }

    fun initializeAgoraEngineAndJoinChannel(){
        initializeAgoraEngine()
        setupVideoProfile()
        setupLocalVideo()
        joinChannel()
    }

    private fun setupVideoProfile() {
        // In simple use cases, we only need to enable video capturing
        // and rendering once at the initialization step.
        // Note: audio recording and playing is enabled by default.
        mRtcEngine!!.enableVideo()
//      mRtcEngine!!.setVideoProfile(Constants.VIDEO_PROFILE_360P, false) // Earlier than 2.3.0

        // Please go to this page for detailed explanation
        // https://docs.agora.io/en/Video/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_rtc_engine.html#af5f4de754e2c1f493096641c5c5c1d8f
        mRtcEngine!!.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x360,
            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
            VideoEncoderConfiguration.STANDARD_BITRATE,
            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT)
        )
    }

    private fun setupLocalVideo() {
        val container = binding.localVideoViewContainer//.local_video_view_container) as FrameLayout
        val surfaceView = RtcEngine.CreateRendererView(applicationContext)
        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)
        // Initializes the local video view.
        // RENDER_MODE_FIT: Uniformly scale the video until one of its dimension fits the boundary. Areas that are not filled due to the disparity in the aspect ratio are filled with black.
        mRtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun joinChannel() {
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name that
        // you use to generate this token.
        var token: String? = applicationContext.getString(R.string.agora_access_token)
        if (token!!.isEmpty()) {
            token = null
        }
        mRtcEngine!!.joinChannel(token, "demoChannel1", "Extra Optional Data", 0) // if you do not specify the uid, we will generate the uid for you
    }

    private fun setupRemoteVideo(uid: Int) {
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.
        val container = binding.remoteVideoViewContainer//findViewById(R.id.remote_video_view_container) as FrameLayout

        if (container.childCount >= 1) {
            Timber.d("Child count is ${container.childCount}")
            return
        }

        /*
          Creates the video renderer view.
          CreateRendererView returns the SurfaceView type. The operation and layout of the view
          are managed by the app, and the Agora SDK renders the view provided by the app.
          The video display view must be created using this method instead of directly
          calling SurfaceView.
         */

        val surfaceView = RtcEngine.CreateRendererView(applicationContext)
        container.addView(surfaceView)
        // Initializes the video view of a remote user.
        mRtcEngine!!.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))

        surfaceView.tag = uid // for mark purpose
        Timber.d("surfaceView.tag is ${surfaceView.tag}")
    }

     fun leaveChannel() {
        mRtcEngine!!.leaveChannel()
    }

    private fun onRemoteUserLeft() {
        val container = binding.remoteVideoViewContainer//findViewById(R.id.remote_video_view_container) as FrameLayout
        container.removeAllViews()
    }

    private fun onRemoteUserVideoMuted(uid: Int, muted: Boolean) {
        val container = binding.remoteVideoViewContainer//findViewById(R.id.remote_video_view_container) as FrameLayout

        val surfaceView = container.getChildAt(0) as SurfaceView

        val tag = surfaceView.tag
        if (tag != null && tag as Int == uid) {
            surfaceView.visibility = if (muted) View.GONE else View.VISIBLE
        }
    }
}