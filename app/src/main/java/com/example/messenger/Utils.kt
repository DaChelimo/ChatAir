package com.example.messenger

import android.os.Parcelable
import com.google.firebase.auth.FirebaseAuth
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

@Parcelize
data class User(val userName: String, val uid: String, val profilePictureUrl: String): Parcelable{
    constructor(): this("",  "", "")
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

@Parcelize
data class EachMessage(val id: String, val fromId: String, val toId: String, val imageUrl: String?, val textMessage: String, val timeStamp: Long, val username: String, val profilePictureUrl: String, val receiverAccount: User?, val senderAccount: User?): Parcelable{
    constructor(): this("", "", "", null, "", -1, "", "", null, null)
}