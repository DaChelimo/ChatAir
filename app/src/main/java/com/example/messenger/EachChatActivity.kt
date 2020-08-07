package com.example.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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

class EachChatActivity : AppCompatActivity() {

    lateinit var binding: ActivityEachChatBinding
    companion object{
        var friendUser: User? = null
        var currentUserImageUrl: String? = null
        var myAccount: User? = null
        const val IMAGE_REQUEST_CODE = 1234
    }

    val adapter = GroupAdapter<GroupieViewHolder>()
    var chooseImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_each_chat)

        friendUser = intent.getParcelableExtra(USER_KEY)
        val intentMessage = intent.getStringExtra(INTENT_URI)

//        Timber.d("user is $friendUser")
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
//        initMessages()

        chatRecyclerView.adapter = adapter
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
                                adapter.add(MyTextChatItem(newMessage.textMessage))
                            }
                            else{
                                adapter.add(MyImageChatItem(newMessage.imageUrl))
                            }
                        }else{
                            if (newMessage.imageUrl == null) {
                                adapter.add(FriendTextChatItem(newMessage.textMessage))
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

//        val notification = JSONObject()
//        val notificationBody = JSONObject()

        var notification: Notification? = null
        var notificationBody: NotificationBody?

        val ref = firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}/${friendUser?.uid}")

        ref.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.getValue(EachMessage::class.java) ?: return

//                try {
//                    notificationBody.put("title", myAccount?.userName)
//                    notificationBody.put("message", lastMessage.textMessage)
//
//                    notification.put("to", topic)
//                    notification.put("data", notificationBody)
//                }
//                catch (jsonException: JSONException){
//                    Timber.e(jsonException)
//                }

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
        val toRef = firebaseDatabase.getReference("/user-messages/$toId/$fromId").push()

        if (firebaseAuth.uid == null || friendUser?.uid == null) {
            Timber.e("Error occurred.")
            return
        }
        if (myAccount?.userName == null || myAccount?.profilePictureUrl == null || friendUser == null) {
            Timber.e("friendUser == null is ${friendUser == null} and myAccount?.userName == null is ${myAccount?.userName == null} and myAccount?.profilePictureUrl == null is ${myAccount?.profilePictureUrl == null}")
            return
        }
        val refMessage = EachMessage(id = ref.key!!, fromId = firebaseAuth.uid!!, toId = friendUser?.uid!!, imageUrl = chooseImageUrl, textMessage = binding.chatEdit.text.toString(), timeStamp = System.currentTimeMillis() / 1000, username = myAccount?.userName!!, profilePictureUrl = myAccount?.profilePictureUrl!!, receiverAccount = friendUser, senderAccount = myAccount)
// data class EachMessage(val id: String, val fromId: String, val toId: String, val imageUrl: String?, val textMessage: String, val timeStamp: Long, val username: String, val profilePictureUrl: String){
        setFirebaseValues(ref, toRef, refMessage)
    }

    private fun setFirebaseValues(ref: DatabaseReference, toRef: DatabaseReference, refMessage: EachMessage){
        ref.setValue(refMessage)
        toRef.setValue(refMessage).addOnSuccessListener {
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

    class MyTextChatItem(val text: String): Item<GroupieViewHolder>(){
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

    class FriendTextChatItem(val text: String): Item<GroupieViewHolder>(){
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
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
            if (currentUserImageUrl != null) {
                Glide.with(viewHolder.itemView.context)
                    .load(currentUserImageUrl)
                    .into(myImage)
            }
            Timber.d("my position is $position")
        }
    }
}

/*
 val topic = "/topics/Enter_topic" //topic has to match what the receiver subscribed to

                val notification = JSONObject()
                val notifcationBody = JSONObject()

                try {
                    notifcationBody.put("title", "Firebase Notification")
                    notifcationBody.put("message", binding.msg.text)
                    notification.put("to", topic)
                    notification.put("data", notifcationBody)
                    Log.e("TAG", "try")
                } catch (e: JSONException) {
                    Log.e("TAG", "onCreate: " + e.message)
                }

                sendNotification(notification)


                Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                Log.i("TAG", "onResponse: $response")
                msg.setText("")
            },
            Response.ErrorListener {
                Toast.makeText(this@MainActivity, "Request error", Toast.LENGTH_LONG).show()
                Log.i("TAG", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
 */