package com.example.messenger

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bigbangbutton.editcodeview.EditCodeView
import com.example.messenger.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.activity_register.view.*
import kotlinx.android.synthetic.main.enter_code_layout.*
import kotlinx.android.synthetic.main.enter_code_layout.view.*
import kotlinx.android.synthetic.main.enter_code_layout.view.enter_code_input
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
                    signInWithCredential(credential)
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

        }

        binding.loginEnterCodeBtn.setOnClickListener {
            if(storedVerificationId == null){
                Toast.makeText(this, "You must first sign up to receive code.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val dialogView = layoutInflater.inflate(R.layout.enter_code_layout, null) //from(this).inflate(R.layout.enter_code_layout, null)

            val alert = AlertDialog.Builder(this)
                .setView(dialogView)

            val enterCodeBtn: Button = dialogView.findViewById(R.id.enter_code_submit_btn)
            val cancelBtn: Button = dialogView.findViewById(R.id.enter_code_cancel_btn)
            val inputCode: EditCodeView = dialogView.findViewById(R.id.enter_code_input)

            alert.setCancelable(true)
            val customAlert = alert.create()
            customAlert.show()

            enterCodeBtn.setOnClickListener {
                if(inputCode.codeLength != 6){
                    Toast.makeText(this, "Code is invalid. Try again", Toast.LENGTH_SHORT).show()
                    Timber.d("Code is too short")
                    inputCode.clearCode()
                }
                else{
                    Toast.makeText(this, "Code is being processed.", Toast.LENGTH_SHORT).show()
                    val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, inputCode.code)
                    signInWithCredential(credential)
                    customAlert.dismiss()
                }
            }

            cancelBtn.setOnClickListener {
                customAlert.dismiss()
            }

        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
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
}