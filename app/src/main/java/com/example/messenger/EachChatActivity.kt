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
import com.example.messenger.databinding.ActivityEachChatBinding
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

class EachChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityEachChatBinding
    companion object{
        var friendUser: User? = null
        var currentUserImageUrl: String? = null
        var myAccount: User? = null
        const val IMAGE_REQUEST_CODE = 1234
        var longPressMessage: Item<GroupieViewHolder>? = null
        var longPressView: ArrayList<View> = ArrayList()
    }

    val adapter = GroupAdapter<GroupieViewHolder>()
    var chooseImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_each_chat)

        friendUser = intent.getParcelableExtra(USER_KEY)
        val intentMessage = intent.getStringExtra(INTENT_URI)

        setToolbarData()

        if (friendUser != null) {
            supportActionBar?.title = friendUser!!.userName
        }

        getProfilePicture()

        val chatRecyclerView = binding.chatRecyclerview
        Timber.d("intentMessage is $intentMessage.")
        if (intentMessage == null) Timber.d("IntentMessage is null") else binding.chatEdit.setText(intentMessage)

        binding.chooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_REQUEST_CODE)
        }

        binding.sendChatBtn.setOnClickListener {
            if (binding.chatEdit.text.isEmpty()){
                Toast.makeText(this, "Text cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage()
        }

        listenForMessages()

        chatRecyclerView.adapter = adapter

        adapter.setOnItemLongClickListener { item, view ->

            longPressMessage = item
            longPressView.add(view)
            Timber.d("longPressView size is ${longPressView.size}")

            binding.longPressToolbar.visibility = View.VISIBLE
            binding.eachChatToolbar.visibility = View.GONE

            val isApplicable: Boolean = when (item){
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

            if (isApplicable){
                val longPressToolbar = binding.longPressToolbar
                setSupportActionBar(longPressToolbar)

                view.setBackgroundColor(resources.getColor(R.color.highlightColor))
            }

            true
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
            returnItemToDefault()

        }

        binding.longPressShareBtn.setOnClickListener {
            shareMessage()
            returnItemToDefault()
            setToolbarData()
        }
    }

    private fun setToolbarData(){
        val toolbar = binding.eachChatToolbar
        binding.toolbarName.text = friendUser?.userName
        Glide.with(this)
            .load(friendUser?.profilePictureUrl)
            .into(binding.toolbarImage)
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            finish()
        }
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
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null){
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

    private fun getProfilePicture(){
        val myUid = firebaseAuth.uid
        val users = firebaseDatabase.getReference("/users")
        users.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val eachAvailableUser = it.getValue(User::class.java)
                    if (eachAvailableUser != null){
                        Timber.i("eachAvailableUser.uid is ${eachAvailableUser.uid} and myUid is $myUid")
                        if (eachAvailableUser.uid == myUid){
                            myAccount = eachAvailableUser
                            currentUserImageUrl = eachAvailableUser.profilePictureUrl
                        }
                    }
                }
            }
        })
    }

    private fun listenForMessages(){
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId")
        adapter.clear()
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {            }

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val newMessage = it.getValue(EachMessage::class.java)
                    if (newMessage != null){
                        if (newMessage.fromId == firebaseAuth.uid){
                            if (newMessage.imageUrl == null) {
                                adapter.add(MyTextChatItem(newMessage.textMessage, newMessage))
                            }
                            else{
                                adapter.add(MyImageChatItem(newMessage.imageUrl))
                            }
                        }else{
                            if (newMessage.imageUrl == null) {
                                adapter.add(FriendTextChatItem(newMessage.textMessage, newMessage))
                            }
                            else{
                                adapter.add(FriendImageChatItem(newMessage.imageUrl))
                            }
                        }
                        binding.chatEdit.text.clear()
                    }
                }
                val lastItem = adapter.itemCount - 1
                Timber.i("adapter.itemCount - 1 is $lastItem and adapter.itemCount is ${adapter.itemCount}")
                binding.chatRecyclerview.layoutManager?.scrollToPosition(lastItem)

            }
        })

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.sendChatBtn.windowToken, 0)
    }

    private fun composeNotification(){
        Timber.d("composeNotification called")
        val topic = "/topics/${friendUser?.uid}"

        var notification: Notification? = null
        var notificationBody: NotificationBody?

        val ref = firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}/${friendUser?.uid}")

        ref.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.getValue(EachMessage::class.java) ?: return

                if (myAccount?.userName == null) return
                notificationBody = NotificationBody(myAccount?.userName!!, lastMessage.textMessage)
                notificationBody?.let {
                    notification = Notification(topic, notificationBody!!)
                }

                notification?.let {
                    sendActualNotification(notification!!)
                }
            }
        })
    }

    fun sendActualNotification(notification: Notification) {
        Timber.d("sendActualNotification called. notification is $notification")
        RetrofitItem.postData.sendNotificationInApi(notification).enqueue(object : Callback<Notification>{
            override fun onFailure(call: Call<Notification>, t: Throwable) {
                Timber.e(t)
            }

            override fun onResponse(call: Call<Notification>, response: Response<Notification>) {
                Timber.d("response.code is ${response.code()}")
                if (response.code() == 400){
                    Timber.i("Success: Notification sent")
                    Timber.i("response body is ${response.body()}")

                }
            }
        })
    }

    private fun sendMessage(){
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
        val refMessage = EachMessage(id = ref.key!!, fromId = firebaseAuth.uid!!, toId = friendUser?.uid!!, imageUrl = chooseImageUrl, textMessage = binding.chatEdit.text.toString(), timeStamp = System.currentTimeMillis() / 1000, username = myAccount?.userName!!, profilePictureUrl = myAccount?.profilePictureUrl!!, receiverAccount = friendUser, senderAccount = myAccount)

        setFirebaseValues(ref, toRef, refMessage)
    }

    private fun setFirebaseValues(ref: DatabaseReference, toRef: DatabaseReference, refMessage: EachMessage){
        toRef.setValue(refMessage)
        ref.setValue(refMessage).addOnSuccessListener {
            listenForMessages()
            composeNotification()
        }
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        latestMessagesFromRef.setValue(refMessage)
        latestMessagesToRef.setValue(refMessage)
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

    private fun copyToClipboard(){
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

    private fun returnItemToDefault(){
        Timber.d("$longPressView")
        longPressView.forEach {
            Timber.d("new view. Size is ${longPressView.size}")
            it.setBackgroundColor(resources.getColor(R.color.defaultBlue))
        }
        longPressView.clear()
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

    class MyTextChatItem(val text: String, val newMessage: EachMessage): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.my_text_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            layout.my_text_chat_text.text = text

            val myImage = layout.my_text_chat_image
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
            if (currentUserImageUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(currentUserImageUrl)
                    .into(myImage)
            }
            Timber.d("my position is $position")
        }
    }

    class MyImageChatItem(private val myImageUrl: String): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.my_image_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            val image = layout.my_image_chat_image

            Glide.with(layout.context)
                .load(myImageUrl)
                .into(image)

            val myImage = layout.my_image_chat_profile_picture
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
            if (currentUserImageUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(currentUserImageUrl)
                    .into(myImage)
            }
            Timber.d("my position is $position")
        }
    }

    class FriendTextChatItem(val text: String, val newMessage: EachMessage): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.friend_text_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val friendImage = viewHolder.itemView.friends_image
            viewHolder.itemView.friends_text.text = text

//            Timber.d("friend's image url is ${friendUser?.profilePictureUrl}")
            if (friendUser?.profilePictureUrl != null){
                Glide.with(viewHolder.itemView.context)
                    .load(friendUser?.profilePictureUrl)
                    .into(friendImage)
            }

            Timber.d("friend position is $position")
        }
    }

    class FriendImageChatItem(private val friendImageUrl: String): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.friend_image_chat

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            val image = layout.image_friends_image

            Glide.with(layout.context)
                .load(friendImageUrl)
                .into(image)

            val myImage = layout.image_friends_profile_picture
            if (currentUserImageUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(currentUserImageUrl)
                    .into(myImage)
            }
            Timber.d("my position is $position")
        }
    }


    private fun deleteFriendMessage(message: FriendTextChatItem) {
        val fromId = firebaseAuth.uid
        val toId = friendUser?.uid
        val myRef =
            firebaseDatabase.getReference("/user-messages/$fromId/$toId/${message.newMessage.id}")
        firebaseDatabase.getReference("/user-messages/$toId/$fromId/${message.newMessage.id}")

        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete message")
            .setMessage("Are you sure you want to delete message from ${friendUser?.userName}")
            .setPositiveButton("DELETE FOR ME") {_, _ ->
                deleteFriendMessageForMe(myRef, latestMessagesFromRef, latestMessagesToRef)
                listenForMessages()
            }
            .setNegativeButton("CANCEL") {_,_ -> }
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
                val myRefClass = snapshot.getValue(EachMessage::class.java)
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
            firebaseDatabase.getReference("/user-messages/$fromId/$toId/${message.newMessage.id}")
        val toRef =
            firebaseDatabase.getReference("/user-messages/$toId/$fromId/${message.newMessage.id}")

        val latestMessagesFromRef = firebaseDatabase.getReference("/latest-messages/$fromId/$toId")
        val latestMessagesToRef = firebaseDatabase.getReference("/latest-messages/$toId/$fromId")

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message")
            .setPositiveButton("DELETE FOR ME") { _, _ ->
                deleteMyMessageForMe(myRef, latestMessagesFromRef, latestMessagesToRef)
                listenForMessages()
            }
            .setNeutralButton("CANCEL") {_, _ -> }
            .setNegativeButton("DELETE FOR EVERYONE") {_ ,_ ->
                deleteMyMessageForMe(myRef, latestMessagesFromRef, latestMessagesToRef)
                deleteMyMessageForFriend(toRef)
                listenForMessages()
            }
            .create()

        alertDialog.show()
    }

    private fun deleteMyMessageForFriend(
        toRef: DatabaseReference
    ) {
        var toRefClassInternal: EachMessage?
        toRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                toRefClassInternal = snapshot.getValue(EachMessage::class.java)
                toRefClassInternal?.textMessage = "This message was deleted."
                toRef.setValue(toRefClassInternal)
                    .addOnSuccessListener {
                        Timber.d("toRef change done")

                    }.addOnFailureListener {
                        Timber.e(it)
                    }
            }
        })
    }

    private fun deleteMyMessageForMe(
        myRef: DatabaseReference,
        latestMessagesFromRef: DatabaseReference,
        latestMessagesToRef: DatabaseReference
    ) {
        var myRefClass: EachMessage?
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                myRefClass = snapshot.getValue(EachMessage::class.java)
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
}
