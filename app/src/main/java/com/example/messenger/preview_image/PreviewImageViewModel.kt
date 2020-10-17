package com.example.messenger.preview_image

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.*
import com.example.messenger.notification_pack.MyResponse
import com.example.messenger.notification_pack.Notification
import com.example.messenger.notification_pack.NotificationBody
import com.example.messenger.notification_pack.RetrofitItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*

class PreviewImageViewModel(fragment: PreviewImageFragment) : ViewModel() {

    val myAccount = Account.getAccount()
    val friendUser = fragment.friendUser

    private var _storageImageUrl = MutableLiveData<String>()
    val storageImageUrl: LiveData<String>
        get() = _storageImageUrl

    private fun composeNotification() {
        Timber.d("composeNotification called")
        val topic = "/topics/${friendUser.uid}"
        val ref =
            firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}/${friendUser.uid}")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.getValue(EachPersonalMessage::class.java) ?: return

                if (myAccount?.userName == null) return
                val notificationBody = NotificationBody(myAccount.userName, lastMessage.textMessage)
                val notification = Notification(topic, notificationBody)

                sendActualNotification(notification)
            }
        })
    }

    fun sendActualNotification(notification: Notification) {
        Timber.d("sendActualNotification called. notification is $notification")
        RetrofitItem.postData.sendNotificationInApi(notification)
            .enqueue(object : Callback<MyResponse> {
                override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                    Timber.e(t)
                }

                override fun onResponse(
                    call: Call<MyResponse>,
                    response: Response<MyResponse>
                ) {
                    Timber.d("response.code is ${response.code()}")
                    if (response.code() == 400) {
                        Timber.i("Success: Notification sent")
                        Timber.i("response body is ${response.body()}")

                    }
                }
            })
    }

    fun getStorageImageUrl(uri: Uri) {
        val storageRef = firebaseStorage.getReference("users/${firebaseAuth.uid}/${UUID.randomUUID()}")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener {
                        _storageImageUrl.value = it.toString()
                    }
                    .addOnFailureListener {
                        Timber.e(it)
                    }
            }
            .addOnFailureListener {
                Timber.e(it)
            }
    }

    fun sendMessage(chooseImageUrl: String?, textMessage: String) {
        val fromId = firebaseAuth.uid
        val toId = friendUser.uid
        val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId").push()
        val toRef = firebaseDatabase.getReference("/user-messages/$toId/$fromId/${ref.key}")

        if (firebaseAuth.uid == null) {
            Timber.e("Error occurred.")
            return
        }
        if (myAccount?.userName == null || myAccount.profilePictureUrl == null) {
            Timber.e("myAccount.userName == null is ${myAccount?.userName == null} and myAccount.profilePictureUrl == null is ${myAccount?.profilePictureUrl == null}")
            return
        }
        val refMessage = EachPersonalMessage(
            id = ref.key.toString(),
            fromId = firebaseAuth.uid!!,
            toId = friendUser.uid!!,
            imageUrl = chooseImageUrl,
            textMessage = textMessage,
            timeStamp = System.currentTimeMillis(),
            username = myAccount.userName!!,
            profilePictureUrl = myAccount.profilePictureUrl!!,
            receiverAccount = friendUser,
            senderAccount = myAccount,
            wasRead = false
        )

        setFirebaseValues(ref, toRef, refMessage)
    }

    private fun setFirebaseValues(
        ref: DatabaseReference,
        toRef: DatabaseReference,
        refPersonalMessage: EachPersonalMessage
    ) {
        toRef.setValue(refPersonalMessage)
        ref.setValue(refPersonalMessage).addOnSuccessListener {
            composeNotification()
        }
        val fromId = firebaseAuth.uid
        val toId = friendUser.uid
        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        refPersonalMessage.id = refPersonalMessage.receiverAccount?.uid.toString()
        latestMessagesFromRef.setValue(refPersonalMessage)
        latestMessagesToRef.setValue(refPersonalMessage)
//        chooseImageUrl = null
    }

}