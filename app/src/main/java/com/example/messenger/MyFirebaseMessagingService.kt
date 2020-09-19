package com.example.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.messenger.latest_messages.LatestMessagesFragment
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (firebaseAuth.currentUser != null){
            sendUserToToDatabase(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Timber.d("From: ${remoteMessage.from}")

        Timber.d("remoteMessage.data is ${remoteMessage.data} and notification is ${remoteMessage.notification}")
        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Timber.d("Message data payload: %s", remoteMessage.data)
            Timber.d("title is ${remoteMessage.data["sendersName"].toString()} and msg is ${remoteMessage.data["message"].toString()}")

            // Compose and show notification
            if (!remoteMessage.data.isNullOrEmpty()) {
                val title : String = remoteMessage.data["sendersName"].toString()
                val msg: String = remoteMessage.data["message"].toString()
                sendNotification(title, msg)
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            sendNotification(null, remoteMessage.notification?.body)
        }
    }

    companion object {
        const val CHANNEL_ID = "Receive notifications on incoming messages."
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, LatestMessagesFragment::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channelId = CHANNEL_ID
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_baseline_person_24)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        // https://developer.android.com/training/notify-user/build-notification#Priority
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Receive notifications on incoming messages.", NotificationManager.IMPORTANCE_HIGH)
            channel.enableLights(true)
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())
    }
}