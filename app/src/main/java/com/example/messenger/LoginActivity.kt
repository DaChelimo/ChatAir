package com.example.messenger

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.messenger.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Login"
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        val email = binding.loginUserEmail

        binding.loginButton.setOnClickListener {
            if (email.text.isEmpty()){
                Toast.makeText(this, "Input fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Processing input...", Toast.LENGTH_SHORT).show()

            firebasePhoneAuth.verifyPhoneNumber(email.text.toString(), 60, TimeUnit.SECONDS, this, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    firebaseAuth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            Timber.i("Success. Login: firebaseAuth.signInWithCredential(credential) and uid is ${it.user?.uid}")
                            val intent = Intent(this@LoginActivity, LatestMessagesActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                        .addOnFailureListener {
                            Timber.e(it)
                            Timber.e("error message: ${it.message}")
                            Timber.e("error cause: ${it.cause}")
//                            Toast.makeText(this, "")
                        }
                }

                override fun onVerificationFailed(it: FirebaseException) {
                    Timber.e("error message: ${it.message}")

                    when (it.message){
                        INVALID_FORMAT_MESSAGE  -> Toast.makeText(this@LoginActivity, "Phone number should start with country code then number eg +1 777 555 666", Toast.LENGTH_SHORT).show()
                        TOO_SHORT_MESSAGE -> Toast.makeText(this@LoginActivity, "phone number is too short. Try a valid number.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    storedVerificationId = verificationId
                    resendingToken = token
                }
            })

            // firebasePhoneAuth.verifyPhoneNumber(emailText, 60, TimeUnit.SECONDS, this, object: PhoneAuthProvider.OnVerificationStateChangedCallbacks()

//            firebaseAuth.signInWithEmailAndPassword(email.text.toString(), password.text.toString())
//                .addOnSuccessListener {
//                    Timber.i("Email is ${email.text} and password is ${password.text}")
//                    val intent = Intent(this, LatestMessagesActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    startActivity(intent)
//                }
//                .addOnFailureListener {
//                    Timber.e(it)
//                    when (it.message) {
//                        "There is no user record corresponding to this identifier. The user may have been deleted." -> {
//                            Toast.makeText(applicationContext, "An account with this email does not exist", Toast.LENGTH_LONG).show()
//                        }
//                        "The password is invalid or the user does not have a password." -> {
//                            Toast.makeText(applicationContext, "Invalid password. Try again.", Toast.LENGTH_LONG).show()
//                        }
//                        else -> {
//                            Toast.makeText(applicationContext, "Unknown error.", Toast.LENGTH_LONG).show()
//                        }
//                    }
//                }
        }
    }
}