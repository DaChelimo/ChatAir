package com.example.messenger.register

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class RegisterViewModel (private val fragment: RegisterFragment): ViewModel() {

    private var _shouldNavigate = MutableLiveData<Boolean>()
    val shouldNavigate: LiveData<Boolean>
        get() = _shouldNavigate

    var imageUri: Uri? = null

    fun verifyPhoneNumber(phoneNumber: String, username: String) {
        firebasePhoneAuth.verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, fragment.requireActivity(), object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential, username, phoneNumber)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendingToken = token
            }

            override fun onVerificationFailed(error: FirebaseException) {
                Timber.e(error)
            }
        })
    }

    fun signInWithCredential(credential: PhoneAuthCredential, username: String, phoneNumber: String) {
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                Timber.d("Success.")
                val displayName = it.user?.displayName
                Timber.i("firebaseAuth.currentUser?.displayName is ${firebaseAuth.currentUser?.displayName} and it.user?.displayName is ${it.user?.displayName}")
                val uid = it.user?.uid
                Timber.i("firebaseAuth.uid is ${firebaseAuth.uid} and it.user?.uid is ${it.user?.uid}")
                Timber.i("displayName is $displayName and uid is $uid")
                if (imageUri == null) saveUserToFirebaseDatabase(null, username, phoneNumber) else uploadImageToFireStoreStorage(imageUri, username, phoneNumber)
                showShortToast("Success. You have signed up.")
            }
            .addOnFailureListener {
                Timber.e(it)
                showShortToast("You have entered the wrong code. Try again..")
            }
    }

    private fun showShortToast(text: String) {
        Toast.makeText(fragment.requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun uploadImageToFireStoreStorage(
        imageUri: Uri?,
        username: String,
        phoneNumber: String
    ) {
        if (imageUri == null)return

        val fileName = UUID.randomUUID().toString()
        val ref = firebaseStorage.getReference("/images/$fileName")

        ref.putFile(imageUri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener {
                Timber.i("downloadUrl is $it")
                saveUserToFirebaseDatabase(it.toString(), username, phoneNumber)
            }
        }
    }

    private fun saveUserToFirebaseDatabase(
        onlineImageUrl: String?,
        username: String,
        phoneNumber: String
    ){
        val uid = firebaseAuth.uid
        val ref = firebaseDatabase.getReference("/users/${uid}")
        Timber.i("ref is $ref")
        val user = uid?.let { User(username, it, onlineImageUrl, null, phoneNumber) }
        showShortToast( "Successfully signed up.")
        ref.setValue(user)
            .addOnSuccessListener {
                _shouldNavigate.value = true
            }
//        val intent = Intent(this, LatestMessagesActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
    }
}