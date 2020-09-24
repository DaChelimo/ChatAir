package com.example.messenger.each_group_chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.*
import com.example.messenger.R
import com.example.messenger.each_personal_chat.EachPersonalChatFragment
import com.example.messenger.notification_pack.MyResponse
import com.example.messenger.notification_pack.Notification
import com.example.messenger.notification_pack.NotificationBody
import com.example.messenger.notification_pack.RetrofitItem
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class EachGroupChatViewModel(private val fragment: Fragment) : ViewModel() {

    private var _scrollToPosition = MutableLiveData<Int>()
    val scrollToPosition: LiveData<Int>
        get() = _scrollToPosition

    var myAccount: User? = null
    var basicGroupData: BasicGroupData? = null
    val IMAGE_REQUEST_CODE = 1234
    var longPressMessage: Item<GroupieViewHolder>? = null
    var longPressView: View? = null
    var messagesList: LinkedHashMap<String, Item<GroupieViewHolder>> = LinkedHashMap()
    var canAllowLongClick = true

    val adapter = GroupAdapter<GroupieViewHolder>()
    var chooseImageUrl: String? = null

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

    fun addMessageToAdapter(eachGroupMessage: EachGroupMessage) {

        if (eachGroupMessage.imageUrl == null) {
//            adapter.add(MyTextChatItem(eachGroupMessage.textMessage, eachGroupMessage))
            messagesList[eachGroupMessage.id] =
                MyTextChatItem(eachGroupMessage.textMessage, eachGroupMessage)
        } else {
//            adapter.add(MyImageChatItem(eachGroupMessage.imageUrl, eachGroupMessage))
            messagesList[eachGroupMessage.id] =
                MyImageChatItem(eachGroupMessage.imageUrl, eachGroupMessage)
        }

        adapter.update(messagesList.values)
        _scrollToPosition.value = adapter.itemCount

    }

    fun listenForMessages() {
        val groupUid = basicGroupData?.groupUid
        val ref = firebaseDatabase.getReference("/groups/$groupUid/messages")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val newMessage = snapshot.getValue(EachGroupMessage::class.java) ?: return
                addGroupMessageToHashMap(newMessage)
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val newMessage = snapshot.getValue(EachGroupMessage::class.java) ?: return
                addGroupMessageToHashMap(newMessage)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
        })
//
//        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(binding.groupSendChatBtn.windowToken, 0)
    }

    fun composeNotification() {
        Timber.d("composeNotification called")

        if (myAccount?.userName == null) return

        val groupUid = basicGroupData?.groupUid
        val groupLatestMessageRef = firebaseDatabase.getReference("/groups/$groupUid/latest-message")

        groupLatestMessageRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.getValue(EachGroupMessage::class.java) ?: return

                basicGroupData?.groupMembers?.forEach {
                    if (it.uid != firebaseAuth.uid) {
                        val topic = "/topics/${it.uid}"

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
                        Timber.d("basicGroupData?.groupMembers.member is ${it.userName} and notification is $notification")
                    }
                }
            }
        })
    }

    fun addGroupMessageToHashMap(newMessage: EachGroupMessage) {
        if (newMessage.fromId == firebaseAuth.uid) {
            if (newMessage.imageUrl == null) {
                val adapterItem =
                    MyTextChatItem(newMessage.textMessage, newMessage)
                messagesList[adapterItem.newGroupMessage.id] = adapterItem

            } else {
                val adapterItem =
                    MyImageChatItem(newMessage.imageUrl, newMessage)
                messagesList[adapterItem.newGroupMessage.id] = adapterItem
            }
        } else {
            if (newMessage.imageUrl == null) {
                val adapterItem = FriendTextChatItem(newMessage)
                messagesList[adapterItem.newGroupMessage.id] = adapterItem
            } else {
                val adapterItem = FriendImageChatItem(newMessage)
                messagesList[adapterItem.newGroupMessage.id] = adapterItem
            }

        }

        adapter.update(messagesList.values)
        Timber.d("adapter size is ${adapter.itemCount} and messageList values is ${messagesList.values.size}")

        _scrollToPosition.value = adapter.itemCount
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

    fun sendMessage(textMessage: String) {
        val uid = basicGroupData?.groupUid
        val groupMessagesRef = firebaseDatabase.getReference("/groups/$uid/messages").push()

        if (firebaseAuth.uid == null || basicGroupData?.groupUid == null || basicGroupData == null) {
            Timber.e("Error occurred.")
            return
        }
        if (myAccount?.userName == null || myAccount?.profilePictureUrl == null || basicGroupData == null) {
            Timber.e("friendUser == null is ${basicGroupData == null} and myAccount?.userName == null is ${myAccount?.userName == null} and myAccount?.profilePictureUrl == null is ${myAccount?.profilePictureUrl == null}")
            return
        }
        val refMessage = EachGroupMessage(
            id = groupMessagesRef.key.toString(),
            fromId = firebaseAuth.uid!!,
            imageUrl = chooseImageUrl,
            textMessage = textMessage,
            timeStamp = System.currentTimeMillis(),
            username = myAccount?.userName!!,
            profilePictureUrl = myAccount?.profilePictureUrl!!,
            receiverAccounts = basicGroupData!!.groupMembers,
            senderAccount = myAccount,
            wasRead = false
        )

        setFirebaseValues(groupMessagesRef, refMessage)
    }

    fun setFirebaseValues(
        groupMessagesRef: DatabaseReference,
        eachGroupMessage: EachGroupMessage
    ) {
        groupMessagesRef.setValue(eachGroupMessage).addOnSuccessListener {
//            listenForMessages()
            addMessageToAdapter(eachGroupMessage)
            composeNotification()
        }
//        val groupLatestMessage = firebaseDatabase.getReference("groups/${basicGroupData?.groupUid}/latest-message")
        val groupUid = basicGroupData?.groupUid
        val groupLatestMessageRef = firebaseDatabase.getReference("/groups/$groupUid/latest-message")

        groupLatestMessageRef.setValue(eachGroupMessage)
        chooseImageUrl = null
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
                friendTextChatItem.newGroupMessage.textMessage
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
            friendTextChatItem.newGroupMessage.textMessage
        }

        val clip = ClipData.newPlainText("Chat Message", clipboardText)
        Timber.d("clipboard data is ${clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()}")
        clipboardManager.setPrimaryClip(clip)
        showShortToast("Copied to clipboard")
    }

    fun returnItemToDefault() {
        longPressView?.setBackgroundColor(fragment.resources.getColor(R.color.defaultBlue))
        longPressView = null
        canAllowLongClick = true
    }

}