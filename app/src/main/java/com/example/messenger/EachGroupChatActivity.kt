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
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityEachGroupChatBinding
import com.google.firebase.database.*
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
import kotlin.collections.LinkedHashMap

class EachGroupChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityEachGroupChatBinding

    companion object {
        var myAccount: User? = null
        var basicGroupData: BasicGroupData? = null
        const val IMAGE_REQUEST_CODE = 1234
        var longPressMessage: Item<GroupieViewHolder>? = null
        var longPressView: View? = null
        var messagesList: LinkedHashMap<String, Item<GroupieViewHolder>> = LinkedHashMap()
        var canAllowLongClick = true
    }

    val adapter = GroupAdapter<GroupieViewHolder>()
    var chooseImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_each_group_chat)

        basicGroupData = intent.getParcelableExtra(GROUP_KEY)
        if (basicGroupData != null) {
            supportActionBar?.title = basicGroupData!!.groupName
        }

        setToolbarData()
        getProfilePicture()

        val chatRecyclerView = binding.groupChatRecyclerview
//        val intentMessage = intent.getStringExtra(INTENT_URI)
//        Timber.d("intentMessage is $intentMessage.")
//        if (intentMessage == null) Timber.d("IntentMessage is null") else binding.groupChatEdit.setText(
//            intentMessage
//        )

        binding.groupChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_REQUEST_CODE)
        }

        binding.groupSendChatBtn.setOnClickListener {
            if (binding.groupChatEdit.text.isEmpty()) {
                Toast.makeText(this, "Text cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage()
        }

        listenForMessages()

        chatRecyclerView.adapter = adapter

        adapter.setOnItemLongClickListener { item, view ->

            if (canAllowLongClick) {
                longPressMessage = item
                longPressView = view

                binding.groupLongPressToolbar.visibility = View.VISIBLE
                binding.eachGroupChatToolbar.visibility = View.GONE

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
                    val longPressToolbar = binding.groupLongPressToolbar
                    setSupportActionBar(longPressToolbar)

                    view.setBackgroundColor(resources.getColor(R.color.highlightColor))
                }

                canAllowLongClick = false

                true
            } else {
                Timber.i("canAllowLongClick is ${canAllowLongClick}")

                false
            }
        }

        binding.groupLongPressCancelBtn.setOnClickListener {
            binding.groupLongPressToolbar.visibility = View.GONE
            binding.eachGroupChatToolbar.visibility = View.VISIBLE

            setToolbarData()
            returnItemToDefault()
        }


        binding.groupLongPressCopyBtn.setOnClickListener {
            copyToClipboard()
            setToolbarData()
            returnItemToDefault()
        }

        binding.groupLongPressDeleteBtn.setOnClickListener {
//            deleteMessage()

            binding.groupLongPressToolbar.visibility = View.GONE
            binding.eachGroupChatToolbar.visibility = View.VISIBLE

            setToolbarData()
            returnItemToDefault()

        }

        binding.groupLongPressShareBtn.setOnClickListener {
            shareMessage()
            returnItemToDefault()
            setToolbarData()
        }
    }

    private fun setToolbarData() {
        val toolbar = binding.eachGroupChatToolbar
        binding.groupToolbarName.text = basicGroupData?.groupName
        Glide.with(this)
            .load(basicGroupData?.groupIcon)
            .into(binding.groupToolbarImage)

        binding.groupBackButton.setOnClickListener {
            finish()
        }

        binding.groupToolbarConstraint.setOnClickListener {
//            val intent = Intent(this, OthersProfileActivity::class.java)
//            intent.putExtra(FRIEND_USER_PROFILE, basicGroupData)
//            Timber.i("friend user is ${basicGroupData}")
//            startActivity(intent)
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
                    binding.groupChatEdit.setText(chooseImageUrl.toString())
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

    private fun addMessageToAdapter(eachGroupMessage: EachGroupMessage) {

        if (eachGroupMessage.imageUrl == null) {
//            adapter.add(MyTextChatItem(eachGroupMessage.textMessage, eachGroupMessage))
            messagesList[eachGroupMessage.id] = MyTextChatItem(eachGroupMessage.textMessage, eachGroupMessage)
        } else {
//            adapter.add(MyImageChatItem(eachGroupMessage.imageUrl, eachGroupMessage))
            messagesList[eachGroupMessage.id] = MyImageChatItem(eachGroupMessage.imageUrl, eachGroupMessage)
        }

        adapter.update(messagesList.values)

        val lastItem = adapter.itemCount - 1
        Timber.i("adapter.itemCount - 1 is $lastItem and adapter.itemCount is ${adapter.itemCount}")
        binding.groupChatRecyclerview.layoutManager?.scrollToPosition(lastItem)

        binding.groupChatEdit.text.clear()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.groupSendChatBtn.windowToken, 0)
    }

    private fun listenForMessages() {
        val groupUid = basicGroupData?.groupUid
        val ref = firebaseDatabase.getReference("/groups/$groupUid/messages")

        ref.addChildEventListener(object : ChildEventListener{
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

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.groupSendChatBtn.windowToken, 0)
    }

    private fun composeNotification() {
        Timber.d("composeNotification called")

        val dataMap = HashMap<String, String?>()
        if (myAccount?.userName == null) return

        val groupUid = basicGroupData?.groupUid
        val groupLatestMessageRef = firebaseDatabase.getReference("/groups/$groupUid/latest-message")

        groupLatestMessageRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.getValue(EachGroupMessage::class.java) ?: return

                dataMap["sendersName"] = myAccount?.userName
                dataMap["message"] = lastMessage.textMessage

                basicGroupData?.groupMembers?.forEach {
                    if (it.uid != firebaseAuth.uid) {
                        val topic = "/topics/${it.uid}"

//                        notificationBody =
//                            NotificationBody(basicGroupData?.groupName.toString() , "${lastMessage.senderAccount?.userName}: ${lastMessage.textMessage}")
//                        notificationBody?.let {
//                            notification = Notification(topic, notificationBody!!)
//                        }
//
//                        notification?.let {
//                            sendActualNotification(notification!!)
//                        }



                        val data = FCMData(topic, dataMap)
                        sendActualNotification(data)

                        Timber.d("basicGroupData?.groupMembers.member is ${it.userName} and notification is $dataMap")
                    }
                }
            }
        })
    }

    private fun addGroupMessageToHashMap(newMessage: EachGroupMessage) {
        if (newMessage.fromId == firebaseAuth.uid) {
            if (newMessage.imageUrl == null) {
                val adapterItem = MyTextChatItem(newMessage.textMessage, newMessage)
                messagesList[adapterItem.newGroupMessage.id] = adapterItem

            } else {
                val adapterItem = MyImageChatItem(newMessage.imageUrl, newMessage)
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

        val lastItem = adapter.itemCount - 1
        Timber.i("adapter.itemCount - 1 is $lastItem and adapter.itemCount is ${adapter.itemCount}")
        binding.groupChatRecyclerview.layoutManager?.scrollToPosition(lastItem)
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
            textMessage = binding.groupChatEdit.text.toString(),
            timeStamp = System.currentTimeMillis(),
            username = myAccount?.userName!!,
            profilePictureUrl = myAccount?.profilePictureUrl!!,
            receiverAccounts = basicGroupData!!.groupMembers,
            senderAccount = myAccount,
            wasRead = false
        )

        setFirebaseValues(groupMessagesRef, refMessage)
    }

    private fun setFirebaseValues(
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
                friendTextChatItem.newGroupMessage.textMessage
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
            friendTextChatItem.newGroupMessage.textMessage
        }

        val clip = ClipData.newPlainText("Chat Message", clipboardText)
        Timber.d("clipboard data is ${clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()}")
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun returnItemToDefault() {
        longPressView?.setBackgroundColor(resources.getColor(R.color.defaultBlue))
        longPressView = null
        canAllowLongClick = true
    }

//    var numberRepeated = 1

    class MyTextChatItem(val text: String, var newGroupMessage: EachGroupMessage) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.my_text_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            layout.my_text_chat_text.text = text

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

    class MyImageChatItem(private val myImageUrl: String, val newGroupMessage: EachGroupMessage) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.my_image_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            val image = layout.my_image_chat_image

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

    class FriendTextChatItem(val newGroupMessage: EachGroupMessage) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.friend_text_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val friendImage = viewHolder.itemView.friends_image
            viewHolder.itemView.friends_text.text = newGroupMessage.textMessage

            if (newGroupMessage.senderAccount?.profilePictureUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(newGroupMessage.senderAccount.profilePictureUrl)
                    .into(friendImage)
            }

            Timber.d("friend position is $position")
        }
    }

    class FriendImageChatItem(val newGroupMessage: EachGroupMessage) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.friend_image_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            val image = layout.image_friends_image

            Glide.with(layout.context)
                .load(newGroupMessage.imageUrl)
                .into(image)

            val friendImage = layout.image_friends_profile_picture
            if (newGroupMessage.senderAccount?.profilePictureUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(basicGroupData?.groupIcon)
                    .into(friendImage)
            }
            Timber.d("my position is $position")
        }
    }
}