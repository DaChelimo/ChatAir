package com.example.messenger

import android.content.Context
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityPersonalVoiceCallBinding
import kotlinx.coroutines.Runnable
import timber.log.Timber
import java.util.*

class PersonalVoiceCallActivity : AppCompatActivity() {

    lateinit var binding: ActivityPersonalVoiceCallBinding
    lateinit var friendUser: User
//    lateinit var currentCall: Call

    var shouldRun = true
    var seconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_personal_voice_call)
    }
}
//
//        if (intent != null) {
//            friendUser = intent!!.getParcelableExtra(VOICE_CALL_USER) ?: return
//        }
//        else{
//            Timber.i("intent is null")
//        }
//
//        setUpBasicInfo()
//
//        val sinchClient = Sinch.getSinchClientBuilder()
//            .applicationKey(APPLICATION_KEY)
//            .applicationSecret(APPLICATION_SECRET)
//            .userId(firebaseAuth.uid)
//            .environmentHost("clientapi.sinch.com")
//            .context(applicationContext)
//            .build()
//
//        sinchClient.setSupportCalling(true)
//        sinchClient.setSupportPushNotifications(true)
//        sinchClient.startListeningOnActiveConnection()
//
//        sinchClient.callClient.addCallClientListener(callClientListener)
//        sinchClient.start()
//
//        Timber.i("sinchClient is $sinchClient")
//        currentCall = sinchClient.callClient.callUser(friendUser.uid)
//        Timber.i("currentCall is $currentCall")
//        currentCall.addCallListener(sinchCallListener)
//
//        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
//
//        binding.voiceCallMuteMic.setOnClickListener {
//            if (audioManager.isMicrophoneMute){
//                Timber.d("Is mute.")
//                audioManager.isMicrophoneMute = false
//            }
//            else{
//                Timber.d("Is not mute.")
//                audioManager.isMicrophoneMute = true
//            }
//        }
//
//        binding.voiceCallHangUp.setOnClickListener {
//            Timber.d("Hanged Up")
//            currentCall.hangup()
//        }
//
//        Timber.d("current call state is ${currentCall.state}")
//    }
//
//    private fun setUpBasicInfo() {
//        binding.voiceCallName.text = friendUser.userName
//
//        Glide.with(this)
//            .load(friendUser.profilePictureUrl)
//            .into(binding.voiceCallUserImage)
//    }
//
//    override fun onBackPressed() {
//        val alertDialog = AlertDialog.Builder(this)
//            .setTitle("End Call")
//            .setMessage("Are you sure you want to end the call?")
//            .setPositiveButton("Yes") { _, _ -> currentCall.hangup() }
//            .setNegativeButton("No") {_, _ -> }
//            .create()
//
//        alertDialog.show()
//    }
//
//    private val callClientListener = object : CallClientListener{
//        override fun onIncomingCall(callClient: CallClient?, incomingCall: Call?) {
//            currentCall = incomingCall ?: return
//            currentCall.answer()
//        }
//    }
//
//    private val sinchCallListener = object : CallListener{
//        override fun onCallProgressing(p0: Call?) {
//            binding.voiceCallState.text = "Ringing"
//            Timber.d("Is ringing")
//        }
//
//        override fun onCallEstablished(p0: Call?) {
//            Toast.makeText(this@PersonalVoiceCallActivity, "Connected", Toast.LENGTH_SHORT)
//                .show()
//
//            volumeControlStream = AudioManager.STREAM_VOICE_CALL
//            val handler = Handler()
//            shouldRun = true
//            handler.post(object : Runnable{
//                override fun run() {
//
//                    if (shouldRun) {
//                        seconds++
//
//                        binding.voiceCallState.text = formatCallTime(seconds)
//
//                        handler.postDelayed(this, 1000)
//                    }
//                    else Timber.d("Is not running")
//                }
//            })
//
//            Timber.d("Is connected")
//        }
//        override fun onCallEnded(p0: Call?) {
//            seconds = 0
//            shouldRun = false
//            volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
//            Toast.makeText(applicationContext, "Call ended!", Toast.LENGTH_SHORT).show()
//
//            Timber.d("Call ended")
//            finish()
//        }
//
//        override fun onShouldSendPushNotification(p0: Call?, p1: MutableList<PushPair>?) {
//            Timber.d("Should send push notification")
//        }
//    }
//}