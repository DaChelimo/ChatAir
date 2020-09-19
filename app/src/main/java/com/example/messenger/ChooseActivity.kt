package com.example.messenger

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.messenger.latest_messages.LatestMessagesFragment
import com.example.messenger.register.RegisterFragment
import timber.log.Timber

class ChooseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        Timber.i("firebase currentUser is ${firebaseAuth.currentUser}")
        if (firebaseAuth.currentUser != null){
            val intent = Intent(this, LatestMessagesFragment::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        else{
            val intent = Intent(this, RegisterFragment::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportActionBar?.show()
    }
}