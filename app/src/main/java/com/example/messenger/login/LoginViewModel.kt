package com.example.messenger.login

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import timber.log.Timber
import java.util.concurrent.TimeUnit

class LoginViewModel (private val fragment: Fragment) : ViewModel() {
    private var _shouldNavigate = MutableLiveData<Boolean>()
    val shouldNavigate: LiveData<Boolean>
        get() = _shouldNavigate

    fun verifyPhoneNumber(phoneNumber: String) {
        firebasePhoneAuth.verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, fragment.requireActivity(), object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(it: FirebaseException) {
                Timber.e("error message: ${it.message}")

                when (it.message){
                    INVALID_FORMAT_MESSAGE  -> showShortToast("Phone number should start with country code then number eg +1 777 555 666")
                    TOO_SHORT_MESSAGE -> showShortToast("Phone number is too short. Try a valid number.")
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendingToken = token
            }
        })
    }

    fun showShortToast(text: String) {
        Toast.makeText(fragment.requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    fun signInWithCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                Timber.i("Success. Login: firebaseAuth.signInWithCredential(credential) and uid is ${it.user?.uid}")
                _shouldNavigate.value = true
            }
            .addOnFailureListener {
                Timber.e(it)
                Timber.e("error message: ${it.message}")
                Timber.e("error cause: ${it.cause}")
                showShortToast("Error occurred signing in")
            }
    }
}