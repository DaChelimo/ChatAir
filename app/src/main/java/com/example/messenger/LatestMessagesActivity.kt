package com.example.messenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityLatestMessagesBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.latest_messages_item.view.*
import timber.log.Timber

class LatestMessagesActivity : AppCompatActivity() {

    lateinit var binding: ActivityLatestMessagesBinding
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        binding = DataBindingUtil.setContentView(this, R.layout.activity_latest_messages)
        val latestMessageRecyclerView = binding.latestRecyclerView

        listenForLatestMessages()

        latestMessageRecyclerView.adapter = adapter

//        if (firebaseInstanceId.token != null) sendUserToToDatabase(firebaseInstanceId.token!!)

        firebaseMessaging.subscribeToTopic("/topics/${firebaseAuth.uid}")

        firebaseInstanceId.instanceId
            .addOnSuccessListener {task ->
                Timber.d("Success. Token is ${task.token}")
                sendUserToToDatabase(task.token)
            }
            .addOnFailureListener {
                Timber.e(it)
            }

        adapter.setOnItemClickListener { item, view ->
            val chatMessageItem = item as LatestMessageItem
            val friendUid = chatMessageItem.chatMessage.toId

            val ref = firebaseDatabase.getReference("/users/$friendUid")

            ref.addValueEventListener(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java) ?: return
                    val intent = Intent(view.context, EachChatActivity::class.java)
                    intent.putExtra(USER_KEY, user)
                    Timber.i("item is $item.")
                    startActivity(intent)
                }
            })
        }

        listenForLatestMessages()
    }

    val latestMessagesMap = HashMap<String, EachMessage>()

    fun refreshRecyclerViewMessages(){
        adapter.clear()
        Timber.i("latestMessagesMap size is ${latestMessagesMap.values.size}")
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessageItem(it))
        }
    }

    private fun listenForLatestMessages(){
        latestMessagesMap.clear()
        val ref = firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}")
        ref.addChildEventListener(object : ChildEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(EachMessage::class.java) ?: return

                if (chatMessage.fromId == firebaseAuth.uid && chatMessage.toId == firebaseAuth.uid){
                    Timber.d("Same account in latest activity")
                }
                else {
                    latestMessagesMap[chatMessage.id] = chatMessage
                    refreshRecyclerViewMessages()
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                val chatMessage = snapshot.getValue(EachMessage::class.java) ?: return

                if (chatMessage.fromId == firebaseAuth.uid && chatMessage.toId == firebaseAuth.uid){
                    Timber.d("Same account in latest activity")
                }
                else {
                    latestMessagesMap[chatMessage.id] = chatMessage
                    refreshRecyclerViewMessages()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }
        })
        refreshRecyclerViewMessages()
    }

    class LatestMessageItem(val chatMessage: EachMessage): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.latest_messages_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView

            val ref = firebaseDatabase.getReference("/users")

            ref.addValueEventListener(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val user = it.getValue(User::class.java) ?: return@forEach

                        Timber.d("chatMessage.toId is ${chatMessage.toId} and user.uid is ${user.uid}")
                        Timber.d("chatMessage.fromId is ${chatMessage.fromId} and firebaseAuth.uid is ${firebaseAuth.uid}")
                        if(chatMessage.fromId == firebaseAuth.uid) {
                            if (chatMessage.toId == user.uid) {
                                layout.latest_item_username.text = user.userName
                                Glide.with(layout.context)
                                    .load(user.profilePictureUrl)
                                    .placeholder(R.drawable.ic_placeholder_person_24)
                                    .into(layout.latest_item_image)
                            }
                        }
                    }
                }
            })

            var formatString = if (chatMessage.textMessage.length > 33) chatMessage.textMessage.substring(0..32) else chatMessage.textMessage
            if(chatMessage.textMessage.length > 27){
                formatString += "..."
            }

            if (chatMessage.imageUrl == null) {
                layout.latest_item_last_message.text = formatString
            }else {
                layout.latest_item_last_message.text = "Image"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.latest_messages_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.add_new_user -> {
                val intent = Intent(this, NewUsersActivity::class.java)
                startActivity(intent)

                true
            }
            R.id.sign_out -> {
                firebaseAuth.signOut()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            }
            else -> false

        }
    }
}