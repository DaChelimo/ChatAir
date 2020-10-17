package com.example.messenger.both_profile

import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import java.util.*

class UserProfileViewModel (private val fragment: UserProfileFragment): ViewModel() {

//    private var _newImage
    private var _currentAccount = MutableLiveData<User>()
    val currentAccount: LiveData<User>
        get() = _currentAccount

    val context = lazy {
        fragment.requireContext()
    }.value

    var modifiedAccount = MutableLiveData<User>()

    init {
        loadCurrentUser()
        modifiedAccount.value = _currentAccount.value?.copy()
        Timber.d("modifiedAccount.value is ${modifiedAccount.value}")
    }

    fun uploadImageToFireStoreStorage(imageUri: Uri){
        val fileName = UUID.randomUUID().toString()
        val ref = firebaseStorage.getReference("/images/$fileName")

        ref.putFile(imageUri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener {
                Timber.i("downloadUrl is $it")
                modifiedAccount.value?.profilePictureUrl = it.toString()
            }
        }
    }



    fun updateUserInFirebaseDatabase() {
        context.showShortToast("Uploading data online")

        val myRef = firebaseDatabase.getReference("users/${firebaseAuth.uid}")
        myRef.setValue(modifiedAccount.value)
            .addOnSuccessListener {
                context.showLongToast("Saved")
                _currentAccount.value = modifiedAccount.value?.copy()
            }
            .addOnFailureListener {
                Timber.e(it)
            }
    }

    private fun loadCurrentUser(){
        val ref = firebaseDatabase.getReference("/users/${firebaseAuth.uid}")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                Timber.d("user is $user")

                user?.let {
                    _currentAccount.value = it
                }
            }
        })
    }

    val userNameTextWatcher = object : CustomTextWatcher() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            Timber.d("s is $s")
            modifiedAccount.value?.userName = s.toString()
        }
    }

    val aboutTextWatcher = object : CustomTextWatcher() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            Timber.d("s is $s")
            modifiedAccount.value?.aboutDescription = s.toString()
        }
    }


}