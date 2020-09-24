package com.example.messenger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.example.messenger.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        Timber.d("firebaseAuth.currentUser is ${firebaseAuth.currentUser}")

    }

    override fun onBackPressed() {
        super.onBackPressed()
        Timber.d(
            "findNavController(R.id.fragment).currentDestination == registerFrag is ${
                findNavController(R.id.fragment).currentDestination == NavDestination("com.example.messenger.register.RegisterFragment")
            }"
        )
        Timber.d("firebaseAuth.currentUser is ${firebaseAuth.currentUser}")

        if (findNavController(R.id.fragment).currentDestination == NavDestination("com.example.messenger.register.RegisterFragment")
            && firebaseAuth.currentUser == null) {
            finish()
        }
    }
}