package com.example.messenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.messenger.databinding.ActivityLoginBinding
import timber.log.Timber

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Login"
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        val email = binding.loginUserEmail
        val password = binding.loginUserPassword


        binding.loginButton.setOnClickListener {
            if (email.text.isEmpty() || password.text.isEmpty()){
                Toast.makeText(this, "Input fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Processing input...", Toast.LENGTH_SHORT).show()
            firebaseAuth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnSuccessListener {
                    Timber.i("Email is ${email.text} and password is ${password.text}")
                    val intent = Intent(this, LatestMessagesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Timber.e(it)
                    when (it.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> {
                            Toast.makeText(applicationContext, "An account with this email does not exist", Toast.LENGTH_LONG).show()
                        }
                        "The password is invalid or the user does not have a password." -> {
                            Toast.makeText(applicationContext, "Invalid password. Try again.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(applicationContext, "Unknown error.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }
}