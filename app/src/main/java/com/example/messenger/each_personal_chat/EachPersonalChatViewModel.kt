package com.example.messenger.each_personal_chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class EachPersonalChatViewModel (private val fragment: EachPersonalChatFragment): ViewModel() {

    private var _scrollToPosition = MutableLiveData<Int>()
    val scrollToPosition: LiveData<Int>
        get() = _scrollToPosition
    
    var friendUser = MutableLiveData<User?>()
    var myAccount: User? = null
    val IMAGE_REQUEST_CODE = 1234
    var longPressMessage: Item<GroupieViewHolder>? = null
    var longPressView: ArrayList<View> = ArrayList()
    var messagesList: ArrayList<Item<GroupieViewHolder>> = ArrayList()
    var canAllowLongClick = true
    var chooseImageUrl: String? = null

    val adapter = GroupAdapter<GroupieViewHolder>()
    
    fun showShortToast(text: String) {
        Toast.makeText(fragment.requireContext(), text, Toast.LENGTH_SHORT).show()
    }
    
    fun getProfilePicture() {
        val myUid = firebaseAuth.uid
        val users = firebaseDatabase.getReference("/users/$myUid")
        users.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val eachAvailableUser = snapshot.getValue(User::class.java) ?: return

                myAccount = eachAvailableUser
            }
        })
    }

     fun addNewMessageToAdapter(eachPersonalMessage: EachPersonalMessage) {

        if (eachPersonalMessage.imageUrl == null) {
            adapter.add(
                MyTextChatItem(
                    eachPersonalMessage.textMessage,
                    eachPersonalMessage
                )
            )
        } else {
            adapter.add(
                MyImageChatItem(
                    eachPersonalMessage.imageUrl,
                    eachPersonalMessage,
                    fragment
                )
            )
        }

         _scrollToPosition.value = adapter.itemCount
         
    }

    fun listenForMessages() {
        val fromId = firebaseAuth.uid
        val toId = friendUser.value?.uid
        val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId")
        adapter.clear()
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val newMessage = it.getValue(EachPersonalMessage::class.java) ?: return@forEach

                    addMessageToAdapter(newMessage)
                    changeEachPersonalMessageToRead(newMessage)
                }
                _scrollToPosition.value = adapter.itemCount
            }
        })

    }

    fun addMessageToAdapter(newMessage: EachPersonalMessage?) {
        if (newMessage != null) {
            if (newMessage.fromId == firebaseAuth.uid) {
                if (newMessage.imageUrl == null) {
                    val adapterItem =
                        MyTextChatItem(newMessage.textMessage, newMessage)
                    adapter.add(adapterItem)
                    messagesList.add(adapterItem)

                } else {
                    val adapterItem =
                        MyImageChatItem(newMessage.imageUrl, newMessage, fragment)
                    adapter.add(adapterItem)
                    messagesList.add(adapterItem)
                }
            } else {
                if (newMessage.imageUrl == null) {
                    val adapterItem = FriendTextChatItem(
                        newMessage.textMessage,
                        newMessage
                    )
                    adapter.add(adapterItem)
                    messagesList.add(adapterItem)
                } else {
                    val adapterItem = FriendImageChatItem(
                        newMessage.imageUrl,
                        newMessage,
                        fragment
                    )
                    adapter.add(adapterItem)
                    messagesList.add(adapterItem)
                }
            }
        }
        
        val fromId = firebaseAuth.uid
        val toId = friendUser.value?.uid
        val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId/${newMessage?.id}/wasRead")

        ref.setValue(true)
            .addOnSuccessListener {
                Timber.d("Success.")
            }
            .addOnFailureListener {
                Timber.e(it)
            }

        _scrollToPosition.value = adapter.itemCount
    }


     fun composeNotification() {
        Timber.d("composeNotification called")
        val topic = "/topics/${friendUser.value?.uid}"

//        var notification: Notification? = null
//        var notificationBody: NotificationBody?

        val ref =
            firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}/${friendUser.value?.uid}")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.getValue(EachPersonalMessage::class.java) ?: return

                if (myAccount?.userName == null) return
                val notificationBody = NotificationBody(myAccount?.userName!!, lastMessage.textMessage)
                val notification = Notification(topic, notificationBody)

//                notificationBody = NotificationBody(myAccount?.userName!!, lastMessage.textMessage)
//                notificationBody?.let {
//                    notification = Notification(topic, notificationBody!!)
//                }
//
//                notification?.let {
//                    sendActualNotification(notification!!)
//                }

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

    fun sendMessage(chooseImageUrl: String?, textMessage: String) {
        val fromId = firebaseAuth.uid
        val toId = friendUser.value?.uid
        val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId").push()
        val toRef = firebaseDatabase.getReference("/user-messages/$toId/$fromId/${ref.key}")

        if (firebaseAuth.uid == null || friendUser.value?.uid == null) {
            Timber.e("Error occurred.")
            return
        }
        if (myAccount?.userName == null || myAccount?.profilePictureUrl == null || friendUser.value == null) {
            Timber.e("friendUser.value == null is ${friendUser.value == null} and myAccount?.userName == null is ${myAccount?.userName == null} and myAccount?.profilePictureUrl == null is ${myAccount?.profilePictureUrl == null}")
            return
        }
        val refMessage = EachPersonalMessage(
            id = ref.key.toString(),
            fromId = firebaseAuth.uid!!,
            toId = friendUser.value?.uid!!,
            imageUrl = chooseImageUrl,
            textMessage = textMessage,
            timeStamp = System.currentTimeMillis(),
            username = myAccount?.userName!!,
            profilePictureUrl = myAccount?.profilePictureUrl!!,
            receiverAccount = friendUser.value,
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
//            listenForMessages()
            addNewMessageToAdapter(refPersonalMessage)
            composeNotification()
//            _closeKeyboard.value = true
            _scrollToPosition.value = adapter.itemCount
        }
        val fromId = firebaseAuth.uid
        val toId = friendUser.value?.uid
        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

         refPersonalMessage.id = refPersonalMessage.receiverAccount?.uid.toString()
        latestMessagesFromRef.setValue(refPersonalMessage)
        latestMessagesToRef.setValue(refPersonalMessage)
//        chooseImageUrl = null
    }

     fun shareMessage() {
        if (longPressMessage == null) {
            Timber.d("longPressShareBtn is null")
            return
        }

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/*"
        intent.putExtra(
            Intent.EXTRA_TEXT,
            if (longPressMessage is MyTextChatItem) {
                val myTextChatItem = longPressMessage as MyTextChatItem
                myTextChatItem.text
            } else {
                val friendTextChatItem = longPressMessage as FriendTextChatItem
                friendTextChatItem.text
            }
        )

//        startActivity(intent)
    }

     fun copyToClipboard() {
        val clipboardManager = fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipboardText = if (longPressMessage is MyTextChatItem) {
            val myTextChatItem = longPressMessage as MyTextChatItem
            myTextChatItem.text
        } else {
            val friendTextChatItem = longPressMessage as FriendTextChatItem
            friendTextChatItem.text
        }

        val clip = ClipData.newPlainText("Chat Message", clipboardText)
        Timber.d("clipboard data is ${clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()}")
        clipboardManager.setPrimaryClip(clip)
         showShortToast("Copied to clipboard")
    }

     fun returnItemToDefault() {
        Timber.d("${longPressView}")
        longPressView.forEach {
            Timber.d("new view. Size is ${longPressView.size}")
            it.setBackgroundColor(fragment.resources.getColor(R.color.defaultBlue))
        }
        longPressView.clear()
        canAllowLongClick = true
    }

     fun removeTextMessageFromAdapterForEveryone(
        myRef: DatabaseReference,
        latestMessagesFromRef: DatabaseReference
    ) {
        if (longPressMessage == null) {
            Timber.d("longPressMessage is null")
            return
        }

        deleteMyMessageForMe(myRef, latestMessagesFromRef)

        val myTextChatItem = longPressMessage as MyTextChatItem

        try {

            Timber.d("myTextChatItem.newMessage.textMessage is ${myTextChatItem.newPersonalMessage.textMessage}")
            Timber.d("Everyone position is ${
                messagesList.indexOf(
                    longPressMessage!!)}")
            Timber.d("MessageList size is ${messagesList.size}")
            Timber.d("Position of item in adapter is ${adapter.getAdapterPosition(
                longPressMessage!!)}")
            Timber.d("longPressMessage is ${(longPressMessage as MyTextChatItem).newPersonalMessage}")

            if (myTextChatItem.newPersonalMessage.textMessage != "This message was deleted") {
                myTextChatItem.newPersonalMessage.textMessage = "This message was deleted"
            }

//                val position = messagesList.indexOf(longPressMessage!!)
//                messagesList[adapter.getAdapterPosition(longPressMessage!!)] = myTextChatItem
//
//
//                Timber.d("Success. messageList[position] with adapter.position is ${messagesList[adapter.getAdapterPosition(longPressMessage!!)]}")
//
//                adapter.update(messagesList)
            longPressMessage = myTextChatItem
            longPressMessage?.notifyChanged()
            adapter.notifyItemChanged(adapter.getAdapterPosition(longPressMessage!!))

            Timber.d("updated message is ${(adapter.getItem(adapter.getAdapterPosition(
                longPressMessage!!)) as MyTextChatItem).newPersonalMessage}")
            longPressMessage = null
        }
        catch(e: ArrayIndexOutOfBoundsException){
            Timber.e(e)
            Timber.i("RemoveMessage for everyone did not work")
            if (numberRepeated <= 5) {
                numberRepeated++
                removeTextMessageFromAdapterForEveryone(myRef, latestMessagesFromRef)
            }
            else{
                numberRepeated = 1
            }
        }
    }

    var numberRepeated = 1

     fun removeTextMessageFromAdapterForMe(
        myRef: DatabaseReference,
        latestMessagesFromRef: DatabaseReference
    ) {
        if (longPressMessage == null) {
            Timber.d("longPressMessage is null")
            return
        }
        val position = messagesList.indexOf(longPressMessage!!)
        Timber.d("messageList size is ${messagesList.size}")

        try {
            if (longPressMessage is MyTextChatItem) {
                Timber.d("position is $position")

                if (position >= 0) {
                    Timber.d("It worked, no error")
                }

                val myTextChatItem = longPressMessage as MyTextChatItem
                Timber.i("myTextChatItem is ${myTextChatItem.newPersonalMessage.textMessage}")

                if (myTextChatItem.newPersonalMessage.textMessage != "This message was deleted") {
                    myTextChatItem.newPersonalMessage.textMessage = "This message was deleted"

                    (messagesList[position] as MyTextChatItem).newPersonalMessage.textMessage =
                        "This message was deleted"

                    adapter.update(messagesList)
                    deleteMyMessageForMe(myRef, latestMessagesFromRef)
                    longPressMessage = null

                } else {
                    adapter.remove(longPressMessage!!)

                    val fromId = myTextChatItem.newPersonalMessage.fromId
                    val toId = myTextChatItem.newPersonalMessage.toId
                    val key = myTextChatItem.newPersonalMessage.id
                    val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId/$key")

                    longPressMessage = null

                    ref.removeValue()
                        .addOnSuccessListener {
                            Timber.i("Removed successfully")
                        }
                        .addOnFailureListener {
                            Timber.e(it)
                        }
                }
            } else {
                adapter.remove(longPressMessage!!)

                val friendTextChatItem = longPressMessage as FriendTextChatItem
                Timber.i("myTextChatItem is ${friendTextChatItem.newPersonalMessage.textMessage}")
                longPressMessage = null

                val fromId = friendTextChatItem.newPersonalMessage.fromId
                val toId = friendTextChatItem.newPersonalMessage.toId
                val key = friendTextChatItem.newPersonalMessage.id
                val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId/$key")


                ref.removeValue()
                    .addOnSuccessListener {
                        Timber.i("Removed successfully")
                    }
                    .addOnFailureListener {
                        Timber.e(it)
                    }
            }
        }
        catch(e: ArrayIndexOutOfBoundsException){
            Timber.e(e)
            Timber.i("Exception called")
            removeTextMessageFromAdapterForMe( myRef, latestMessagesFromRef)
        }
    }


     fun deleteMessage() {
        if (longPressMessage == null) {
            Timber.d("longPressShareBtn is null")
            return
        }

        if (longPressMessage is MyTextChatItem) {
            val message = longPressMessage as MyTextChatItem

            deleteMyMessage(message)
        } else {
            val message = longPressMessage as FriendTextChatItem

            deleteFriendMessage(message)
        }
    }

    fun changeLatestMessageStatusToRead(){
        val latestWasReadRef = firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}/${friendUser.value?.uid}/wasRead")
        latestWasReadRef.setValue(true)
    }

    fun changeEachPersonalMessageToRead(eachPersonalMessage: EachPersonalMessage){
        val fromId = firebaseAuth.uid
        val toId = friendUser.value?.uid
        val eachMessageWasReadRef = firebaseDatabase.getReference("/user-messages/$fromId/$toId/${eachPersonalMessage.id}/wasRead")

        eachMessageWasReadRef.setValue(true)
            .addOnFailureListener {
                Timber.e(it)
            }
            .addOnSuccessListener {
                Timber.d("Success.")
            }
    }





    private fun deleteFriendMessage(message: FriendTextChatItem) {
        val fromId = firebaseAuth.uid
        val toId = friendUser.value?.uid
        val myRef =
            firebaseDatabase.getReference("/user-messages/$fromId/$toId/${message.newPersonalMessage.id}")
        firebaseDatabase.getReference("/user-messages/$toId/$fromId/${message.newPersonalMessage.id}")

        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        val alertDialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Delete message")
            .setMessage("Are you sure you want to delete message from ${friendUser.value?.userName}")
            .setPositiveButton("DELETE FOR ME") { _, _ ->
                removeTextMessageFromAdapterForMe(myRef, latestMessagesFromRef)
                returnItemToDefault()
            }
            .setNegativeButton("CANCEL") { _, _ ->
                returnItemToDefault()
            }
            .setOnDismissListener{ returnItemToDefault() }
            .create()

        alertDialog.show()

    }


    private fun deleteFriendMessageForMe(
        myRef: DatabaseReference,
        latestMessagesFromRef: DatabaseReference,
        latestMessagesToRef: DatabaseReference
    ) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val myRefClass = snapshot.getValue(EachPersonalMessage::class.java)
                myRefClass?.textMessage = "This message was deleted."
                myRef.setValue(myRefClass).addOnSuccessListener {
                    Timber.d("myRef change done")
                }.addOnFailureListener {
                    Timber.e(it)
                }
                latestMessagesFromRef.setValue(myRefClass)
                latestMessagesToRef.setValue(myRefClass)
            }
        })
    }

    private fun deleteMyMessage(message: MyTextChatItem) {
        val fromId = firebaseAuth.uid
        val toId = friendUser.value?.uid
        val myRef =
            firebaseDatabase.getReference("/user-messages/$fromId/$toId/${message.newPersonalMessage.id}")
        val toRef =
            firebaseDatabase.getReference("/user-messages/$toId/$fromId/${message.newPersonalMessage.id}")

        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        val alertDialog = AlertDialog.Builder(fragment.requireContext())
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message")
            .setPositiveButton("DELETE FOR ME") { _, _ ->
                removeTextMessageFromAdapterForMe(myRef, latestMessagesFromRef)
                returnItemToDefault()
            }
            .setNeutralButton("CANCEL") { _, _ ->
                returnItemToDefault()
            }
            .setNegativeButton("DELETE FOR EVERYONE") { _, _ ->
                deleteMyMessageForFriend(toRef)
                removeTextMessageFromAdapterForEveryone(myRef, latestMessagesFromRef)
                returnItemToDefault()
            }
            .setOnDismissListener { returnItemToDefault() }
            .create()

        alertDialog.show()
    }

    private fun deleteMyMessageForFriend(
        toRef: DatabaseReference
    ) {
        var toRefClassInternal: EachPersonalMessage?
        toRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                toRefClassInternal = snapshot.getValue(EachPersonalMessage::class.java)
                toRefClassInternal?.textMessage = "This message was deleted."
                toRef.setValue(toRefClassInternal)
                    .addOnSuccessListener {
                        Timber.d("toRef change done")

                    }.addOnFailureListener {
                        Timber.e(it)
                    }
//                (longPressMessage as MyTextChatItem).newMessage = toRefClassInternal!!
//                longPressMessage = null
            }
        })
    }

    private fun deleteMyMessageForMe(
        myRef: DatabaseReference,
        latestMessagesFromRef: DatabaseReference
    ) {
        myRef.child("textMessage").setValue("This message was deleted.")
            .addOnSuccessListener {
                Timber.d("Changed my ref successfully")
            }
            .addOnFailureListener {
                Timber.e(it)
            }
        latestMessagesFromRef.child("textMessage").setValue("This message was deleted.")
            .addOnSuccessListener {
                Timber.d("Changed my latest messages ref successfully")
            }
            .addOnFailureListener {
                Timber.e(it)
            }

//        myRef.addValueEventListener(object : ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {
//                Timber.e(error.details)
//            }
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//                myRefClass = snapshot.getValue(EachMessage::class.java)
//                myRefClass?.textMessage = "This message was deleted."
//                myRef.setValue(myRefClass).addOnSuccessListener {
//                    Timber.d("myRef change done")
//                }.addOnFailureListener {
//                    Timber.e(it)
//                }
//                latestMessagesFromRef.setValue(myRefClass)
////                latestMessagesToRef.setValue(myRefClass)
//            }
//        })
    }
}