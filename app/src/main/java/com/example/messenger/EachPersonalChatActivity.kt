package com.example.messenger

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityEachPersonalChatBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.friend_image_chat.view.*
import kotlinx.android.synthetic.main.friend_text_chat.view.*
import kotlinx.android.synthetic.main.my_image_chat.view.*
import kotlinx.android.synthetic.main.my_text_chat.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class EachPersonalChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityEachPersonalChatBinding

    companion object {
        var friendUser: User? = null
        var myAccount: User? = null
        const val IMAGE_REQUEST_CODE = 1234
        var longPressMessage: Item<GroupieViewHolder>? = null
        var longPressView: ArrayList<View> = ArrayList()
        var messagesList: ArrayList<Item<GroupieViewHolder>> = ArrayList()
        var canAllowLongClick = true
    }

    val adapter = GroupAdapter<GroupieViewHolder>()
    var chooseImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        canAllowLongClick = true

        binding = DataBindingUtil.setContentView(this, R.layout.activity_each_personal_chat)

        friendUser = intent.getParcelableExtra(USER_KEY)
        val intentMessage = intent.getStringExtra(INTENT_URI)

        setToolbarData()
        changeLatestMessageStatusToRead()

        if (friendUser != null) {
            supportActionBar?.title = friendUser!!.userName
        }

        getProfilePicture()

        val chatRecyclerView = binding.chatRecyclerview
        Timber.d("intentMessage is $intentMessage.")
        if (intentMessage == null) Timber.d("IntentMessage is null") else binding.chatEdit.setText(
            intentMessage
        )

        binding.chooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_REQUEST_CODE)
        }

        binding.sendChatBtn.setOnClickListener {
            if (binding.chatEdit.text.isEmpty()) {
                Toast.makeText(this, "Text cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage()
        }

        listenForMessages()

        chatRecyclerView.adapter = adapter

        adapter.setOnItemLongClickListener { item, view ->

            if(canAllowLongClick) {
                longPressMessage = item
                longPressView.add(view)
                Timber.d("longPressView size is ${longPressView.size}")

                binding.longPressToolbar.visibility = View.VISIBLE
                binding.eachChatToolbar.visibility = View.GONE

                val isApplicable: Boolean = when (item) {
                    is FriendImageChatItem -> {
                        false
                    }
                    is MyImageChatItem -> {
                        false
                    }
                    else -> {
                        true
                    }
                }

                Timber.d("isApplicable is $isApplicable")

                if (isApplicable) {
                    val longPressToolbar = binding.longPressToolbar
                    setSupportActionBar(longPressToolbar)

                    view.setBackgroundColor(resources.getColor(R.color.highlightColor))
                }

                canAllowLongClick = false

                true
            }
            else{
                Timber.i("canAllowLongClick is $canAllowLongClick")

                false
            }
        }

        binding.longPressCancelBtn.setOnClickListener {
            binding.longPressToolbar.visibility = View.GONE
            binding.eachChatToolbar.visibility = View.VISIBLE

            setToolbarData()
            returnItemToDefault()
        }


        binding.longPressCopyBtn.setOnClickListener {
            copyToClipboard()
            setToolbarData()
            returnItemToDefault()
        }

        binding.longPressDeleteBtn.setOnClickListener {
            deleteMessage()

            binding.longPressToolbar.visibility = View.GONE
            binding.eachChatToolbar.visibility = View.VISIBLE

            setToolbarData()

        }

        binding.longPressShareBtn.setOnClickListener {
            shareMessage()
            returnItemToDefault()
            setToolbarData()
        }

        binding.personalVoiceCallBtn.setOnClickListener {
            val intent = Intent(this, PersonalVoiceCallActivity::class.java)
            intent.putExtra(VOICE_CALL_USER, friendUser)
            startActivity(intent)
        }

        binding.personalVideoCallBtn.setOnClickListener {
            val intent = Intent(this, PersonalVideoCallActivity::class.java)
//            intent.putExtra(VOICE_CALL_USER, friendUser)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        val intent =  Intent(this, LatestMessagesActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun setToolbarData() {
        val toolbar = binding.eachChatToolbar
        binding.toolbarName.text = friendUser?.userName
        Glide.with(this)
            .load(friendUser?.profilePictureUrl)
            .into(binding.toolbarImage)

        binding.personalBackButton.setOnClickListener {
            val intent =  Intent(this, LatestMessagesActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }

        binding.toolbarLastSeen.text = "Offline"
        val lastSeenRef = firebaseDatabase.getReference("/users-activity/${friendUser?.uid}")

        lastSeenRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val userActivity = snapshot.getValue(UserActivity::class.java) ?: return
                if (!userActivity.isOnline){
                    binding.toolbarLastSeen.text = convertTimeToLastSeenTime(userActivity.lastSeen)
                }
                else{
                    binding.toolbarLastSeen.text = "Online"
                }
            }
        })

        binding.toolbarConstraint.setOnClickListener {
            val intent = Intent(this, OthersProfileActivity::class.java)
            intent.putExtra(FRIEND_USER_PROFILE, friendUser)
            Timber.i("friend user is $friendUser")
            startActivity(intent)
        }
        setSupportActionBar(toolbar)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            if (data.data == null) return
            val uri = data.data
            val ref = firebaseStorage.getReference("/images/chat-images/${UUID.randomUUID()}")
            ref.putFile(uri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    Timber.d("download url is $it")
                    chooseImageUrl = it.toString()
                    binding.chatEdit.setText(chooseImageUrl.toString())
                }
            }.addOnFailureListener {
                Timber.e(it)
            }
        }
    }

    private fun getProfilePicture() {
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

    private fun addNewMessageToAdapter(eachPersonalMessage: EachPersonalMessage) {

        if (eachPersonalMessage.imageUrl == null) {
            adapter.add(MyTextChatItem(eachPersonalMessage.textMessage, eachPersonalMessage))
        } else {
            adapter.add(MyImageChatItem(eachPersonalMessage.imageUrl, eachPersonalMessage))
        }

        val lastItem = adapter.itemCount - 1
        Timber.i("adapter.itemCount - 1 is $lastItem and adapter.itemCount is ${adapter.itemCount}")
        binding.chatRecyclerview.layoutManager?.scrollToPosition(lastItem)

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.sendChatBtn.windowToken, 0)
    }

    private fun listenForMessages() {
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
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
            }
        })

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.sendChatBtn.windowToken, 0)
    }
    
    fun addMessageToAdapter(newMessage: EachPersonalMessage?) {
        if (newMessage != null) {
            if (newMessage.fromId == firebaseAuth.uid) {
                if (newMessage.imageUrl == null) {
                    val adapterItem = MyTextChatItem(newMessage.textMessage, newMessage)
                    adapter.add(adapterItem)
                    messagesList.add(adapterItem)

                } else {
                    val adapterItem = MyImageChatItem(newMessage.imageUrl, newMessage)
                    adapter.add(adapterItem)
                    messagesList.add(adapterItem)
                }
            } else {
                if (newMessage.imageUrl == null) {
                    val adapterItem = FriendTextChatItem(newMessage.textMessage, newMessage)
                    adapter.add(FriendTextChatItem(newMessage.textMessage, newMessage))
                    messagesList.add(adapterItem)
                } else {
                    val adapterItem = FriendImageChatItem(newMessage.imageUrl, newMessage)
                    adapter.add(FriendImageChatItem(newMessage.imageUrl, newMessage))
                    messagesList.add(adapterItem)
                }
            }
            binding.chatEdit.text.clear()
        }
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId/${newMessage?.id}/wasRead")

        ref.setValue(true)
            .addOnSuccessListener {
                Timber.d("Success.")
            }
            .addOnFailureListener {
                Timber.e(it)
            }

        val lastItem = adapter.itemCount - 1
        Timber.i("adapter.itemCount - 1 is $lastItem and adapter.itemCount is ${adapter.itemCount}")
        binding.chatRecyclerview.layoutManager?.scrollToPosition(lastItem)
    }


    private fun composeNotification() {
        Timber.d("composeNotification called")
        val topic = "/topics/${friendUser?.uid}"

//        var notification: Notification? = null
//        var notificationBody: NotificationBody?

        val ref =
            firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}/${friendUser?.uid}")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.getValue(EachPersonalMessage::class.java) ?: return

                if (myAccount?.userName == null) return
                val dataMap = DataMap(myAccount?.userName!!, lastMessage.textMessage)

//                notificationBody = NotificationBody(myAccount?.userName!!, lastMessage.textMessage)
//                notificationBody?.let {
//                    notification = Notification(topic, notificationBody!!)
//                }
//
//                notification?.let {
//                    sendActualNotification(notification!!)
//                }

                val data = FCMData(topic, dataMap)
                sendActualNotification(data)
            }
        })
    }

    fun sendActualNotification(/*notification: Notification*/ data: FCMData) {
        Timber.d("sendActualNotification called. notification is $data")
        RetrofitItem.postData.sendNotificationInApi(data)
            .enqueue(object : Callback<FCMData> {
                override fun onFailure(call: Call<FCMData>, t: Throwable) {
                    Timber.e(t)
                }

                override fun onResponse(
                    call: Call<FCMData>,
                    response: Response<FCMData>
                ) {
                    Timber.d("response.code is ${response.code()}")
                    if (response.code() == 400) {
                        Timber.i("Success: Notification sent")
                        Timber.i("response body is ${response.body()}")

                    }
                }
            })
    }

    private fun sendMessage() {
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId").push()
        val toRef = firebaseDatabase.getReference("/user-messages/$toId/$fromId/${ref.key}")

        if (firebaseAuth.uid == null || friendUser?.uid == null) {
            Timber.e("Error occurred.")
            return
        }
        if (myAccount?.userName == null || myAccount?.profilePictureUrl == null || friendUser == null) {
            Timber.e("friendUser == null is ${friendUser == null} and myAccount?.userName == null is ${myAccount?.userName == null} and myAccount?.profilePictureUrl == null is ${myAccount?.profilePictureUrl == null}")
            return
        }
        val refMessage = EachPersonalMessage(
            id = ref.key.toString(),
            fromId = firebaseAuth.uid!!,
            toId = friendUser?.uid!!,
            imageUrl = chooseImageUrl,
            textMessage = binding.chatEdit.text.toString(),
            timeStamp = System.currentTimeMillis(),
            username = myAccount?.userName!!,
            profilePictureUrl = myAccount?.profilePictureUrl!!,
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
//            listenForMessages()
            addNewMessageToAdapter(refPersonalMessage)
            composeNotification()
        }
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        val latestMessage = refPersonalMessage
        latestMessage.id = latestMessage.receiverAccount?.uid.toString()
        latestMessagesFromRef.setValue(latestMessage)
        latestMessagesToRef.setValue(latestMessage)
        chooseImageUrl = null
    }

    private fun shareMessage() {
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

        startActivity(intent)
    }

    private fun copyToClipboard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun returnItemToDefault() {
        Timber.d("$longPressView")
        longPressView.forEach {
            Timber.d("new view. Size is ${longPressView.size}")
            it.setBackgroundColor(resources.getColor(R.color.defaultBlue))
        }
        longPressView.clear()
        canAllowLongClick = true
    }

    private fun removeTextMessageFromAdapterForEveryone(
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
            Timber.d("Everyone position is ${messagesList.indexOf(longPressMessage!!)}")
            Timber.d("MessageList size is ${messagesList.size}")
            Timber.d("Position of item in adapter is ${adapter.getAdapterPosition(longPressMessage!!)}")
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

            Timber.d("updated message is ${(adapter.getItem(adapter.getAdapterPosition(longPressMessage!!)) as MyTextChatItem).newPersonalMessage}")
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

    private fun removeTextMessageFromAdapterForMe(
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


    private fun deleteMessage() {
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
        val latestWasReadRef = firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}/${friendUser?.uid}/wasRead")
        latestWasReadRef.setValue(true)
    }

    fun changeEachPersonalMessageToRead(eachPersonalMessage: EachPersonalMessage){
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val eachMessageWasReadRef = firebaseDatabase.getReference("/user-messages/$fromId/$toId/${eachPersonalMessage.id}/wasRead")

        eachMessageWasReadRef.setValue(true)
            .addOnFailureListener {
                Timber.e(it)
            }
            .addOnSuccessListener {
                Timber.d("Success.")
            }
    }

    class MyTextChatItem(val text: String, var newPersonalMessage: EachPersonalMessage) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.my_text_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            layout.my_text_chat_text.text = newPersonalMessage.textMessage
            layout.my_text_chat_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)

            val myImage = layout.my_text_chat_image
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
            if (myAccount?.profilePictureUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(myAccount?.profilePictureUrl)
                    .into(myImage)
            }
            Timber.d("my position is $position")
        }
    }

    class MyImageChatItem(private val myImageUrl: String, val newPersonalMessage: EachPersonalMessage) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.my_image_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            val image = layout.my_image_chat_image

            layout.my_image_chat_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)

            Glide.with(layout.context)
                .load(myImageUrl)
                .into(image)

            val myImage = layout.my_image_chat_profile_picture
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
            if (myAccount?.profilePictureUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(myAccount?.profilePictureUrl)
                    .into(myImage)
            }
            Timber.d("my position is $position")
        }
    }

    class FriendTextChatItem(val text: String, val newPersonalMessage: EachPersonalMessage) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.friend_text_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val friendImage = viewHolder.itemView.friends_image
            val layout = viewHolder.itemView
            viewHolder.itemView.friends_text.text = text
            layout.friends_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)

            if (friendUser?.profilePictureUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(friendUser?.profilePictureUrl)
                    .into(friendImage)
            }

            Timber.d("friend position is $position")
        }
    }

    class FriendImageChatItem(private val friendImageUrl: String, val newPersonalMessage: EachPersonalMessage) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.friend_image_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            val image = layout.image_friends_image

            Glide.with(layout.context)
                .load(friendImageUrl)
                .into(image)

            layout.image_friends_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)

            val friendImage = layout.image_friends_profile_picture
            if (friendUser?.profilePictureUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(friendUser?.profilePictureUrl)
                    .into(friendImage)
            }
            Timber.d("my position is $position")
        }
    }


    private fun deleteFriendMessage(message: FriendTextChatItem) {
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val myRef =
            firebaseDatabase.getReference("/user-messages/$fromId/$toId/${message.newPersonalMessage.id}")
        firebaseDatabase.getReference("/user-messages/$toId/$fromId/${message.newPersonalMessage.id}")

        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete message")
            .setMessage("Are you sure you want to delete message from ${friendUser?.userName}")
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
        val toId = friendUser?.uid
        val myRef =
            firebaseDatabase.getReference("/user-messages/$fromId/$toId/${message.newPersonalMessage.id}")
        val toRef =
            firebaseDatabase.getReference("/user-messages/$toId/$fromId/${message.newPersonalMessage.id}")

        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        val alertDialog = AlertDialog.Builder(this)
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
