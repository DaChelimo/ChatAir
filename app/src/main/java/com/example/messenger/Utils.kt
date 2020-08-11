package com.example.messenger

import android.os.Parcelable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.parcel.Parcelize
import timber.log.Timber

val firebaseAuth = FirebaseAuth.getInstance()
val firebaseStorage = FirebaseStorage.getInstance() // for images
val firebaseDatabase = FirebaseDatabase.getInstance() // for data
val firebaseInstanceId = FirebaseInstanceId.getInstance()
val firebaseMessaging = FirebaseMessaging.getInstance()
val firebasePhoneAuth = PhoneAuthProvider.getInstance()

var storedVerificationId: String? = null
var resendingToken: PhoneAuthProvider.ForceResendingToken? = null

const val TOO_SHORT_MESSAGE = "The format of the phone number provided is incorrect. Please enter the phone number in a format that can be parsed into E.164 format. E.164 phone numbers are written in the format [+][country code][subscriber number including area code]. [ TOO_SHORT ]"
const val INVALID_FORMAT_MESSAGE = "The format of the phone number provided is incorrect. Please enter the phone number in a format that can be parsed into E.164 format. E.164 phone numbers are written in the format [+][country code][subscriber number including area code]. [ Invalid format. ]"

@Parcelize
data class User(var userName: String, val uid: String, var profilePictureUrl: String, var aboutDescription: String?, val phoneNumber: String): Parcelable{
    constructor(): this("",  "", "", null, "")
}

data class TokenClass(val userName: String, val uid: String, val token: String){
    constructor(): this("", "", "")
}

data class NotificationBody(val title: String, val message: String)
data class Notification(val to: String, val data: NotificationBody)

fun sendUserToToDatabase(token: String){
    Timber.d("Token: $token")

    val databaseRef = firebaseDatabase.getReference("/user/${firebaseAuth.uid}")
    databaseRef.addValueEventListener(object : ValueEventListener{
        override fun onCancelled(error: DatabaseError) {
            Timber.e(error.details)
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            val user = snapshot.getValue(User::class.java) ?: return

            val ref = firebaseDatabase.getReference("/tokens/${firebaseAuth.uid}}")
            if (firebaseAuth.uid == null) return
            ref.setValue(TokenClass(user.userName, firebaseAuth.uid!!, token))
        }
    })
}

const val USER_KEY = "USER_KEY"
const val INTENT_URI = "INTENT_URI"
const val FRIEND_USER_PROFILE = "FRIEND_USER_PROFILE"

@Parcelize
data class EachMessage(val id: String, val fromId: String, val toId: String, val imageUrl: String?, var textMessage: String, val timeStamp: Long, val username: String, val profilePictureUrl: String, val receiverAccount: User?, val senderAccount: User?): Parcelable{
    constructor(): this("", "", "", null, "", -1, "", "", null, null)
}
