package com.example.messenger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.messenger.databinding.ActivityPersonalVideoCallBinding

class PersonalVideoCallActivity: AppCompatActivity() {

    lateinit var binding: ActivityPersonalVideoCallBinding
    lateinit var agoraEngine: AgoraEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_personal_video_call)

        agoraEngine = AgoraEngine(this, binding)

        agoraEngine.initializeAgoraEngineAndJoinChannel()

        binding.endVideoCallBtn.setOnClickListener {
            agoraEngine.leaveChannel()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        agoraEngine.leaveChannel()
    }

}